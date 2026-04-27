package cn.szu.blankxiao.panoramaview.api.panorama.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PanoramaInputImageUploadSignRequestDto(
    @Json(name = "fileName") val fileName: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "sizeBytes") val sizeBytes: Long
)
