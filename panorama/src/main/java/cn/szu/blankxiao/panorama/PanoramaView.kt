package cn.szu.blankxiao.panorama

import android.R
import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.TextureView
import android.widget.FrameLayout
import cn.szu.blankxiao.panorama.cg.gl.GLProducerThread
import cn.szu.blankxiao.panorama.sphere.SphereRenderer
import cn.szu.blankxiao.panorama.utils.ImageUtil
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author BlankXiao
 * @description PanoramaView
 * @date 2025-10-26 17:13
 */
class PanoramaView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs),
	TextureView.SurfaceTextureListener {

	// TextureView 作为openGL渲染内容的显示载体
	// 内部使用了SurfaceTexture 是opengl的直接渲染目标
	var renderView: TextureView

	// 渲染器
	var renderer: SphereRenderer

	// 渲染线程 继承Thread
	lateinit var producerThread: GLProducerThread

	// 是否已经初始化GL线程
	private var isGLThreadAvailable = false
	lateinit var currentBitmapUrl: String
	var placeHolder: Bitmap


	init {
		val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
		val configurationInfo = activityManager.deviceConfigurationInfo
		val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
		if (supportsEs2) {
			placeHolder = BitmapFactory.decodeResource(resources, R.color.black)

			renderView = TextureView(context)
			renderer = SphereRenderer(context)
			renderView.surfaceTextureListener = this
			addView(renderView)
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
		// 尝试从缓存读取图片资源
		val bitmap: Bitmap? = ImageUtil.loadBitmapFromCache(context, currentBitmapUrl)
		if (bitmap != null) {
			loadBitmapToGLTexture(bitmap)
		} else {
			// 不存在
			// 先显示默认图 再网络加载
			loadBitmapToGLTexture(placeHolder)
			ImageUtil.loadBitmapFromNetwork(context, currentBitmapUrl) { tempBitmap ->
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
		val bitmap: Bitmap? = ImageUtil.loadBitmapFromCache(context, currentBitmapUrl)
		if (bitmap != null) {
			changeBitmapToGLTexture(bitmap)
		} else {
			loadBitmapToGLTexture(placeHolder)
			ImageUtil.loadBitmapFromNetwork(context, currentBitmapUrl) { tempBitmap ->
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


}