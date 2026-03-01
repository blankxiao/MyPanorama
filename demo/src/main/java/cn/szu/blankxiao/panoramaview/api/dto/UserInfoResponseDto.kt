package cn.szu.blankxiao.panoramaview.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserInfoResponseDto(
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "status") val status: Int? = null,
    @Json(name = "createTime") val createTime: String? = null
)
