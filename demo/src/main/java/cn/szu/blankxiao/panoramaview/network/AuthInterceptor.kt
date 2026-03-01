package cn.szu.blankxiao.panoramaview.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 为需鉴权接口添加 Authorization 头（与后端 Sa-Token 约定一致）。
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()
        val request = chain.request().newBuilder()
        if (!token.isNullOrBlank()) {
            request.addHeader("Authorization", token.trim())
        }
        return chain.proceed(request.build())
    }
}
