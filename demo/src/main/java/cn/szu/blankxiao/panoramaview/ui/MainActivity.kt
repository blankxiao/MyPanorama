package cn.szu.blankxiao.panoramaview.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import cn.szu.blankxiao.panoramaview.R
import cn.szu.blankxiao.panoramaview.data.ThemePreference
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var jankStats: JankStats? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(ThemePreference.getNightMode(this))
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val root = findViewById<android.view.View>(R.id.root_main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        // 卡顿检测：JankStats 每帧回调，jank 时打日志（可扩展为写文件/上报）
        val metricsHolder = PerformanceMetricsState.getHolderForHierarchy(root)
        jankStats = JankStats.createAndTrack(window, jankFrameListener)
        metricsHolder.state?.putState("Activity", javaClass.simpleName)

        // 绑定底部导航栏和Fragment
        // menu的id与nav_graph的id对应
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        jankStats?.isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        jankStats?.isTrackingEnabled = false
    }

    /** 每帧回调；FrameData 会被复用，需要当场拷贝所需字段。回调需尽快返回。 */
    private val jankFrameListener = JankStats.OnFrameListener { frameData ->
        if (!frameData.isJank) return@OnFrameListener
        val durationMs = frameData.frameDurationUiNanos / 1_000_000
        val states = frameData.states.joinToString { "${it.key}=${it.value}" }
        Log.w(TAG_JANK, "Jank: ${durationMs}ms, states=[$states]")
    }

    companion object {
        private const val TAG_JANK = "JankStats"
    }
}
