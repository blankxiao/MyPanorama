package cn.szu.blankxiao.panoramaview.ui.resouces

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import cn.szu.blankxiao.panorama.PanoramaView
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panoramaview.FullscreenHost
import cn.szu.blankxiao.panoramaview.R
import java.util.Locale

class PanoramaFragment : Fragment() {

    private lateinit var panoramaTextureView: PanoramaView
    private lateinit var btnGyroController: Button
    private lateinit var btnMeshType: Button
    private lateinit var seekBarTouchSensitivity: SeekBar
    private lateinit var tvTouchSensitivity: TextView
    private lateinit var tvFov: TextView
    private lateinit var controlsContainer: LinearLayout

    private var isFullscreen = false
    private val normalPanoramaHeightPx by lazy {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 480f, resources.displayMetrics).toInt()
    }
    private val normalPaddingPx by lazy {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_panorama, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        panoramaTextureView = view.findViewById(R.id.panorama)
        btnGyroController = view.findViewById(R.id.btn_gyro_controller)
        btnMeshType = view.findViewById(R.id.btn_mesh_type)
        seekBarTouchSensitivity = view.findViewById(R.id.seekbar_touch_sensitivity)
        tvTouchSensitivity = view.findViewById(R.id.tv_touch_sensitivity)
        tvFov = view.findViewById(R.id.tv_fov)
        controlsContainer = view.findViewById(R.id.controls_container)

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.pano)
        panoramaTextureView.setBitmap(bitmap)
        panoramaTextureView.setGyroTrackingEnabled(true)

        btnGyroController.tag = false
        btnGyroController.text = "关闭陀螺仪"

        updateMeshTypeButton()
        setupTouchSensitivitySeekBar()

        updateFovText(panoramaTextureView.getFov())
        panoramaTextureView.onFovChangedListener = { updateFovText(it) }
        panoramaTextureView.onDoubleTapListener = { toggleFullscreen() }

        view.findViewById<Button>(R.id.btn_recenter).setOnClickListener { panoramaTextureView.reCenter() }
        btnGyroController.setOnClickListener { enableGyroTracking() }
        btnMeshType.setOnClickListener { toggleMeshType() }
    }

    private fun updateFovText(fov: Float) {
        tvFov.text = String.Companion.format(Locale.getDefault(), "%.0f°", fov)
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        (requireActivity() as? FullscreenHost)?.setFullscreen(isFullscreen)
        val params = panoramaTextureView.layoutParams as ConstraintLayout.LayoutParams
        val activity = requireActivity()
        if (isFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            requireView().setPadding(0, 0, 0, 0)
            params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            controlsContainer.visibility = View.GONE
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        } else {
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            requireView().setPadding(normalPaddingPx, normalPaddingPx, normalPaddingPx, normalPaddingPx)
            params.height = normalPanoramaHeightPx
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = R.id.controls_container
            controlsContainer.visibility = View.VISIBLE
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        panoramaTextureView.layoutParams = params
    }

    private fun enableGyroTracking() {
        val enable = btnGyroController.tag as Boolean
        btnGyroController.text = if (enable) "关闭陀螺仪" else "打开陀螺仪"
        btnGyroController.tag = enable.not()
        panoramaTextureView.setGyroTrackingEnabled(enable)
    }

    private fun toggleMeshType() {
        val currentType = panoramaTextureView.getMeshType()
        val newType = when (currentType) {
            MeshType.SPHERE -> MeshType.CYLINDER
            MeshType.CYLINDER -> MeshType.SPHERE
        }
        panoramaTextureView.setMeshType(newType)
        updateMeshTypeButton()
    }

    private fun updateMeshTypeButton() {
        val currentType = panoramaTextureView.getMeshType()
        btnMeshType.text = when (currentType) {
            MeshType.SPHERE -> "球体 (Sphere)"
            MeshType.CYLINDER -> "圆柱体 (Cylinder)"
        }
    }

    private fun setupTouchSensitivitySeekBar() {
        val currentSensitivity = panoramaTextureView.getTouchSensitivity()
        val progress = (currentSensitivity * 100).toInt().coerceIn(0, 200)
        seekBarTouchSensitivity.progress = progress
        updateTouchSensitivityText(currentSensitivity)

        seekBarTouchSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val sensitivity = progress / 100f
                    panoramaTextureView.setTouchSensitivity(sensitivity)
                    updateTouchSensitivityText(sensitivity)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateTouchSensitivityText(sensitivity: Float) {
        tvTouchSensitivity.text = String.Companion.format(Locale.getDefault(), "%.2f", sensitivity)
    }
}