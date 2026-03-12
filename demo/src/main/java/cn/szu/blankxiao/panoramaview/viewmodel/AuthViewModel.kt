package cn.szu.blankxiao.panoramaview.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.szu.blankxiao.panoramaview.api.auth.AuthApi
import cn.szu.blankxiao.panoramaview.api.auth.dto.LoginRequestDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.LoginResponseDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.RegisterRequestDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.ResetPasswordRequestDto
import cn.szu.blankxiao.panoramaview.api.auth.dto.SendCodeRequestDto
import cn.szu.blankxiao.panoramaview.data.TokenManager
import cn.szu.blankxiao.panoramaview.network.RetrofitProvider
import cn.szu.blankxiao.panoramaview.network.TokenProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class LoginUiState(
    val loading: Boolean = false,
    val errorMsg: String? = null,
    val success: Boolean = false
)

data class CodeUiState(
    val sending: Boolean = false,
    val sent: Boolean = false,
    val errorMsg: String? = null,
    val cooldown: Int = 0
)

data class ResetPwUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val errorMsg: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager.getInstance(application)

    private val okHttpClient = RetrofitProvider.createOkHttpClient(
        tokenProvider = { runBlocking { tokenManager.getToken() } }
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

    private val _codeState = MutableStateFlow(CodeUiState())
    val codeState: StateFlow<CodeUiState> = _codeState.asStateFlow()

    private val _resetPwState = MutableStateFlow(ResetPwUiState())
    val resetPwState: StateFlow<ResetPwUiState> = _resetPwState.asStateFlow()

    // ==================== 密码登录 ====================

    fun loginByPassword(email: String, password: String) {
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
                handleLoginResult(result, email)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "loginByPassword failed", e)
                _loginState.value = LoginUiState(errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    // ==================== 验证码登录 ====================

    fun loginByCode(email: String, code: String) {
        if (email.isBlank() || code.isBlank()) {
            _loginState.value = LoginUiState(errorMsg = "邮箱和验证码不能为空")
            return
        }
        _loginState.value = LoginUiState(loading = true)
        viewModelScope.launch {
            try {
                val result = authApi.login(
                    LoginRequestDto(email = email, code = code, loginType = "EMAIL_CODE")
                )
                handleLoginResult(result, email)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "loginByCode failed", e)
                _loginState.value = LoginUiState(errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    // ==================== 注册 ====================

    fun register(email: String, code: String, username: String, password: String) {
        if (email.isBlank() || code.isBlank() || username.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState(errorMsg = "所有字段不能为空")
            return
        }
        if (password.length < 6) {
            _loginState.value = LoginUiState(errorMsg = "密码长度至少6位")
            return
        }
        _loginState.value = LoginUiState(loading = true)
        viewModelScope.launch {
            try {
                val result = authApi.register(
                    RegisterRequestDto(email = email, code = code, username = username, password = password)
                )
                handleLoginResult(result, email)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "register failed", e)
                _loginState.value = LoginUiState(errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    // ==================== 发送验证码 ====================

    fun sendCode(email: String, type: String) {
        if (email.isBlank()) {
            _codeState.value = CodeUiState(errorMsg = "请输入邮箱")
            return
        }
        _codeState.value = CodeUiState(sending = true)
        viewModelScope.launch {
            try {
                val result = authApi.sendCode(SendCodeRequestDto(email = email, type = type))
                if (result.success == true || result.code == 200) {
                    _codeState.value = CodeUiState(sent = true, cooldown = 60)
                    startCooldown()
                } else {
                    _codeState.value = CodeUiState(errorMsg = result.info ?: "发送失败")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "sendCode failed", e)
                _codeState.value = CodeUiState(errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    private fun startCooldown() {
        viewModelScope.launch {
            var remaining = 60
            while (remaining > 0) {
                delay(1000)
                remaining--
                _codeState.value = _codeState.value.copy(cooldown = remaining)
            }
        }
    }

    // ==================== 重置密码 ====================

    fun resetPassword(email: String, code: String, newPassword: String) {
        if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
            _resetPwState.value = ResetPwUiState(errorMsg = "所有字段不能为空")
            return
        }
        if (newPassword.length < 6) {
            _resetPwState.value = ResetPwUiState(errorMsg = "密码长度至少6位")
            return
        }
        _resetPwState.value = ResetPwUiState(loading = true)
        viewModelScope.launch {
            try {
                val result = authApi.resetPassword(
                    ResetPasswordRequestDto(email = email, code = code, newPassword = newPassword)
                )
                if (result.success == true || result.code == 200) {
                    _resetPwState.value = ResetPwUiState(success = true)
                } else {
                    _resetPwState.value = ResetPwUiState(errorMsg = result.info ?: "重置失败")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "resetPassword failed", e)
                _resetPwState.value = ResetPwUiState(errorMsg = "${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    // ==================== 登出 ====================

    fun logout() {
        viewModelScope.launch {
            try { authApi.logout() } catch (_: Exception) {}
            tokenManager.clear()
            _loginState.value = LoginUiState()
        }
    }

    // ==================== 工具 ====================

    fun clearLoginState() {
        _loginState.value = LoginUiState()
    }

    fun clearCodeState() {
        _codeState.value = CodeUiState()
    }

    fun clearResetPwState() {
        _resetPwState.value = ResetPwUiState()
    }

    private suspend fun handleLoginResult(
        result: cn.szu.blankxiao.panoramaview.api.common.Result<LoginResponseDto?>,
        email: String
    ) {
        if ((result.success == true || result.code == 200) && result.data != null) {
            val data = result.data
            val token = data.token
            if (token.isNullOrBlank()) {
                _loginState.value = LoginUiState(errorMsg = "登录成功但未返回 token")
                return
            }
            tokenManager.saveLogin(token, data.userId, data.email ?: email, data.username)
            _loginState.value = LoginUiState(success = true)
        } else {
            _loginState.value = LoginUiState(errorMsg = result.info ?: "操作失败")
        }
    }
}
