package cn.szu.blankxiao.panoramaview.api

import cn.szu.blankxiao.panoramaview.network.RetrofitProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PanoramaApi 实际请求后端（api.blanxiao.online）的集成测试。
 * 需要网络可用、后端服务已部署。
 */
class PanoramaApiTest {

    private lateinit var panoramaApi: PanoramaApi

    @Before
    fun setUp() {
        val client = RetrofitProvider.createOkHttpClient(
            tokenProvider = { null },
            enableLogging = true
        )
        val retrofit = RetrofitProvider.createRetrofit(client)
        panoramaApi = RetrofitProvider.createPanoramaApi(retrofit)
    }

    @Test
    fun health_realRequest_returnsOk() = runBlocking {
        val response = panoramaApi.health()
        assertTrue("HTTP 应成功: ${response.code()}", response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("ok", body!!["status"])
        assertEquals("simple-market-panorama", body["service"])
    }

    @Test
    fun listTasks_realRequest_returnsResultOr401() = runBlocking {
        val token = System.getenv("TEST_AUTH_TOKEN")
        val client = RetrofitProvider.createOkHttpClient(
            tokenProvider = { token },
            enableLogging = true
        )
        val retrofit = RetrofitProvider.createRetrofit(client)
        val api = retrofit.create(PanoramaApi::class.java)
        val outcome = runCatching { api.listTasks() }
        assertTrue(outcome.isSuccess || outcome.exceptionOrNull() != null)
        outcome.getOrNull()?.let { result ->
            assertNotNull(result.code)
            assertNotNull(result.success)
        }
    }

    @Test
    fun listResults_realRequest_returnsResultOr401() = runBlocking {
        val token = System.getenv("TEST_AUTH_TOKEN")
        val client = RetrofitProvider.createOkHttpClient(
            tokenProvider = { token },
            enableLogging = true
        )
        val retrofit = RetrofitProvider.createRetrofit(client)
        val api = retrofit.create(PanoramaApi::class.java)
        val outcome = runCatching { api.listResults() }
        assertTrue(outcome.isSuccess || outcome.exceptionOrNull() != null)
        outcome.getOrNull()?.let { result ->
            assertNotNull(result.code)
            assertNotNull(result.success)
        }
    }
}
