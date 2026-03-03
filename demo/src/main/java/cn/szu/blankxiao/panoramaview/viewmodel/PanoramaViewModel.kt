package cn.szu.blankxiao.panoramaview.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.szu.blankxiao.panoramaview.api.common.PushEventMessage
import cn.szu.blankxiao.panoramaview.api.panorama.PanoramaApi
import cn.szu.blankxiao.panoramaview.api.panorama.dto.CreateTaskRequestDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaTaskDetailDto
import cn.szu.blankxiao.panoramaview.api.panorama.dto.PanoramaTaskListItemDto
import cn.szu.blankxiao.panoramaview.data.TokenManager
import cn.szu.blankxiao.panoramaview.network.RetrofitProvider
import cn.szu.blankxiao.panoramaview.network.TokenProvider
import cn.szu.blankxiao.panoramaview.network.WebSocketManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

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
        _createState.value = CreateTaskUiState(loading = true)
        viewModelScope.launch {
            try {
                val request = CreateTaskRequestDto(
                    prompt = prompt,
                    name = name?.ifBlank { null },
                    mode = mode,
                    inputImageUrl = inputImageUrl?.ifBlank { null }
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
    }
}
