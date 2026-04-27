package cn.szu.blankxiao.panoramaview.api.panorama.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PanoramaInputImageConfirmRequestDto(
    @Json(name = "objectKey") val objectKey: String,
    @Json(name = "ossUrl") val ossUrl: String? = null,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "sizeBytes") val sizeBytes: Long,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null
)
