package cn.szu.blankxiao.panorama.helper

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.controller.CameraController
import cn.szu.blankxiao.panorama.controller.internal.TouchRotationController

/**
 * 处理 PanoramaView 的手势逻辑：
 * - 单指拖动旋转
 * - 双指缩放 FOV
 * - 双击回调
 */
class PanoramaGestureController(
	context: Context,
	// FOV 读写能力（捏合缩放只影响投影矩阵）
	private val cameraController: CameraController,
	// 触摸旋转能力（拖拽只影响视角朝向）
	private val touchRotationController: TouchRotationController,
	// 所有会改 GL 状态的操作统一投递到 GL 线程执行
	private val enqueueToGl: (() -> Unit) -> Unit,
	private val isRenderReady: () -> Boolean,
	private val onFovChanged: (Float) -> Unit,
	private val onDoubleTap: () -> Unit
) {
	// 上一帧触摸位置，用于计算本帧增量（delta）
	private var lastTouchX = 0f
	private var lastTouchY = 0f
	// 同一触摸事件链中，双击已被识别并消费
	private var doubleTapHandled = false

	/** 当前手势阶段 */
	private sealed class GesturePhase {
		// 无手势
		object Idle : GesturePhase()
		// 单指拖拽
		data class Dragging(var rotationStarted: Boolean = false) : GesturePhase()
		// 双指缩放
		object Scaling : GesturePhase()
	}
	private var phase: GesturePhase = GesturePhase.Idle

	/**
	 * 结束手势动作时
	 */
	private fun endRotationIfStarted() {
		val drag = phase as? GesturePhase.Dragging ?: return
		if (!drag.rotationStarted) return
		drag.rotationStarted = false
		// 应用最新的矩阵
		enqueueToGl { touchRotationController.endTouchRotation() }
	}

	private val scaleGestureDetector = ScaleGestureDetector(context, FovScaleListener())
	private val doubleTapDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
		override fun onDoubleTap(e: MotionEvent): Boolean {
			doubleTapHandled = true
			onDoubleTap()
			return true
		}
	})

	fun onTouchEvent(event: MotionEvent): Boolean {
		// 渲染会话未就绪时，手势不应改变任何渲染状态
		if (!isRenderReady()) return false

		// 先做双击识别：若命中则直接消费，避免继续进入拖拽/缩放分支
		doubleTapHandled = false
		doubleTapDetector.onTouchEvent(event)
		if (doubleTapHandled) return true

		// 再做缩放识别：缩放期间暂停旋转，避免两种输入竞争同一帧状态
		scaleGestureDetector.onTouchEvent(event)
		if (phase == GesturePhase.Scaling) {
			if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
				phase = GesturePhase.Idle
			}
			return true
		}

		when (event.actionMasked) {
			// 第一根手指按下，进入单指拖拽阶段
			MotionEvent.ACTION_DOWN -> {
				lastTouchX = event.x
				lastTouchY = event.y
				phase = GesturePhase.Dragging()
				return true
			}

			// 单指移动：在拖拽阶段且仍为单指时，应用旋转增量
			MotionEvent.ACTION_MOVE -> {
				if (phase is GesturePhase.Dragging && event.pointerCount == 1) {
					val drag = phase as GesturePhase.Dragging
					if (!drag.rotationStarted) {
						drag.rotationStarted = true
						enqueueToGl { touchRotationController.startTouchRotation() }
					}
					// 计算本帧位移增量并投递到 GL 线程应用
					val deltaX = event.x - lastTouchX
					val deltaY = event.y - lastTouchY
					lastTouchX = event.x
					lastTouchY = event.y
					enqueueToGl { touchRotationController.applyTouchRotation(deltaX, deltaY) }
					return true
				}
			}

			// 手指抬起或手势被取消，结束拖拽并回到空闲
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				if (phase is GesturePhase.Dragging) {
					endRotationIfStarted()
					phase = GesturePhase.Idle
					return true
				}
			}
		}
		return false
	}

	private inner class FovScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
		override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
			endRotationIfStarted()
			phase = GesturePhase.Scaling
			return true
		}

		override fun onScale(detector: ScaleGestureDetector): Boolean {
			// 缩放映射规则：手指张开(scaleFactor>1) -> FOV 变小 -> 画面“放大”
			val currentFov = cameraController.getFov()
			val newFov = (currentFov / detector.scaleFactor)
				.coerceIn(Camera.MIN_FOV, Camera.MAX_FOV)
			enqueueToGl { cameraController.setFov(newFov) }
			onFovChanged(newFov)
			return true
		}
	}
}
