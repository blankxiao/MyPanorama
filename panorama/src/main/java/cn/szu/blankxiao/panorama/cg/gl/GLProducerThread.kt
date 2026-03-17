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

	/**
	 * 请求线程安全退出渲染循环。
	 * 会将 shouldRender 置为 false，并唤醒可能在 wait 中的循环。
	 */
	fun stopRender() {
		shouldRender.set(false)
		// 确保如果当前在暂停等待中可以被唤醒并尽快退出
		requestRender()
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
			// 先执行事件队列中没完成的任务
			eventHandler.dequeueEventAndRun()

			// 渲染新的纹理到缓存 然后和正在展示的缓存交换
			textureRenderer.onDrawFrame()

			// 交换缓存 刷新页面
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