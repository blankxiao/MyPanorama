package cn.szu.blankxiao.panoramaview.api.auth

import cn.szu.blankxiao.panoramaview.api.auth.dto.LoginRequestDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.LoginResponseDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.RegisterRequestDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.ResetPasswordRequestDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.SendCodeRequestDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.UserInfoResponseDto
import cn.szu.blankxiao.panoramaview.api.common.Result
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Result<LoginResponseDto?>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Result<LoginResponseDto?>

    @POST("auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequestDto): Result<Boolean?>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): Result<Boolean?>

    @POST("auth/logout")
    suspend fun logout(): Result<Boolean?>

    @GET("auth/user-info")
    suspend fun getUserInfo(): Result<UserInfoResponseDto?>
}
