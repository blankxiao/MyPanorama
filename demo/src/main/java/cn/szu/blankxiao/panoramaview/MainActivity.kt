package cn.szu.blankxiao.panoramaview

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import cn.szu.blankxiao.panorama.PanoramaView
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import java.util.Locale


class MainActivity : AppCompatActivity() {


	// 原网络 URL（备用）
	// val IMAGE_URL: String =
	// 	"https://raw.githubusercontent.com/ShinooGoyal/PanoramaView/refs/heads/main/app/src/main/res/drawable/panorama.jpg"

	lateinit var panoramaTextureView: PanoramaView
	lateinit var btnGyroController: Button
	lateinit var btnMeshType: Button
	lateinit var seekBarTouchSensitivity: SeekBar
	lateinit var tvTouchSensitivity: TextView
	lateinit var tvFov: TextView
	lateinit var controlsContainer: LinearLayout

	private var isFullscreen = false
	private val normalPanoramaHeightPx by lazy {
		TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 480f, resources.displayMetrics).toInt()
	}

	@SuppressLint("MissingInflatedId")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		panoramaTextureView = findViewById(R.id.panorama)
		btnGyroController = findViewById(R.id.btn_gyro_controller)
		btnMeshType = findViewById(R.id.btn_mesh_type)
		seekBarTouchSensitivity = findViewById(R.id.seekbar_touch_sensitivity)
		tvTouchSensitivity = findViewById(R.id.tv_touch_sensitivity)
		tvFov = findViewById(R.id.tv_fov)
		controlsContainer = findViewById(R.id.controls_container)

		// 使用本地全景图（MVDiffusion 生成的）
		val bitmap = BitmapFactory.decodeResource(resources, R.drawable.pano)
		panoramaTextureView.setBitmap(bitmap)
		panoramaTextureView.setGyroTrackingEnabled(true)

		btnGyroController.tag = false
		btnGyroController.text = "关闭陀螺仪"

		// 初始化模型类型按钮
		updateMeshTypeButton()

		// 初始化触摸灵敏度调节器
		setupTouchSensitivitySeekBar()

		// FOV 显示
		updateFovText(panoramaTextureView.getFov())
		panoramaTextureView.onFovChangedListener = { updateFovText(it) }

		// 双击全屏
		panoramaTextureView.onDoubleTapListener = { toggleFullscreen() }
	}

	private fun updateFovText(fov: Float) {
		tvFov.text = String.format(Locale.getDefault(), "%.0f°", fov)
	}

	private fun toggleFullscreen() {
		isFullscreen = !isFullscreen
		val params = panoramaTextureView.layoutParams as ConstraintLayout.LayoutParams
		if (isFullscreen) {
			params.height = ConstraintLayout.LayoutParams.MATCH_PARENT
			params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
			params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
			controlsContainer.visibility = View.GONE
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
			window.decorView.systemUiVisibility = (
				View.SYSTEM_UI_FLAG_FULLSCREEN
				or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			)
		} else {
			params.height = normalPanoramaHeightPx
			params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
			params.bottomToTop = R.id.controls_container
			controlsContainer.visibility = View.VISIBLE
			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
			window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
		}
		panoramaTextureView.layoutParams = params
	}

	fun recenter(view: View?) {
		panoramaTextureView.reCenter()
	}

	fun enableGyroTracking(v: View?) {
		val enable = btnGyroController.tag as Boolean
		btnGyroController.text = if (enable) "关闭陀螺仪" else "打开陀螺仪"
		btnGyroController.tag = enable.not()
		panoramaTextureView.setGyroTrackingEnabled(enable)
	}

	/**
	 * 切换模型类型
	 */
	fun toggleMeshType(v: View?) {
		val currentType = panoramaTextureView.getMeshType()
		val newType = when (currentType) {
			MeshType.SPHERE -> MeshType.CYLINDER
			MeshType.CYLINDER -> MeshType.SPHERE
		}
		panoramaTextureView.setMeshType(newType)
		updateMeshTypeButton()
	}

	/**
	 * 更新模型类型按钮文本
	 */
	private fun updateMeshTypeButton() {
		val currentType = panoramaTextureView.getMeshType()
		btnMeshType.text = when (currentType) {
			MeshType.SPHERE -> "球体 (Sphere)"
			MeshType.CYLINDER -> "圆柱体 (Cylinder)"
		}
	}

	/**
	 * 设置触摸灵敏度调节器
	 */
	private fun setupTouchSensitivitySeekBar() {
		// SeekBar 范围：0-200，对应灵敏度 0.0-2.0
		// 默认值 50 对应灵敏度 0.5
		val currentSensitivity = panoramaTextureView.getTouchSensitivity()
		val progress = (currentSensitivity * 100).toInt().coerceIn(0, 200)
		seekBarTouchSensitivity.progress = progress
		updateTouchSensitivityText(currentSensitivity)

		seekBarTouchSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					// 将进度值转换为灵敏度：0-200 -> 0.0-2.0
					val sensitivity = progress / 100f
					panoramaTextureView.setTouchSensitivity(sensitivity)
					updateTouchSensitivityText(sensitivity)
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {
				// 开始拖动时不做处理
			}

			override fun onStopTrackingTouch(seekBar: SeekBar?) {
				// 停止拖动时不做处理
			}
		})
	}

	/**
	 * 更新触摸灵敏度显示文本
	 */
	private fun updateTouchSensitivityText(sensitivity: Float) {
		tvTouchSensitivity.text = String.format(Locale.getDefault(), "%.2f", sensitivity)
	}

}