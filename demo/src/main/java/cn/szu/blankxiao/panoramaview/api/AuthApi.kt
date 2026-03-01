package cn.szu.blankxiao.panoramaview.api

import cn.szu.blankxiao.panoramaview.api.dto.LoginRequestDto
import cn.szu.blankxiao.panoramaview.api.dto.LoginResponseDto
import cn.szu.blankxiao.panoramaview.api.dto.Result
import cn.szu.blankxiao.panoramaview.api.dto.UserInfoResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 认证后端接口，路径与网关 /auth/ 一致。
 */
interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Result<LoginResponseDto?>

    @POST("auth/logout")
    suspend fun logout(): Result<Boolean?>

    @GET("auth/user-info")
    suspend fun getUserInfo(): Result<UserInfoResponseDto?>
}
