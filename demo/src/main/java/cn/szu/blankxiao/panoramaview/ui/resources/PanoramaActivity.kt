package cn.szu.blankxiao.panoramaview.ui.resources

import android.graphics.Color
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import cn.szu.blankxiao.panorama.PanoramaView
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panoramaview.R
import com.google.android.material.button.MaterialButton
import java.util.Locale

/**
 * 全景资源详情页。
 * 普通模式：上方全景预览 + 下方资源信息与控制。
 * 双击全景图进入完全全屏（纯全景，无任何 UI 元素），再次双击退出。
 */
class PanoramaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESOURCE_ID = "resource_id"
        const val EXTRA_RESOURCE_NAME = "resource_name"
        const val EXTRA_RESOURCE_TIME = "resource_time"
        const val EXTRA_RESOURCE_MODE = "resource_mode"
        const val EXTRA_RESOURCE_URL = "resource_url"
    }

    private lateinit var panoramaView: PanoramaView
    private lateinit var panoramaRoot: View
    private lateinit var infoContainer: View
    private lateinit var tvFov: TextView
    private lateinit var tvSensitivity: TextView
    private lateinit var btnGyro: MaterialButton
    private lateinit var btnMeshType: MaterialButton
    private lateinit var insetsController: WindowInsetsControllerCompat

    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_panorama)

        // 非全屏：状态栏可见且有背景色
        applyStatusBarForNonFullscreen()

        insetsController = WindowInsetsControllerCompat(window, window.decorView)

        panoramaRoot = findViewById(R.id.panorama_root)
        panoramaView = findViewById(R.id.panorama)
        infoContainer = findViewById(R.id.info_container)

        // 非全屏时根据系统栏 insets 添加 padding，避免内容被状态栏遮挡
        ViewCompat.setOnApplyWindowInsetsListener(panoramaRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                if (isFullscreen) 0 else bars.left,
                if (isFullscreen) 0 else bars.top,
                if (isFullscreen) 0 else bars.right,
                if (isFullscreen) 0 else bars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(panoramaRoot)
        tvFov = findViewById(R.id.tv_fov)
        tvSensitivity = findViewById(R.id.tv_touch_sensitivity)
        btnGyro = findViewById(R.id.btn_gyro_controller)
        btnMeshType = findViewById(R.id.btn_mesh_type)

        loadPanorama()
        bindResourceInfo()
        setupControls()
    }

    private fun loadPanorama() {
        val url = intent.getStringExtra(EXTRA_RESOURCE_URL)
        if (!url.isNullOrBlank()) {
            panoramaView.setBitmapUrl(url)
        } else {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.pano_cylinder)
            panoramaView.setBitmap(bitmap)
        }
        panoramaView.setGyroTrackingEnabled(true)
    }

    private fun bindResourceInfo() {
        val name = intent.getStringExtra(EXTRA_RESOURCE_NAME) ?: getString(R.string.demo_panorama_name)
        val time = intent.getStringExtra(EXTRA_RESOURCE_TIME) ?: ""
        val mode = intent.getStringExtra(EXTRA_RESOURCE_MODE) ?: ""

        findViewById<TextView>(R.id.tv_resource_name).text = name
        findViewById<TextView>(R.id.tv_resource_time).apply {
            text = time
            visibility = if (time.isEmpty()) View.GONE else View.VISIBLE
        }
        findViewById<TextView>(R.id.tv_resource_mode).apply {
            text = mode
            visibility = if (mode.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun setupControls() {
        btnGyro.tag = false
        btnGyro.text = getString(R.string.gyro_off)
        updateMeshTypeButton()

        val sensitivity = panoramaView.getTouchSensitivity()
        val seekBar = findViewById<SeekBar>(R.id.seekbar_touch_sensitivity)
        seekBar.progress = (sensitivity * 100).toInt().coerceIn(0, 200)
        updateSensitivityText(sensitivity)

        tvFov.text = formatFov(panoramaView.getFov())
        panoramaView.onFovChangedListener = { tvFov.text = formatFov(it) }
        panoramaView.onDoubleTapListener = { toggleFullscreen() }

        findViewById<MaterialButton>(R.id.btn_recenter).setOnClickListener {
            panoramaView.reCenter()
        }

        btnGyro.setOnClickListener {
            val enable = btnGyro.tag as Boolean
            btnGyro.text = if (enable) getString(R.string.gyro_off) else getString(R.string.gyro_on)
            btnGyro.tag = !enable
            panoramaView.setGyroTrackingEnabled(enable)
        }

        btnMeshType.setOnClickListener {
            val newType = when (panoramaView.getMeshType()) {
                MeshType.SPHERE -> MeshType.CYLINDER
                MeshType.CYLINDER -> MeshType.SPHERE
            }
            panoramaView.setMeshType(newType)
            updateMeshTypeButton()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val s = progress / 100f
                    panoramaView.setTouchSensitivity(s)
                    updateSensitivityText(s)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            infoContainer.visibility = View.GONE
            panoramaRoot.setBackgroundColor(Color.BLACK)
            // 全屏：状态栏/导航栏透明，内容延伸至边缘，无背景条
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            panoramaRoot.setPadding(0, 0, 0, 0)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            infoContainer.visibility = View.VISIBLE
            panoramaRoot.setBackgroundColor(Color.BLACK)
            applyStatusBarForNonFullscreen()
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            ViewCompat.requestApplyInsets(panoramaRoot)
        }
    }

    /** 非全屏：状态栏有背景色，与主题一致 */
    private fun applyStatusBarForNonFullscreen() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_dark)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun updateMeshTypeButton() {
        btnMeshType.text = when (panoramaView.getMeshType()) {
            MeshType.SPHERE -> getString(R.string.mesh_sphere)
            MeshType.CYLINDER -> getString(R.string.mesh_cylinder)
        }
    }

    private fun updateSensitivityText(s: Float) {
        tvSensitivity.text = String.format(Locale.getDefault(), "%.2f", s)
    }

    private fun formatFov(fov: Float): String =
        String.format(Locale.getDefault(), "%.0f°", fov)
}
