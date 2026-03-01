package cn.szu.blankxiao.panoramaview.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.szu.blankxiao.panoramaview.api.AuthApi
import cn.szu.blankxiao.panoramaview.api.dto.LoginRequestDto
import cn.szu.blankxiao.panoramaview.data.TokenManager
import cn.szu.blankxiao.panoramaview.network.RetrofitProvider
import cn.szu.blankxiao.panoramaview.network.TokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val errorMsg: String? = null,
    val success: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager.getInstance(application)

    private val okHttpClient = RetrofitProvider.createOkHttpClient(
        tokenProvider = TokenProvider { kotlinx.coroutines.runBlocking { tokenManager.getToken() } }
    )
    private val retrofit = RetrofitProvider.createRetrofit(okHttpClient)
    private val authApi: AuthApi = RetrofitProvider.createAuthApi(retrofit)

    val isLoggedIn: StateFlow<Boolean> = tokenManager.isLoggedInFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val email: StateFlow<String?> = tokenManager.emailFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val username: StateFlow<String?> = tokenManager.usernameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState(errorMsg = "邮箱和密码不能为空")
            return
        }
        _loginState.value = LoginUiState(loading = true)
        viewModelScope.launch {
            try {
                val result = authApi.login(
                    LoginRequestDto(email = email, password = password, loginType = "EMAIL_PASSWORD")
                )
                if ((result.success == true || result.code == 200) && result.data != null) {
                    val data = result.data
                    val token = data.token
                    if (token.isNullOrBlank()) {
                        _loginState.value = LoginUiState(errorMsg = "登录成功但未返回 token")
                        return@launch
                    }
                    tokenManager.saveLogin(token, data.userId, data.email ?: email, data.username)
                    _loginState.value = LoginUiState(success = true)
                } else {
                    _loginState.value = LoginUiState(errorMsg = result.info ?: "登录失败")
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState(errorMsg = e.message ?: "网络错误")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authApi.logout()
            } catch (_: Exception) {
            }
            tokenManager.clear()
            _loginState.value = LoginUiState()
        }
    }

    fun clearError() {
        _loginState.value = _loginState.value.copy(errorMsg = null)
    }
}
