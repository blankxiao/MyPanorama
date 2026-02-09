package cn.szu.blankxiao.panoramaview

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import cn.szu.blankxiao.panorama.PanoramaView
import cn.szu.blankxiao.panorama.cg.mesh.MeshType


class MainActivity : AppCompatActivity() {


	// 原网络 URL（备用）
	// val IMAGE_URL: String =
	// 	"https://raw.githubusercontent.com/ShinooGoyal/PanoramaView/refs/heads/main/app/src/main/res/drawable/panorama.jpg"

	lateinit var panoramaTextureView: PanoramaView
	lateinit var btnGyroController: Button
	lateinit var btnMeshType: Button

	@SuppressLint("MissingInflatedId")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		panoramaTextureView = findViewById(R.id.panorama)
		btnGyroController = findViewById(R.id.btn_gyro_controller)
		btnMeshType = findViewById(R.id.btn_mesh_type)

		// 使用本地全景图（MVDiffusion 生成的）
		val bitmap = BitmapFactory.decodeResource(resources, R.drawable.pano)
		panoramaTextureView.setBitmap(bitmap)
		panoramaTextureView.setGyroTrackingEnabled(true)

		btnGyroController.tag = false
		btnGyroController.text = "关闭陀螺仪"

		// 初始化模型类型按钮
		updateMeshTypeButton()
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

}