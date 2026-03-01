package cn.szu.blankxiao.panoramaview.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PanoramaTaskListItemDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "mode") val mode: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "resultOssUrl") val resultOssUrl: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)
