package cn.szu.blankxiao.panoramaview.network

import android.util.Log
import cn.szu.blankxiao.panoramaview.api.common.PanoramaTaskDoneData
import cn.szu.blankxiao.panoramaview.api.common.PushEventMessage
import cn.szu.blankxiao.panoramaview.data.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

/**
 * 全局 WebSocket 单例：连接后端 /message/ws，接收推送并通过 SharedFlow 分发。
 * 登录时连接，登出时断开；断线后指数退避重连（2s, 4s, 8s… 最大 30s）。
 */
object WebSocketManager {

    private const val WS_PATH = "message/ws"
    private const val TAG = "WebSocketManager"
    private const val INITIAL_RECONNECT_DELAY_MS = 2_000L
    private const val MAX_RECONNECT_DELAY_MS = 30_000L

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val wsRef = AtomicReference<WebSocket?>(null)
    private val reconnectJob = AtomicReference<Job?>(null)
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
    private var tokenManager: TokenManager? = null
    private var okHttpClient: OkHttpClient? = null

    private val _events = MutableSharedFlow<PushEventMessage>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<PushEventMessage> = _events.asSharedFlow()

    /**
     * 在 Application 中调用，传入 Application 上下文。
     * 内部会监听 TokenManager.isLoggedInFlow，登录则连接，登出则断开。
     */
    fun init(context: android.content.Context) {
        if (tokenManager != null) return
        val app = context.applicationContext
        tokenManager = TokenManager.getInstance(app)
        okHttpClient = RetrofitProvider.createOkHttpClient(
            tokenProvider = TokenProvider { runBlocking { TokenManager.getInstance(app).getToken() } },
            enableLogging = true
        )
        scope.launch {
            tokenManager!!.isLoggedInFlow.collect { isLoggedIn ->
                if (isLoggedIn) connect() else disconnect()
            }
        }
    }

    private fun wsBaseUrl(): String {
        val base = RetrofitProvider.BASE_URL
        return base.replace("https://", "wss://").replace("http://", "ws://").trimEnd('/')
    }

    private fun connect() {
        val client = okHttpClient ?: return
        val url = "${wsBaseUrl()}/$WS_PATH"
        val request = Request.Builder().url(url).build()
        wsRef.getAndSet(null)?.close(1000, null)
        wsRef.set(client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened")
                reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseAndEmit(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure", t)
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                wsRef.compareAndSet(webSocket, null)
            }
        }))
    }

    private fun parseAndEmit(text: String) {
        try {
            val json = JSONObject(text)
            val userId = if (json.has("userId")) json.optLong("userId") else null
            val type = json.optString("type", null).takeIf { it?.isNotEmpty() == true }
            val dataObj = json.optJSONObject("data")
            val data: PanoramaTaskDoneData? = if (type == "panorama_task_done" && dataObj != null) {
                PanoramaTaskDoneData(
                    taskId = if (dataObj.has("taskId")) dataObj.optLong("taskId") else null,
                    status = dataObj.optString("status", null).takeIf { it?.isNotEmpty() == true },
                    resultOssUrl = dataObj.optString("resultOssUrl", null).takeIf { it?.isNotEmpty() == true },
                    errorMessage = dataObj.optString("errorMessage", null).takeIf { it?.isNotEmpty() == true }
                )
            } else null
            val message = PushEventMessage(userId = userId, type = type, data = data)
            scope.launch {
                _events.emit(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse push message failed: $text", e)
        }
    }

    private fun scheduleReconnect() {
        reconnectJob.getAndSet(null)?.cancel()
        val job = scope.launch {
            if (!isActive) return@launch
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            val tm = tokenManager ?: return@launch
            if (runBlocking { tm.getToken() != null }) connect()
        }
        reconnectJob.set(job)
    }

    private fun disconnect() {
        reconnectJob.getAndSet(null)?.cancel()
        wsRef.getAndSet(null)?.close(1000, null)
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
    }

    /** 仅用于测试或显式释放，一般由 init + token 流管理即可。 */
    fun shutdown() {
        disconnect()
        scope.cancel()
    }
}
