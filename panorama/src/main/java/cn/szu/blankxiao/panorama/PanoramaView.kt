package cn.szu.blankxiao.panorama

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.TextureView
import android.widget.FrameLayout
import cn.szu.blankxiao.panorama.api.PanoramaController
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.helper.PanoramaImageLoader
import cn.szu.blankxiao.panorama.helper.PanoramaGestureController
import cn.szu.blankxiao.panorama.renderer.Renderer
import cn.szu.blankxiao.panorama.renderer.RenderSession
import cn.szu.blankxiao.panorama.renderer.TextureUpdateRenderDriver
import cn.szu.blankxiao.panorama.renderer.UserInteractionRenderDriver

/**
 * @author BlankXiao
 * @description PanoramaView 集成陀螺仪功能的FrameLayout
 * @date 2025-10-26 17:13
 */
class PanoramaView(
	context: Context,
	attrs: AttributeSet?
) : FrameLayout(context, attrs),
	TextureView.SurfaceTextureListener,
	PanoramaController {

	// TextureView 作为openGL渲染内容的显示载体 是当前Frame布局的唯一子view
	// 内部使用了SurfaceTexture 是opengl的直接渲染目标
	private lateinit var renderView: TextureView

	// 渲染器
	private lateinit var renderer: Renderer
	private lateinit var interactionDriver: UserInteractionRenderDriver
	private lateinit var textureDriver: TextureUpdateRenderDriver

	// 渲染线程 继承Thread
	private lateinit var renderSession: RenderSession

	private val gestureController: PanoramaGestureController
	private val imageLoader: PanoramaImageLoader

	init {
		val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		val configurationInfo = activityManager.deviceConfigurationInfo
		val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
		if (!supportsEs2) {
			throw RuntimeException("your device does not support opengles 2.0")
		}
		renderView = TextureView(context).apply {
			surfaceTextureListener = this@PanoramaView
			setOnTouchListener { _, _ -> false }
		}
		addView(renderView)
		renderer = Renderer(context)
		interactionDriver = renderer
		textureDriver = renderer
		renderSession = RenderSession(renderer)

		gestureController = PanoramaGestureController(
			context = context,
			renderDriver = interactionDriver,
			enqueueToGl = { task -> renderSession.post(task) },
			isRenderReady = { renderSession.isReady() },
			onFovChanged = { newFov -> onFovChangedListener?.invoke(newFov) },
			onDoubleTap = { onDoubleTapListener?.invoke() }
		)

		imageLoader = PanoramaImageLoader(
			context = context,
			renderDriver = textureDriver,
			enqueueToGl = { task -> renderSession.post(task) },
			isRenderReady = { renderSession.isReady() }
		)
	}

	/**
	 * 设置陀螺仪是否可用，默认可用
	 *
	 * @param enabled
	 */
	override fun setGyroTrackingEnabled(enabled: Boolean) {
		renderer.enableGyroTracking(enabled)
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
	 * FOV 变化回调（捏合缩放或 setFov 时触发）
	 */
	override var onFovChangedListener: ((Float) -> Unit)? = null

	/**
	 * 双击回调（用于全屏切换等）
	 */
	override var onDoubleTapListener: (() -> Unit)? = null

	/**
	 * 添加到窗口时调用
	 */
	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		// 添加渲染器
		renderer.onAttached()
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		renderer.onDetached()
		// 释放 EGL 上下文资源
		renderSession.release()
	}


	// -------------------------------
	// 对listener接口的实现
	// -------------------------------
	// detached 以后再 attached 还会回调此方法
	override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
		// 确保从后台回来的时候只调用一次，只初始化一条 GLProducerThread
		if (!renderSession.isReady()) {
			renderSession.init(surface, width, height) {
				imageLoader.loadForFirstSurface()
			}
		} else {
			renderSession.resume(surface) {
				imageLoader.reloadForExistingSurface()
			}
		}
	}

	override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
		renderSession.resize(width, height)
	}

	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
		renderSession.pause()
		return true
	}

	override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
	}

	/**
	 * 处理触摸事件
	 * 支持单指拖动旋转、双指捏合缩放 FOV、双击回调
	 */
	override fun onTouchEvent(event: MotionEvent): Boolean {
		return gestureController.onTouchEvent(event) || super.onTouchEvent(event)
	}

	/**
	 * 拦截触摸事件，确保触摸事件被正确处理
	 */
	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		// 允许触摸事件传递给 onTouchEvent
		return false
	}

}
