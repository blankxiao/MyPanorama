package cn.szu.blankxiao.panorama

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.TextureView
import android.widget.FrameLayout
import cn.szu.blankxiao.panorama.controller.PanoramaController
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.helper.PanoramaImageLoader
import cn.szu.blankxiao.panorama.helper.PanoramaGestureController
import cn.szu.blankxiao.panorama.helper.PanoramaSurfaceTextureCoordinator
import cn.szu.blankxiao.panorama.orientation.GyroOrientationProvider
import cn.szu.blankxiao.panorama.renderer.Renderer
import cn.szu.blankxiao.panorama.renderer.RenderSession
import cn.szu.blankxiao.panorama.renderer.TextureUpdateRenderDriver
import cn.szu.blankxiao.panorama.renderer.rotation.DefaultRotationController

/**
 * @author BlankXiao
 * @description PanoramaView 集成陀螺仪功能的FrameLayout
 * @date 2025-10-26 17:13
 */
class PanoramaView(
	context: Context,
	attrs: AttributeSet?
) : FrameLayout(context, attrs),
	PanoramaController {


	// 渲染器
	private var renderer: Renderer

	// 封装渲染线程的逻辑
	private var renderSession: RenderSession

	// 手势控制
	private val gestureController: PanoramaGestureController
	// 纹理资源
	private val imageLoader: PanoramaImageLoader
	private val surfaceTextureCoordinator: PanoramaSurfaceTextureCoordinator

	init {
		// 环境校验 判断是否支持GLES 2.0
		val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		val configurationInfo = activityManager.deviceConfigurationInfo
		val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
		if (!supportsEs2) {
			throw RuntimeException("your device does not support opengles 2.0")
		}

		// 陀螺仪服务
		val gyroProvider = GyroOrientationProvider(context)
		// 视角控制
		val rotationController = DefaultRotationController(
			orientationProvider = gyroProvider,
			angleOfViewController = gyroProvider,
			lifecycleController = gyroProvider,
			mesherProvider = { renderer.getMesher() }
		)
		// 全景图渲染核心
		renderer = Renderer(context.applicationContext, rotationController)
		renderSession = RenderSession(renderer)
		// 手势业务逻辑
		gestureController = PanoramaGestureController(
			context = context,
			cameraController = renderer,
			touchRotationController = renderer,
			enqueueToGl = { task -> renderSession.post(task) },
			isRenderReady = { renderSession.isReady() },
			onFovChanged = { newFov -> onFovChangedListener?.invoke(newFov) },
			onDoubleTap = { onDoubleTapListener?.invoke() }
		)
		// 纹理资源加载
		imageLoader = PanoramaImageLoader(
			context = context,
			renderDriver = renderer,
			enqueueToGl = { task -> renderSession.post(task) },
			isRenderReady = { renderSession.isReady() }
		)
		surfaceTextureCoordinator = PanoramaSurfaceTextureCoordinator(renderSession, imageLoader)

		// 渲染的目标view
		val renderView = TextureView(context).apply {
			surfaceTextureListener = surfaceTextureCoordinator
			// 不拦截点击事件
			setOnTouchListener { _, _ -> false }
		}
		addView(renderView)
	}

	/**
	 * 设置陀螺仪是否可用，默认可用
	 *
	 * @param enabled
	 */
	override fun setGyroTrackingEnabled(enabled: Boolean) {
		renderer.setGyroTrackingEnabled(enabled)
	}

	/**
	 * 回到图片初始角度
	 */
	override fun reCenter() {
		renderer.reCenter()
	}

	/**
	 * 设置要加载的图片地址，需要
	 *
	 * @param url
	 */
	override fun setBitmapUrl(url: String) {
		imageLoader.setBitmapUrl(url)
	}

	/**
	 * 直接设置 Bitmap（用于本地图片）
	 *
	 * @param bitmap
	 */
	override fun setBitmap(bitmap: Bitmap) {
		imageLoader.setBitmap(bitmap)
	}

	/**
	 * 设置全景图模型类型
	 * 
	 * @param meshType 模型类型
	 *   - MeshType.SPHERE: 球体模型，适用于 equirectangular 全景图（默认）
	 *   - MeshType.CYLINDER: 圆柱体模型，适用于圆柱形全景图
	 */
	override fun setMeshType(meshType: MeshType) {
		renderer.setMeshType(meshType)
	}

	/**
	 * 获取当前模型类型
	 */
	override fun getMeshType(): MeshType = renderer.getMeshType()

	/**
	 * 设置触摸灵敏度
	 * @param sensitivity 灵敏度值，默认 0.5f，值越大越敏感
	 */
	override fun setTouchSensitivity(sensitivity: Float) {
		renderer.setTouchSensitivity(sensitivity)
	}

	/**
	 * 获取触摸灵敏度
	 */
	override fun getTouchSensitivity(): Float = renderer.getTouchSensitivity()

	/**
	 * 设置 FOV（视场角）
	 * @param fovDegrees 视场角（度），范围 [30, 120]
	 */
	override fun setFov(fovDegrees: Float) {
		if (renderSession.isReady()) {
			val clamped = fovDegrees.coerceIn(Camera.MIN_FOV, Camera.MAX_FOV)
			renderSession.post {
				renderer.setFov(clamped)
			}
			onFovChangedListener?.invoke(clamped)
		}
	}

	/**
	 * 获取当前 FOV
	 */
	override fun getFov(): Float = renderer.getFov()

	/**
	 * FOV 变化回调
	 */
	override var onFovChangedListener: ((Float) -> Unit)? = null

	/**
	 * 双击回调
	 */
	override var onDoubleTapListener: (() -> Unit)? = null

	/**
	 * 添加到窗口时调用
	 */
	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		renderer.onAttached()
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		renderer.onDetached()
		// 释放 EGL 上下文资源
		renderSession.release()
	}

	/**
	 * 处理触摸事件
	 * 支持单指拖动旋转、双指捏合缩放 FOV、双击回调
	 */
	override fun onTouchEvent(event: MotionEvent): Boolean {
		return gestureController.onTouchEvent(event) || super.onTouchEvent(event)
	}

	/**
	 * 拦截触摸事件，由本 View 统一处理。
	 * 子 View（TextureView）不处理触摸，所有手势由 PanoramaView.onTouchEvent 处理，
	 * 直接拦截可避免先分发给子 View 再回传的无效分发。
	 */
	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		return true
	}

}
