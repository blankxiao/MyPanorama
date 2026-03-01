package cn.szu.blankxiao.panoramaview.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 登录请求。loginType: EMAIL_CODE | EMAIL_PASSWORD
 */
@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "code") val code: String? = null,
    @Json(name = "password") val password: String? = null,
    @Json(name = "loginType") val loginType: String
)
