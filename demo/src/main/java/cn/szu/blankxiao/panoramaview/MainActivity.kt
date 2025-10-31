package cn.szu.blankxiao.panoramaview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import cn.szu.blankxiao.panorama.PanoramaView


class MainActivity : AppCompatActivity() {


	val IMAGE_URL: String =
		"https://raw.githubusercontent.com/ShinooGoyal/PanoramaView/refs/heads/main/app/src/main/res/drawable/panorama.jpg"

	lateinit var panoramaTextureView: PanoramaView
	lateinit var btnGyroController: Button

	@SuppressLint("MissingInflatedId")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		panoramaTextureView = findViewById(R.id.panorama)
		btnGyroController = findViewById(R.id.btn_gyro_controller)

		panoramaTextureView.setBitmapUrl(IMAGE_URL)
		panoramaTextureView.setGyroTrackingEnabled(true)

		btnGyroController.tag = false
		btnGyroController.text = "关闭陀螺仪"
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

}