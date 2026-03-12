package cn.szu.blankxiao.panorama.renderer

/**
 * 用户交互导致的渲染变更能力约定。
 * PanoramaGestureController 仅依赖该接口，不依赖具体 Renderer 实现。
 */
interface UserInteractionRenderDriver {
	fun getFov(): Float
	fun setFov(fovDegrees: Float)

	fun startTouchRotation()
	fun applyTouchRotation(deltaX: Float, deltaY: Float)
	fun endTouchRotation()
}
