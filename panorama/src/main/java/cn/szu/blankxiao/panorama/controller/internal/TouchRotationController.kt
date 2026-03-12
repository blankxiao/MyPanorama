package cn.szu.blankxiao.panorama.controller.internal

/**
 * 触摸旋转控制能力：
 * - 触摸开始/移动/结束
 * - 触摸灵敏度配置
 */
interface TouchRotationController {
	fun startTouchRotation()
	fun applyTouchRotation(deltaX: Float, deltaY: Float)
	fun endTouchRotation()

	// TODO
	fun setTouchSensitivity(sensitivity: Float)
	fun getTouchSensitivity(): Float
}