package cn.szu.blankxiao.panoramaview.api.panorama.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PanoramaInputImageAssetDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "ossUrl") val ossUrl: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null,
    @Json(name = "sizeBytes") val sizeBytes: Long? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)
