package cn.szu.blankxiao.panoramaview.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private val viewModel: AuthViewModel by activityViewModels()

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvUserStatus = view.findViewById<TextView>(R.id.tv_user_status)
        val itemLogin = view.findViewById<LinearLayout>(R.id.item_login)
        val icLogin = view.findViewById<ImageView>(R.id.ic_login)
        val tvLoginLabel = view.findViewById<TextView>(R.id.tv_login_label)
        val tvLoginHint = view.findViewById<TextView>(R.id.tv_login_hint)
        val itemAbout = view.findViewById<LinearLayout>(R.id.item_about)

        itemAbout.setOnClickListener {
            findNavController().navigate(R.id.about)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoggedIn.collect { loggedIn ->
                    if (loggedIn) {
                        val name = viewModel.username.value
                        val email = viewModel.email.value
                        tvUserStatus.text = name ?: email ?: getString(R.string.profile_unknown_user)
                        tvLoginLabel.text = getString(R.string.logout)
                        tvLoginHint.text = email.orEmpty()
                        icLogin.setImageResource(R.drawable.ic_nav_profile)
                        itemLogin.setOnClickListener { viewModel.logout() }
                    } else {
                        tvUserStatus.text = getString(R.string.not_logged_in)
                        tvLoginLabel.text = getString(R.string.go_login)
                        tvLoginHint.text = getString(R.string.not_logged_in)
                        icLogin.setImageResource(R.drawable.ic_nav_profile)
                        itemLogin.setOnClickListener {
                            loginLauncher.launch(
								Intent(
									requireContext(),
									LoginActivity::class.java
								)
							)
                        }
                    }
                }
            }
        }
    }
}