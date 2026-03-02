package cn.szu.blankxiao.panoramaview.api.auth.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * type: LOGIN / REGISTER / RESET_PASSWORD
 */
@JsonClass(generateAdapter = true)
data class SendCodeRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "type") val type: String
)
