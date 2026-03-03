package cn.szu.blankxiao.panoramaview.api.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * WebSocket 推送消息，与后端 PushEventMessage 一致。
 * data 为业务载荷，type 为 "panorama_task_done" 时对应 PanoramaTaskDoneData。
 */
@JsonClass(generateAdapter = true)
data class PushEventMessage(
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "data") val data: PanoramaTaskDoneData? = null
)

@JsonClass(generateAdapter = true)
data class PanoramaTaskDoneData(
    @Json(name = "taskId") val taskId: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "resultOssUrl") val resultOssUrl: String? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null
)
