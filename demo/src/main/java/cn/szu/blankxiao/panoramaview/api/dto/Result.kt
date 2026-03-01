package cn.szu.blankxiao.panoramaview.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 与后端 Result 一致：success, code, info, data
 */
@JsonClass(generateAdapter = true)
data class Result<T>(
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "code") val code: Int? = null,
    @Json(name = "info") val info: String? = null,
    @Json(name = "data") val data: T? = null
)
