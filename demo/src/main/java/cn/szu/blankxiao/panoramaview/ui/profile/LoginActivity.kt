package cn.szu.blankxiao.panoramaview.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
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

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val progress = findViewById<CircularProgressIndicator>(R.id.progress)

        btnLogin.setOnClickListener {
            viewModel.login(
                email = etEmail.text?.toString().orEmpty(),
                password = etPassword.text?.toString().orEmpty()
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    progress.visibility = if (state.loading) View.VISIBLE else View.GONE
                    btnLogin.isEnabled = !state.loading

                    if (state.errorMsg != null) {
                        tvError.text = state.errorMsg
                        tvError.visibility = View.VISIBLE
                    } else {
                        tvError.visibility = View.GONE
                    }

                    if (state.success) {
                        finish()
                    }
                }
            }
        }
    }
}