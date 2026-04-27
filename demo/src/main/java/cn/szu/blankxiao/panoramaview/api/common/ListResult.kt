package cn.szu.blankxiao.panoramaview.api.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ListResult<T>(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "code") val code: Int? = null,
    @Json(name = "info") val info: String? = null,
    @Json(name = "data") val data: List<T>? = null,
    @Json(name = "total") val total: Int? = null,
    @Json(name = "page") val page: Int? = null,
    @Json(name = "pageSize") val pageSize: Int? = null
)
