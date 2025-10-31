package cn.szu.blankxiao.panorama.cg.gl

import android.graphics.SurfaceTexture
import cn.szu.blankxiao.panorama.cg.render.GLTextureRenderer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author BlankXiao
 * @description GLProducerThread
 * @date 2025-10-26 23:50
 */
class GLProducerThread(
	val surfaceTexture: SurfaceTexture,
	val textureRenderer: GLTextureRenderer,
	var shouldRender: AtomicBoolean
) : Thread() {

	// 渲染目标
	var renderMode = 0

	// EGL相关内容的封装 初始化 刷新 缓存交换 清理
	var curEGLHelper: EglHelper = EglHelper()

	// event
	// 本地锁
	var LOCK = Object()

	// 消息队列
	var eventHandler: GLEventHandler = GLEventHandler()

	var isPaused = false


	/**
	 * 向线程入队一个任务
	 * 事件包括 刷新页面 销毁上下文 加载/切换bitmap
	 * @param runnable
	 */
	fun enqueueEvent(runnable: Runnable?) {
		eventHandler.enqueueEvent(runnable)
	}

	/**
	 * 控制线程循环暂停
	 */
	fun onPause() {
		isPaused = true
	}

	/**
	 * 控制线程循环继续
	 */
	fun onResume() {
		isPaused = false
		requestRender()
	}

	fun requestRender() {
		synchronized(LOCK) {
			LOCK.notifyAll()
		}
	}

	fun releaseEglContext() {
		curEGLHelper.releaseEGLContext()
	}

	fun refreshSurfaceTexture(surfaceTexture: SurfaceTexture) {
		enqueueEvent {
			curEGLHelper.refreshSurfaceTexture(surfaceTexture)
		}
	}

	override fun run() {
		curEGLHelper.initEGLContext(surfaceTexture)
		textureRenderer.onGLContextAvailable()

		while (shouldRender.get()) {
			// 处理事件

			eventHandler.dequeueEventAndRun() // 先执行事件队列中没完成的任务

			// 渲染
			textureRenderer.onDrawFrame()

			// TODO 缓存处理？
			curEGLHelper.swapBuffers()

			if (isPaused) {
				pauseLoop()
			}

			sleep(5) // 预防帧数过高，手机发热
		}
	}

	private fun pauseLoop() {
		synchronized(LOCK) {
			try {
				LOCK.wait()
			} catch (e: InterruptedException) {
				e.printStackTrace()
			}
		}
	}
}