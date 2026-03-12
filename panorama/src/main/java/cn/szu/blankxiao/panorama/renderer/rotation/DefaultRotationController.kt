package cn.szu.blankxiao.panorama.renderer.rotation

import cn.szu.blankxiao.panorama.cg.camera.Axis
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.orientation.OrientationProvider
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaMesher

private data class OrientationSnapshot(
	val rotationMatrix: FloatArray,
	val biasMatrix: FloatArray
)

class DefaultRotationController(
	private val orientationProvider: OrientationProvider
) : RotationController, OrientationProvider by orientationProvider {

	private var isTouchActive = false

	override fun setGyroTrackingEnabled(enabled: Boolean) {
		orientationProvider.setGyroTrackingEnabled(enabled)
	}

	override fun startTouchRotation(mesher: PanoramaMesher) {
		isTouchActive = true
		val orientation = currentOrientation()
		mesher.onTouchStart(orientation.rotationMatrix, orientation.biasMatrix)
	}

	override fun applyTouchRotation(
		mesher: PanoramaMesher,
		deltaX: Float,
		deltaY: Float,
		touchSensitivity: Float
	) {
		if (!isTouchActive) return
		mesher.onTouchMove(deltaX, deltaY, touchSensitivity)
	}

	override fun endTouchRotation(mesher: PanoramaMesher) {
		if (!isTouchActive) return
		isTouchActive = false
		val orientation = currentOrientation()
		setBiasMatrix(
			mesher.onTouchEnd(orientation.rotationMatrix, orientation.biasMatrix)
		)
	}

	override fun updateCameraView(camera: Camera, mesher: PanoramaMesher) {
		camera.rebuildViewMatrix()

		if (isTouchActive) {
			mesher.applyTouchRotation(camera)
		} else {
			val orientation = currentOrientation()
			mesher.applyGyroRotation(camera, orientation.rotationMatrix, orientation.biasMatrix)
		}

		// 模型默认朝向与相机前向存在 90° 偏差，这里做统一修正
		camera.rotate(-90.0f, Axis.AXIS_Y)
	}

	private fun currentOrientation(): OrientationSnapshot {
		return OrientationSnapshot(
			rotationMatrix = getRotationMatrix(),
			biasMatrix = getBiasMatrix()
		)
	}
}
