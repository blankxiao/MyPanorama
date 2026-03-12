package cn.szu.blankxiao.panorama.renderer

import android.graphics.SurfaceTexture
import cn.szu.blankxiao.panorama.cg.gl.GLProducerThread
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 管理 Panorama 渲染会话的生命周期：
 * - init/resume/resize/pause/release
 * - 统一向 GL 线程投递任务
 *
 * 设为 internal：仅限 panorama module 内使用。
 */
internal class RenderSession(private val renderer: Renderer) {
	private var initialized = false
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
			post {
				producerThread.releaseEglContext()
			}
		}
	}

	fun post(task: () -> Unit) {
		if (::producerThread.isInitialized) {
			producerThread.enqueueEvent { task() }
		}
	}
}