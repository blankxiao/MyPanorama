package cn.szu.blankxiao.panorama

import android.R
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.widget.FrameLayout
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.cg.gl.GLProducerThread
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.renderer.Renderer
import cn.szu.blankxiao.panorama.utils.ImageUtil
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author BlankXiao
 * @description PanoramaView 集成陀螺仪功能的FrameLayout
 * @date 2025-10-26 17:13
 */
class PanoramaView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs),
	TextureView.SurfaceTextureListener {

	// TextureView 作为openGL渲染内容的显示载体 的当前Frame布局的唯一子view
	// 内部使用了SurfaceTexture 是opengl的直接渲染目标
	var renderView: TextureView

	// 渲染器
	var renderer: Renderer

	// 渲染线程 继承Thread
	lateinit var producerThread: GLProducerThread

	// 是否已经初始化GL线程
	private var isGLThreadAvailable = false
	private var currentBitmapUrl: String? = null
	private var localBitmap: Bitmap? = null  // 直接设置的本地 Bitmap
	var placeHolder: Bitmap

	// 触摸相关
	private var lastTouchX: Float = 0f
	private var lastTouchY: Float = 0f
	private var isTouching: Boolean = false
	private var rotationStarted: Boolean = false  // 是否已开始旋转（用于区分单击与双击）

	// 捏合缩放手势检测器
	private val scaleGestureDetector: ScaleGestureDetector
	private var isScaling: Boolean = false

	// 双击手势检测器
	private val gestureDetector: GestureDetector
	private var doubleTapHandled: Boolean = false

	init {
		val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		val configurationInfo = activityManager.deviceConfigurationInfo
		val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
		if (supportsEs2) {
			placeHolder = BitmapFactory.decodeResource(resources, R.color.black)

			renderView = TextureView(context)
			renderer = Renderer(context)
			renderView.surfaceTextureListener = this
			// 设置 renderView 不拦截触摸事件，让父视图处理
			renderView.setOnTouchListener { _, _ -> false }
			addView(renderView)

			// 初始化捏合缩放手势
			scaleGestureDetector = ScaleGestureDetector(context, FovScaleListener())

			// 初始化双击手势
			gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
				override fun onDoubleTap(e: MotionEvent): Boolean {
					doubleTapHandled = true
					onDoubleTapListener?.invoke()
					return true
				}
			})
		} else {
			throw RuntimeException("your device does not support opengles 2.0")
		}
	}

	/**
	 * 设置陀螺仪是否可用，默认可用
	 *
	 * @param enabled
	 */
	fun setGyroTrackingEnabled(enabled: Boolean) {
		renderer.enableGyroTracking(enabled)
	}

	/**
	 * 回到图片初始角度
	 */
	fun reCenter() {
		renderer.reCenter()
	}

	/**
	 * 设置要加载的图片地址，需要
	 *
	 * @param url
	 */
	fun setBitmapUrl(url: String) {
		currentBitmapUrl = url
	}

	/**
	 * 直接设置 Bitmap（用于本地图片）
	 *
	 * @param bitmap
	 */
	fun setBitmap(bitmap: Bitmap) {
		localBitmap = bitmap
		currentBitmapUrl = null  // 清除 URL，使用本地 Bitmap
		if (isGLThreadAvailable) {
			loadBitmapToGLTexture(bitmap)
		}
	}

	/**
	 * 设置全景图模型类型
	 * 
	 * @param meshType 模型类型
	 *   - MeshType.SPHERE: 球体模型，适用于 equirectangular 全景图（默认）
	 *   - MeshType.CYLINDER: 圆柱体模型，适用于圆柱形全景图
	 */
	fun setMeshType(meshType: MeshType) {
		renderer.setMeshType(meshType)
	}

	/**
	 * 获取当前模型类型
	 */
	fun getMeshType(): MeshType = renderer.getMeshType()

	/**
	 * 设置触摸灵敏度
	 * @param sensitivity 灵敏度值，默认 0.5f，值越大越敏感
	 */
	fun setTouchSensitivity(sensitivity: Float) {
		renderer.touchSensitivity = sensitivity
	}

	/**
	 * 获取触摸灵敏度
	 */
	fun getTouchSensitivity(): Float = renderer.touchSensitivity

	/**
	 * 设置 FOV（视场角）
	 * @param fovDegrees 视场角（度），范围 [30, 120]
	 */
	fun setFov(fovDegrees: Float) {
		if (isGLThreadAvailable) {
			val clamped = fovDegrees.coerceIn(Camera.MIN_FOV, Camera.MAX_FOV)
			producerThread.enqueueEvent {
				renderer.setFov(clamped)
			}
			onFovChangedListener?.invoke(clamped)
		}
	}

	/**
	 * 获取当前 FOV
	 */
	fun getFov(): Float = renderer.getFov()

	/**
	 * FOV 变化回调（捏合缩放或 setFov 时触发）
	 */
	var onFovChangedListener: ((Float) -> Unit)? = null

	/**
	 * 双击回调（用于全屏切换等）
	 */
	var onDoubleTapListener: (() -> Unit)? = null

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
		if (isGLThreadAvailable) {
			producerThread.enqueueEvent({
				producerThread.releaseEglContext()
			})
		}
	}


	// -------------------------------
	// 对listener接口的实现
	// -------------------------------
	// detached 以后再 attached 还会回调此方法
	override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
		// 确保从后台回来的时候只调用一次，只初始化一条 GLProducerThread
		if (!isGLThreadAvailable) {
			isGLThreadAvailable = true
			producerThread = GLProducerThread(surface, renderer, AtomicBoolean(true))
			producerThread.start()

			producerThread.enqueueEvent { renderer.onSurfaceChanged(width, height) }

			loadBitmapFromCurrentUrl()
		} else {
			// 已经加载了EGL上下文 只需刷新
			producerThread.refreshSurfaceTexture(surface)
			changeBitmapFromCurrentUrl()
			producerThread.onResume()
		}
	}

	override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
		producerThread.enqueueEvent { renderer.onSurfaceChanged(width, height) }
	}

	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
		if (isGLThreadAvailable) {
			producerThread.onPause()
		}
		return true
	}

	override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
	}

	private fun loadBitmapFromCurrentUrl() {
		// 优先使用直接设置的本地 Bitmap
		val local = localBitmap
		if (local != null) {
			loadBitmapToGLTexture(local)
			return
		}

		// 使用 URL 加载
		val url = currentBitmapUrl
		if (url == null) {
			loadBitmapToGLTexture(placeHolder)
			return
		}

		// 尝试从缓存读取图片资源
		val bitmap: Bitmap? = ImageUtil.loadBitmapFromCache(context, url)
		if (bitmap != null) {
			loadBitmapToGLTexture(bitmap)
		} else {
			// 不存在
			// 先显示默认图 再网络加载
			loadBitmapToGLTexture(placeHolder)
			ImageUtil.loadBitmapFromNetwork(context, url) { tempBitmap ->
				if (tempBitmap != null) {
					loadBitmapToGLTexture(tempBitmap)
				}
			}
		}
	}

	private fun loadBitmapToGLTexture(bitmap: Bitmap) {
		producerThread.enqueueEvent {
			renderer.loadBitmap(bitmap)
		}
	}

	private fun changeBitmapFromCurrentUrl() {
		// 优先使用直接设置的本地 Bitmap
		val local = localBitmap
		if (local != null) {
			changeBitmapToGLTexture(local)
			return
		}

		// 使用 URL 加载
		val url = currentBitmapUrl
		if (url == null) {
			loadBitmapToGLTexture(placeHolder)
			return
		}

		val bitmap: Bitmap? = ImageUtil.loadBitmapFromCache(context, url)
		if (bitmap != null) {
			changeBitmapToGLTexture(bitmap)
		} else {
			loadBitmapToGLTexture(placeHolder)
			ImageUtil.loadBitmapFromNetwork(context, url) { tempBitmap ->
				if (tempBitmap != null) {
					changeBitmapToGLTexture(tempBitmap)
				}
			}
		}
	}

	private fun changeBitmapToGLTexture(bitmap: Bitmap) {
		producerThread.enqueueEvent {
			renderer.changeTextureBitmap(bitmap)
		}
	}

	/**
	 * 处理触摸事件
	 * 支持单指拖动旋转、双指捏合缩放 FOV、双击回调
	 */
	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (!isGLThreadAvailable) {
			return super.onTouchEvent(event)
		}

		// 先交给双击手势检测器
		doubleTapHandled = false
		gestureDetector.onTouchEvent(event)
		if (doubleTapHandled) {
			return true
		}

		// 再交给缩放手势检测器
		scaleGestureDetector.onTouchEvent(event)

		// 如果正在缩放，不处理单指拖动
		if (isScaling) {
			if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
				isScaling = false
				if (rotationStarted) {
					rotationStarted = false
					producerThread.enqueueEvent { renderer.endTouchRotation() }
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
						producerThread.enqueueEvent { renderer.startTouchRotation() }
					}
					val deltaX = event.x - lastTouchX
					val deltaY = event.y - lastTouchY
					lastTouchX = event.x
					lastTouchY = event.y
					producerThread.enqueueEvent {
						renderer.applyTouchRotation(deltaX, deltaY)
					}
					return true
				}
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				if (isTouching) {
					isTouching = false
					if (rotationStarted) {
						rotationStarted = false
						producerThread.enqueueEvent { renderer.endTouchRotation() }
					}
					return true
				}
			}
			MotionEvent.ACTION_POINTER_DOWN -> {
				if (rotationStarted) {
					rotationStarted = false
					producerThread.enqueueEvent { renderer.endTouchRotation() }
				}
			}
		}
		return super.onTouchEvent(event)
	}

	/**
	 * 拦截触摸事件，确保触摸事件被正确处理
	 */
	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		// 允许触摸事件传递给 onTouchEvent
		return false
	}

	/**
	 * 捏合缩放手势监听器
	 * 双指捏合调节 FOV：张开 → FOV 变小（放大/看近），捏合 → FOV 变大（缩小/看远）
	 */
	private inner class FovScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
		override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
			isScaling = true
			return true
		}

		override fun onScale(detector: ScaleGestureDetector): Boolean {
			// scaleFactor > 1 = 双指张开 → FOV 变小（放大）
			// scaleFactor < 1 = 双指捏合 → FOV 变大（缩小）
			val currentFov = renderer.getFov()
			val newFov = (currentFov / detector.scaleFactor)
				.coerceIn(Camera.MIN_FOV, Camera.MAX_FOV)

			producerThread.enqueueEvent {
				renderer.setFov(newFov)
			}
			onFovChangedListener?.invoke(newFov)
			return true
		}

		override fun onScaleEnd(detector: ScaleGestureDetector) {
			// isScaling 在 ACTION_UP 时重置，保证缩放结束后不会误触发拖动
		}
	}
}
