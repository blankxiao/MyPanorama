package cn.szu.blankxiao.panoramaview.api.panorama.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PanoramaInputImageUploadSignDto(
    @Json(name = "uploadUrl") val uploadUrl: String,
    @Json(name = "objectKey") val objectKey: String,
    @Json(name = "policy") val policy: String,
    @Json(name = "signature") val signature: String,
    @Json(name = "accessKeyId") val accessKeyId: String,
    @Json(name = "contentType") val contentType: String,
    @Json(name = "successActionStatus") val successActionStatus: String,
    @Json(name = "expireAt") val expireAt: String
)
