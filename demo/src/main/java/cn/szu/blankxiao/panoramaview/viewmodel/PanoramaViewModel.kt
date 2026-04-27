package cn.szu.blankxiao.panoramaview.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.szu.blankxiao.panoramaview.api.panorama.PanoramaApi
import cn.szu.blankxiao.panoramaview.api.panorama.dto.CreateTaskRequestDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageAssetDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageConfirmRequestDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageUploadSignDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaInputImageUploadSignRequestDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaTaskDetailDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaTaskListItemDto
import cn.szu.blankxiao.panoramaview.data.TokenManager
import cn.szu.blankxiao.panoramaview.network.RetrofitProvider
import cn.szu.blankxiao.panoramaview.network.TokenProvider
import cn.szu.blankxiao.panoramaview.network.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.Locale

private data class UploadPayload(
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val bytes: ByteArray
)

data class CreateTaskUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val taskId: Long? = null,
    val errorMsg: String? = null
)

data class TaskListUiState(
    val refreshing: Boolean = false,
    val tasks: List<PanoramaTaskListItemDto> = emptyList(),
    val errorMsg: String? = null
)

data class ResultListUiState(
    val refreshing: Boolean = false,
    val results: List<PanoramaTaskListItemDto> = emptyList(),
    val errorMsg: String? = null
)

data class TaskDetailUiState(
    val loading: Boolean = false,
    val detail: PanoramaTaskDetailDto? = null,
    val errorMsg: String? = null
)

data class InputImageUiState(
    val loading: Boolean = false,
    val uploading: Boolean = false,
    val assets: List<PanoramaInputImageAssetDto> = emptyList(),
    val selectedImageUrl: String? = null,
    val selectedPreviewUrl: String? = null,
    val errorMsg: String? = null
)

class PanoramaViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager.getInstance(application)

    private val okHttpClient = RetrofitProvider.createOkHttpClient(
        tokenProvider = TokenProvider { kotlinx.coroutines.runBlocking { tokenManager.getToken() } }
    )
    private val retrofit = RetrofitProvider.createRetrofit(okHttpClient)
    private val panoramaApi: PanoramaApi = RetrofitProvider.createPanoramaApi(retrofit)

    private val _createState = MutableStateFlow(CreateTaskUiState())
    val createState: StateFlow<CreateTaskUiState> = _createState.asStateFlow()

    private val _taskListState = MutableStateFlow(TaskListUiState())
    val taskListState: StateFlow<TaskListUiState> = _taskListState.asStateFlow()

    private val _resultListState = MutableStateFlow(ResultListUiState())
    val resultListState: StateFlow<ResultListUiState> = _resultListState.asStateFlow()

    private val _detailState = MutableStateFlow(TaskDetailUiState())
    val detailState: StateFlow<TaskDetailUiState> = _detailState.asStateFlow()

    private val _inputImageState = MutableStateFlow(InputImageUiState())
    val inputImageState: StateFlow<InputImageUiState> = _inputImageState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 16)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            WebSocketManager.events
                .filter { it.type == "panorama_task_done" }
                .collect { event ->
                    loadTaskList()
                    loadResultList()
                    val msg = event.data?.let { d ->
                        if (d.status == "completed") "任务 #${d.taskId} 已完成"
                        else "任务 #${d.taskId}: ${d.status}${d.errorMessage?.let { " - $it" }.orEmpty()}"
                    } ?: "任务已完成"
                    _toastMessage.emit(msg)
                }
        }
    }

    fun createTask(prompt: String, name: String? = null, mode: String? = null, inputImageUrl: String? = null) {
        if (prompt.isBlank()) {
            _createState.value = CreateTaskUiState(errorMsg = "提示词不能为空")
            return
        }
        val finalMode = mode?.ifBlank { null } ?: "text2pano"
        val finalInputImageUrl = if (finalMode == "outpaint") {
            inputImageUrl?.ifBlank { null } ?: _inputImageState.value.selectedImageUrl
        } else {
            null
        }
        if (finalMode == "outpaint" && finalInputImageUrl.isNullOrBlank()) {
            _createState.value = CreateTaskUiState(errorMsg = "outpaint 模式请先选择或上传原图")
            return
        }

        _createState.value = CreateTaskUiState(loading = true)
        viewModelScope.launch {
            try {
                val request = CreateTaskRequestDto(
                    prompt = prompt,
                    name = name?.ifBlank { null },
                    mode = finalMode,
                    inputImageUrl = finalInputImageUrl
                )
                val result = panoramaApi.createTask(request)
                if (result.success == true || result.code == 200) {
                    _createState.value = CreateTaskUiState(success = true, taskId = result.data)
                } else {
                    _createState.value = CreateTaskUiState(errorMsg = result.info ?: "创建失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "createTask failed", e)
                _createState.value = CreateTaskUiState(errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    fun clearCreateState() {
        _createState.value = CreateTaskUiState()
    }

    fun ensureInputImagesLoaded() {
        val state = _inputImageState.value
        if (state.loading || state.assets.isNotEmpty()) {
            return
        }
        loadInputImageAssets()
    }

    fun loadInputImageAssets(page: Int = 1, pageSize: Int = 20) {
        val current = _inputImageState.value
        _inputImageState.value = current.copy(loading = true, errorMsg = null)
        viewModelScope.launch {
            try {
                val result = panoramaApi.listMyInputImages(page = page, pageSize = pageSize)
                if (result.success == true || result.code == 200) {
                    val assets = result.data.orEmpty()
                    val selected = current.selectedImageUrl?.takeIf { selectedUrl ->
                        assets.any { it.ossUrl == selectedUrl }
                    }
                    _inputImageState.value = current.copy(
                        loading = false,
                        assets = assets,
                        selectedImageUrl = selected,
                        selectedPreviewUrl = selected,
                        errorMsg = null
                    )
                } else {
                    _inputImageState.value = current.copy(loading = false, errorMsg = result.info ?: "原图列表加载失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadInputImageAssets failed", e)
                _inputImageState.value = current.copy(loading = false, errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    fun selectInputImage(asset: PanoramaInputImageAssetDto) {
        val ossUrl = asset.ossUrl ?: return
        val current = _inputImageState.value
        _inputImageState.value = current.copy(
            selectedImageUrl = ossUrl,
            selectedPreviewUrl = ossUrl,
            errorMsg = null
        )
    }

    fun uploadInputImage(uri: Uri) {
        val current = _inputImageState.value
        _inputImageState.value = current.copy(uploading = true, selectedPreviewUrl = uri.toString(), errorMsg = null)
        viewModelScope.launch {
            try {
                Log.i(TAG, "uploadInputImage: start uri=$uri")
                val payload = buildUploadPayload(uri)
                Log.i(
                    TAG,
                    "uploadInputImage: payload prepared name=${payload.fileName}, type=${payload.contentType}, size=${payload.sizeBytes}, wh=${payload.width}x${payload.height}"
                )
                val sign = requestUploadSign(payload)
                Log.i(
                    TAG,
                    "uploadInputImage: sign received objectKey=${sign.objectKey}, uploadHost=${android.net.Uri.parse(sign.uploadUrl).host}"
                )
                val uploaded = uploadToOss(sign, payload)
                if (!uploaded) {
                    throw IllegalStateException("OSS 上传失败")
                }
                Log.i(TAG, "uploadInputImage: oss upload success objectKey=${sign.objectKey}")
                val confirmResult = panoramaApi.confirmInputImageUpload(
                    PanoramaInputImageConfirmRequestDto(
                        objectKey = sign.objectKey,
                        ossUrl = null,
                        contentType = payload.contentType,
                        sizeBytes = payload.sizeBytes,
                        width = payload.width,
                        height = payload.height
                    )
                )
                if (confirmResult.success != true && confirmResult.code != 200) {
                    throw IllegalStateException(confirmResult.info ?: "上传确认失败")
                }
                val confirmed = confirmResult.data ?: throw IllegalStateException("上传确认无返回数据")
                Log.i(TAG, "uploadInputImage: confirm success ossUrl=${confirmed.ossUrl}")
                _inputImageState.value = _inputImageState.value.copy(
                    uploading = false,
                    selectedImageUrl = confirmed.ossUrl,
                    selectedPreviewUrl = confirmed.ossUrl,
                    errorMsg = null
                )
                _toastMessage.emit("原图上传成功")
                loadInputImageAssets()
            } catch (e: Exception) {
                Log.e(TAG, "uploadInputImage failed", e)
                _inputImageState.value = _inputImageState.value.copy(
                    uploading = false,
                    errorMsg = "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    private suspend fun requestUploadSign(payload: UploadPayload): PanoramaInputImageUploadSignDto {
        Log.d(TAG, "requestUploadSign: request fileName=${payload.fileName}, size=${payload.sizeBytes}, type=${payload.contentType}")
        val signResult = panoramaApi.uploadInputImageSign(
            PanoramaInputImageUploadSignRequestDto(
                fileName = payload.fileName,
                contentType = payload.contentType,
                sizeBytes = payload.sizeBytes
            )
        )
        if (signResult.success == true || signResult.code == 200) {
            Log.d(TAG, "requestUploadSign: success code=${signResult.code}")
            val sign = signResult.data ?: throw IllegalStateException("上传签名为空")
            val policyDecoded = try {
                String(Base64.getDecoder().decode(sign.policy))
            } catch (e: Exception) {
                "<policy decode failed: ${e.javaClass.simpleName}>"
            }
            Log.d(
                TAG,
                "requestUploadSign: objectKey=${sign.objectKey}, expireAt=${sign.expireAt}, uploadUrl=${sign.uploadUrl}, policy=${policyDecoded}"
            )
            return sign
        }
        Log.w(TAG, "requestUploadSign: failed code=${signResult.code}, info=${signResult.info}")
        throw IllegalStateException(signResult.info ?: "获取上传签名失败")
    }

    private suspend fun uploadToOss(sign: PanoramaInputImageUploadSignDto, payload: UploadPayload): Boolean =
        withContext(Dispatchers.IO) {
            val fileBody = payload.bytes.toRequestBody(payload.contentType.toMediaTypeOrNull())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", sign.objectKey)
                .addFormDataPart("policy", sign.policy)
                .addFormDataPart("OSSAccessKeyId", sign.accessKeyId)
                .addFormDataPart("signature", sign.signature)
                .addFormDataPart("success_action_status", sign.successActionStatus)
                .addFormDataPart("Content-Type", payload.contentType)
                .addFormDataPart("file", payload.fileName, fileBody)
                .build()

            val request = Request.Builder()
                .url(sign.uploadUrl)
                .post(multipart)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val requestId = response.header("x-oss-request-id")
                    ?: response.header("x-oss-requestid")
                    ?: response.header("x-oss-request-id".uppercase(Locale.ROOT))
                val responseBody = response.body?.string().orEmpty()
                val responseHeaders = response.headers.toMultimap()
                Log.d(
                    TAG,
                    "uploadToOss: response code=${response.code}, success=${response.isSuccessful}, requestId=${requestId ?: "-"}, headers=${responseHeaders}, body=${responseBody}"
                )
                response.isSuccessful
            }
        }

    private suspend fun buildUploadPayload(uri: Uri): UploadPayload = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        var fileName: String? = null
        var declaredSize: Long? = null
        var contentType = resolver.getType(uri)?.lowercase(Locale.ROOT)

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    declaredSize = cursor.getLong(sizeIndex)
                }
            }
        }

        if (contentType.isNullOrBlank()) {
            val ext = fileName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT)
            contentType = when (ext) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> null
            }
        }
        if (contentType.isNullOrBlank()) {
            throw IllegalArgumentException("无法识别图片类型，请选择 jpg/png/webp")
        }

        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("无法读取所选图片")
        val finalName = fileName ?: "input_${System.currentTimeMillis()}.jpg"
        val rawSize = declaredSize ?: rawBytes.size.toLong()

        val processed = preprocessForOutpaint(rawBytes)
        val bytes = processed.first
        contentType = processed.second
        val sizeBytes = bytes.size.toLong()

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val width = options.outWidth.takeIf { it > 0 }
        val height = options.outHeight.takeIf { it > 0 }

        Log.d(
            TAG,
            "buildUploadPayload: rawSize=${rawSize}, processedSize=${sizeBytes}, processedType=${contentType}, processedWh=${width}x${height}"
        )

        UploadPayload(
            fileName = finalName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            bytes = bytes
        )
    }

    private fun preprocessForOutpaint(rawBytes: ByteArray): Pair<ByteArray, String> {
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: throw IllegalArgumentException("图片解码失败")

        val square = centerCropSquare(decoded)
        val resized = if (square.width == OUTPAINT_TARGET_SIZE && square.height == OUTPAINT_TARGET_SIZE) {
            square
        } else {
            Bitmap.createScaledBitmap(square, OUTPAINT_TARGET_SIZE, OUTPAINT_TARGET_SIZE, true)
        }

        val out = java.io.ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, OUTPAINT_JPEG_QUALITY, out)

        if (resized !== square) {
            resized.recycle()
        }
        if (square !== decoded) {
            square.recycle()
        }
        decoded.recycle()

        return out.toByteArray() to "image/jpeg"
    }

    private fun centerCropSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    fun loadTaskList() {
        val current = _taskListState.value
        _taskListState.value = current.copy(refreshing = true, errorMsg = null)
        viewModelScope.launch {
            try {
                val result = panoramaApi.listTasks()
                if (result.success == true || result.code == 200) {
                    _taskListState.value = TaskListUiState(tasks = result.data.orEmpty())
                } else {
                    _taskListState.value = current.copy(refreshing = false, errorMsg = result.info ?: "加载失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadTaskList failed", e)
                _taskListState.value = current.copy(refreshing = false, errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    fun loadResultList() {
        val current = _resultListState.value
        _resultListState.value = current.copy(refreshing = true, errorMsg = null)
        viewModelScope.launch {
            try {
                val result = panoramaApi.listResults()
                if (result.success == true || result.code == 200) {
                    _resultListState.value = ResultListUiState(results = result.data.orEmpty())
                } else {
                    _resultListState.value = current.copy(refreshing = false, errorMsg = result.info ?: "加载失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadResultList failed", e)
                _resultListState.value = current.copy(refreshing = false, errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    fun loadTaskDetail(taskId: Long) {
        _detailState.value = TaskDetailUiState(loading = true)
        viewModelScope.launch {
            try {
                val result = panoramaApi.taskDetail(taskId)
                if (result.success == true || result.code == 200) {
                    _detailState.value = TaskDetailUiState(detail = result.data)
                } else {
                    _detailState.value = TaskDetailUiState(errorMsg = result.info ?: "加载失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadTaskDetail failed", e)
                _detailState.value = TaskDetailUiState(errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                val result = panoramaApi.deleteTask(taskId)
                if (result.success == true || result.code == 200) {
                    loadTaskList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteTask failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "PanoramaViewModel"
        private const val OUTPAINT_TARGET_SIZE = 512
        private const val OUTPAINT_JPEG_QUALITY = 92
    }
}
