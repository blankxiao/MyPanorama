package cn.szu.blankxiao.panoramaview.api.panorama.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 创建全景图任务请求，与后端 CreateTaskRequest 一致。
 * mode: text2pano | outpaint
 */
@JsonClass(generateAdapter = true)
data class CreateTaskRequestDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "prompt") val prompt: String,
    @Json(name = "mode") val mode: String? = null,
    @Json(name = "inputImageUrl") val inputImageUrl: String? = null
)
