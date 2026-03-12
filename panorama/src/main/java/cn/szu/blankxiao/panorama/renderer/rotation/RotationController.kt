package cn.szu.blankxiao.panorama.renderer.rotation

import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaMesher

/**
 * 旋转控制器
 * 负责触摸状态管理、朝向数据接入以及相机视图矩阵更新
 */
interface RotationController {
	fun onAttached()
	fun onDetached()
	fun reCenter()
	fun setGyroTrackingEnabled(enabled: Boolean)

	fun startTouchRotation(mesher: PanoramaMesher)
	fun applyTouchRotation(
		mesher: PanoramaMesher,
		deltaX: Float,
		deltaY: Float,
		touchSensitivity: Float
	)
	fun endTouchRotation(mesher: PanoramaMesher)

	fun updateCameraView(camera: Camera, mesher: PanoramaMesher)
}
