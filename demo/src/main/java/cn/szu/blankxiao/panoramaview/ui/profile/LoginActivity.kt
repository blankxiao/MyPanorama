package cn.szu.blankxiao.panoramaview.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val vm: AuthViewModel by viewModels()

    private lateinit var flipper: ViewFlipper
    private lateinit var tvTitle: TextView
    private lateinit var tvError: TextView
    private lateinit var progress: CircularProgressIndicator

    companion object {
        private const val PAGE_PASSWORD_LOGIN = 0
        private const val PAGE_CODE_LOGIN = 1
        private const val PAGE_REGISTER = 2
        private const val PAGE_RESET_PW = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        flipper = findViewById(R.id.view_flipper)
        tvTitle = findViewById(R.id.tv_page_title)
        tvError = findViewById(R.id.tv_error)
        progress = findViewById(R.id.progress)

        setupPasswordLogin()
        setupCodeLogin()
        setupRegister()
        setupResetPassword()
        observeState()
    }

    // ==================== Page 0: 密码登录 ====================

    private fun setupPasswordLogin() {
        val etEmail = findViewById<TextInputEditText>(R.id.et_login_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_login_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login_password)

        btnLogin.setOnClickListener {
            vm.loginByPassword(
                etEmail.text?.toString().orEmpty(),
                etPassword.text?.toString().orEmpty()
            )
        }

        findViewById<View>(R.id.tv_switch_to_code_login).setOnClickListener {
            switchTo(PAGE_CODE_LOGIN, getString(R.string.subtitle_code_login))
        }
        findViewById<View>(R.id.tv_forgot_password).setOnClickListener {
            switchTo(PAGE_RESET_PW, getString(R.string.subtitle_reset_pw))
        }
        findViewById<View>(R.id.tv_go_register).setOnClickListener {
            switchTo(PAGE_REGISTER, getString(R.string.subtitle_register))
        }
    }

    // ==================== Page 1: 验证码登录 ====================

    private fun setupCodeLogin() {
        val etEmail = findViewById<TextInputEditText>(R.id.et_code_login_email)
        val etCode = findViewById<TextInputEditText>(R.id.et_code_login_code)
        val btnSend = findViewById<MaterialButton>(R.id.btn_send_login_code)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login_code)

        btnSend.setOnClickListener {
            vm.sendCode(etEmail.text?.toString().orEmpty(), "LOGIN")
        }
        btnLogin.setOnClickListener {
            vm.loginByCode(
                etEmail.text?.toString().orEmpty(),
                etCode.text?.toString().orEmpty()
            )
        }
        findViewById<View>(R.id.tv_switch_to_password_login).setOnClickListener {
            switchTo(PAGE_PASSWORD_LOGIN, getString(R.string.login_subtitle))
        }
    }

    // ==================== Page 2: 注册 ====================

    private fun setupRegister() {
        val etEmail = findViewById<TextInputEditText>(R.id.et_reg_email)
        val etCode = findViewById<TextInputEditText>(R.id.et_reg_code)
        val etUsername = findViewById<TextInputEditText>(R.id.et_reg_username)
        val etPassword = findViewById<TextInputEditText>(R.id.et_reg_password)
        val btnSend = findViewById<MaterialButton>(R.id.btn_send_reg_code)
        val btnRegister = findViewById<MaterialButton>(R.id.btn_register)

        btnSend.setOnClickListener {
            vm.sendCode(etEmail.text?.toString().orEmpty(), "REGISTER")
        }
        btnRegister.setOnClickListener {
            vm.register(
                etEmail.text?.toString().orEmpty(),
                etCode.text?.toString().orEmpty(),
                etUsername.text?.toString().orEmpty(),
                etPassword.text?.toString().orEmpty()
            )
        }
        findViewById<View>(R.id.tv_go_login_from_reg).setOnClickListener {
            switchTo(PAGE_PASSWORD_LOGIN, getString(R.string.login_subtitle))
        }
    }

    // ==================== Page 3: 忘记密码 ====================

    private fun setupResetPassword() {
        val etEmail = findViewById<TextInputEditText>(R.id.et_reset_email)
        val etCode = findViewById<TextInputEditText>(R.id.et_reset_code)
        val etNewPw = findViewById<TextInputEditText>(R.id.et_reset_new_password)
        val btnSend = findViewById<MaterialButton>(R.id.btn_send_reset_code)
        val btnReset = findViewById<MaterialButton>(R.id.btn_reset_password)

        btnSend.setOnClickListener {
            vm.sendCode(etEmail.text?.toString().orEmpty(), "RESET_PASSWORD")
        }
        btnReset.setOnClickListener {
            vm.resetPassword(
                etEmail.text?.toString().orEmpty(),
                etCode.text?.toString().orEmpty(),
                etNewPw.text?.toString().orEmpty()
            )
        }
        findViewById<View>(R.id.tv_go_login_from_reset).setOnClickListener {
            switchTo(PAGE_PASSWORD_LOGIN, getString(R.string.login_subtitle))
        }
    }

    // ==================== 状态观察 ====================

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.loginState.collect { state ->
                        showLoading(state.loading)
                        showError(state.errorMsg)
                        if (state.success) finish()
                    }
                }
                launch {
                    vm.codeState.collect { state ->
                        val sendButtons = listOf(
                            R.id.btn_send_login_code, R.id.btn_send_reg_code, R.id.btn_send_reset_code
                        )
                        for (id in sendButtons) {
                            val btn = findViewById<MaterialButton>(id)
                            btn.isEnabled = !state.sending && state.cooldown == 0
                            btn.text = if (state.cooldown > 0)
                                "${state.cooldown}s"
                            else
                                getString(R.string.send_code)
                        }
                        if (state.sent && state.cooldown == 60) {
                            Toast.makeText(this@LoginActivity, R.string.code_sent, Toast.LENGTH_SHORT).show()
                        }
                        showError(state.errorMsg)
                    }
                }
                launch {
                    vm.resetPwState.collect { state ->
                        showLoading(state.loading)
                        showError(state.errorMsg)
                        if (state.success) {
                            Toast.makeText(this@LoginActivity, R.string.reset_pw_success, Toast.LENGTH_SHORT).show()
                            vm.clearResetPwState()
                            switchTo(PAGE_PASSWORD_LOGIN, getString(R.string.login_subtitle))
                        }
                    }
                }
            }
        }
    }

    // ==================== 工具 ====================

    private fun switchTo(page: Int, subtitle: String) {
        flipper.displayedChild = page
        tvTitle.text = subtitle
        tvError.visibility = View.GONE
        vm.clearLoginState()
        vm.clearCodeState()
        vm.clearResetPwState()
    }

    private fun showLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(msg: String?) {
        if (msg != null) {
            tvError.text = msg
            tvError.visibility = View.VISIBLE
        } else {
            tvError.visibility = View.GONE
        }
    }
}
