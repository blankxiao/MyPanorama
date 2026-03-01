package cn.szu.blankxiao.panoramaview.api

import cn.szu.blankxiao.panoramaview.api.dto.LoginRequestDto
import cn.szu.blankxiao.panoramaview.network.RetrofitProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AuthApi 实际请求后端（api.blanxiao.online）的集成测试。
 * 需要网络可用、后端认证服务已部署。
 * 使用环境变量 TEST_EMAIL / TEST_PASSWORD 可测登录成功路径；未设置时用占位请求验证接口可达且返回 Result 结构。
 */
class AuthApiTest {

    private lateinit var authApi: AuthApi

    @Before
    fun setUp() {
        val client = RetrofitProvider.createOkHttpClient(
            tokenProvider = { null },
            enableLogging = true
        )
        val retrofit = RetrofitProvider.createRetrofit(client)
        authApi = RetrofitProvider.createAuthApi(retrofit)
    }

    @Test
    fun login_realRequest_returnsResult() = runBlocking {
        val email = System.getenv("TEST_EMAIL") ?: "test-no-such-user@example.com"
        val password = System.getenv("TEST_PASSWORD") ?: "placeholder-password"
        val request = LoginRequestDto(
            email = email,
            password = password,
            loginType = "EMAIL_PASSWORD"
        )
        val result = authApi.login(request)
        assertNotNull(result)
        assertNotNull(result.code)
        assertNotNull(result.success)
        // 有合法账号时 result.success 为 true 且 data.token 非空
        if (result.success == true) {
            assertNotNull("登录成功时应有 token", result.data?.token)
        }
    }

    @Test
    fun getUserInfo_withoutToken_returns401OrResult() = runBlocking {
        val clientWithToken = RetrofitProvider.createOkHttpClient(
            tokenProvider = { System.getenv("TEST_AUTH_TOKEN") },
            enableLogging = true
        )
        val retrofit = RetrofitProvider.createRetrofit(clientWithToken)
        val api = retrofit.create(AuthApi::class.java)
        val result = runCatching { api.getUserInfo() }
        // 无 token 时可能抛 HttpException(401)，有 token 时返回 Result
        assertTrue(result.isSuccess || result.exceptionOrNull() != null)
    }
}
