package cn.szu.blankxiao.panorama.renderer.rotation

import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.controller.AngleOfViewController
import cn.szu.blankxiao.panorama.controller.internal.LifecycleController
import cn.szu.blankxiao.panorama.controller.internal.RotationController
import cn.szu.blankxiao.panorama.orientation.OrientationProvider
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaMesher

class DefaultRotationController(
	private val orientationProvider: OrientationProvider,
	private val angleOfViewController: AngleOfViewController,
	private val lifecycleController: LifecycleController,
	private val mesherProvider: () -> PanoramaMesher
) : RotationController,
	OrientationProvider by orientationProvider,
	AngleOfViewController by angleOfViewController,
	LifecycleController by lifecycleController
{

	private var isTouchActive = false
	private var touchSensitivity: Float = 0.5f

	override fun startTouchRotation() {
		isTouchActive = true
		val mesher = mesherProvider()
		mesher.onTouchStart(getRotationMatrix(), getBiasMatrix())
	}

	override fun applyTouchRotation(
		deltaX: Float,
		deltaY: Float
	) {
		if (!isTouchActive) return
		val mesher = mesherProvider()
		mesher.onTouchMove(deltaX, deltaY, touchSensitivity)
	}

	override fun endTouchRotation() {
		if (!isTouchActive) return
		isTouchActive = false
		val mesher = mesherProvider()
		setBiasMatrix(
			mesher.onTouchEnd(getRotationMatrix(), getBiasMatrix())
		)
	}

	override fun updateCameraView(camera: Camera) {
		camera.rebuildViewMatrix()
		val mesher = mesherProvider()

		if (isTouchActive) {
			mesher.applyTouchRotation(camera)
		} else {
			mesher.applyGyroRotation(camera, getRotationMatrix(), getBiasMatrix())
		}
	}

	override fun setTouchSensitivity(sensitivity: Float) {
		touchSensitivity = sensitivity
	}

	override fun getTouchSensitivity(): Float = touchSensitivity

}
