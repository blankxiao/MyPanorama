package cn.szu.blankxiao.panoramaview.network

import cn.szu.blankxiao.panoramaview.api.AuthApi
import cn.szu.blankxiao.panoramaview.api.PanoramaApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 后端 API 配置：baseUrl 硬编码，OkHttp 拦截器（鉴权 + 日志），Retrofit + Moshi。
 */
object RetrofitProvider {

    const val BASE_URL = "https://api.blanxiao.online/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * 创建带鉴权与日志的 OkHttpClient。
     * @param tokenProvider 未登录可传 { null }，登录后传入从 DataStore 取 token 的 lambda
     */
    fun createOkHttpClient(
        tokenProvider: TokenProvider = TokenProvider { null },
        enableLogging: Boolean = true
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenProvider))
        if (enableLogging) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun createPanoramaApi(retrofit: Retrofit): PanoramaApi =
        retrofit.create(PanoramaApi::class.java)

    fun createAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)
}
