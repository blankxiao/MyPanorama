package cn.szu.blankxiao.panorama.renderer

import android.graphics.SurfaceTexture
import cn.szu.blankxiao.panorama.cg.gl.GLProducerThread
import cn.szu.blankxiao.panorama.cg.render.GLTextureRenderer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 管理 Panorama 渲染会话的生命周期：
 * - init/resume/resize/pause/release
 * - 统一向 GL 线程投递任务
 *
 * 设为 internal：仅限 panorama module 内使用。
 */
internal class RenderSession(private val renderer: GLTextureRenderer) {
	private var initialized = false
	// 渲染线程 继承Thread
	// 初始化GL环境
	private lateinit var producerThread: GLProducerThread

	fun isReady(): Boolean = initialized

	fun init(surface: SurfaceTexture, width: Int, height: Int, onReady: () -> Unit) {
		initialized = true
		producerThread = GLProducerThread(surface, renderer, AtomicBoolean(true))
		producerThread.start()
		post { renderer.onSurfaceChanged(width, height) }
		onReady()
	}

	fun resume(surface: SurfaceTexture, onResume: () -> Unit) {
		// 已经加载了 EGL 上下文，只需刷新 surface 并恢复绘制循环
		producerThread.refreshSurfaceTexture(surface)
		onResume()
		producerThread.onResume()
	}

	fun resize(width: Int, height: Int) {
		post { renderer.onSurfaceChanged(width, height) }
	}

	fun pause() {
		if (initialized) {
			producerThread.onPause()
		}
	}

	fun release() {
		if (initialized) {
			// 请求线程退出渲染循环并在 GL 线程中释放 EGL 资源。
			post {
				producerThread.releaseEglContext()
				producerThread.stopRender()
			}
			initialized = false
		}
	}

	fun post(task: () -> Unit) {
		if (::producerThread.isInitialized) {
			producerThread.enqueueEvent { task() }
		}
	}
}