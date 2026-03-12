package cn.szu.blankxiao.panorama.helper

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.renderer.UserInteractionRenderDriver

/**
 * 处理 PanoramaView 的手势逻辑：
 * - 单指拖动旋转
 * - 双指缩放 FOV
 * - 双击回调
 */
class PanoramaGestureController(
	context: Context,
	private val renderDriver: UserInteractionRenderDriver,
	private val enqueueToGl: (() -> Unit) -> Unit,
	private val isRenderReady: () -> Boolean,
	private val onFovChanged: (Float) -> Unit,
	private val onDoubleTap: () -> Unit
) {
	private var lastTouchX = 0f
	private var lastTouchY = 0f
	private var isTouching = false
	private var isScaling = false
	private var rotationStarted = false
	private var doubleTapHandled = false

	private val scaleGestureDetector = ScaleGestureDetector(context, FovScaleListener())
	private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
		override fun onDoubleTap(e: MotionEvent): Boolean {
			doubleTapHandled = true
			onDoubleTap()
			return true
		}
	})

	fun onTouchEvent(event: MotionEvent): Boolean {
		if (!isRenderReady()) return false

		doubleTapHandled = false
		gestureDetector.onTouchEvent(event)
		if (doubleTapHandled) return true

		scaleGestureDetector.onTouchEvent(event)
		if (isScaling) {
			if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
				isScaling = false
				if (rotationStarted) {
					rotationStarted = false
					enqueueToGl { renderDriver.endTouchRotation() }
				}
			}
			return true
		}

		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				lastTouchX = event.x
				lastTouchY = event.y
				isTouching = true
				rotationStarted = false
				return true
			}

			MotionEvent.ACTION_MOVE -> {
				if (isTouching && event.pointerCount == 1) {
					if (!rotationStarted) {
						rotationStarted = true
						enqueueToGl { renderDriver.startTouchRotation() }
					}
					val deltaX = event.x - lastTouchX
					val deltaY = event.y - lastTouchY
					lastTouchX = event.x
					lastTouchY = event.y
					enqueueToGl { renderDriver.applyTouchRotation(deltaX, deltaY) }
					return true
				}
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				if (isTouching) {
					isTouching = false
					if (rotationStarted) {
						rotationStarted = false
						enqueueToGl { renderDriver.endTouchRotation() }
					}
					return true
				}
			}

			MotionEvent.ACTION_POINTER_DOWN -> {
				if (rotationStarted) {
					rotationStarted = false
					enqueueToGl { renderDriver.endTouchRotation() }
				}
			}
		}
		return false
	}

	private inner class FovScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
		override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
			isScaling = true
			return true
		}

		override fun onScale(detector: ScaleGestureDetector): Boolean {
			val currentFov = renderDriver.getFov()
			val newFov = (currentFov / detector.scaleFactor)
				.coerceIn(Camera.MIN_FOV, Camera.MAX_FOV)
			enqueueToGl { renderDriver.setFov(newFov) }
			onFovChanged(newFov)
			return true
		}
	}
}
