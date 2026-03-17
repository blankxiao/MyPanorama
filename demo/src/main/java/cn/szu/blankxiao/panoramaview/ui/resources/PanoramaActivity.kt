package cn.szu.blankxiao.panoramaview.ui.resources

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        insetsController = WindowInsetsControllerCompat(window, window.decorView)

        panoramaView = findViewById(R.id.panorama)
        infoContainer = findViewById(R.id.info_container)
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
            findViewById<View>(R.id.panorama_root).setBackgroundColor(
                android.graphics.Color.BLACK
            )
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            infoContainer.visibility = View.VISIBLE
            findViewById<View>(R.id.panorama_root).setBackgroundColor(
                android.graphics.Color.BLACK
            )
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
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
