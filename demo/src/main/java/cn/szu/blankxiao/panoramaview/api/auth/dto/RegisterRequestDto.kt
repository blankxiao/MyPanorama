package cn.szu.blankxiao.panoramaview.api.auth.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "code") val code: String,
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)
