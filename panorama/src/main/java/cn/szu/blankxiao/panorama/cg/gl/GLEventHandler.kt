package cn.szu.blankxiao.panorama.cg.gl

import java.util.LinkedList

/**
 * @author BlankXiao
 * @description GLEventHandler
 * @date 2025-10-26 23:49
 */
class GLEventHandler {
	// 事件队列 存储执行的任务
	private val mGLQueue = LinkedList<Runnable?>()

	fun dequeueEventAndRun() {
		while (!mGLQueue.isEmpty()) {
			mGLQueue.removeFirst()!!.run()
		}
	}

	fun enqueueEvent(runnable: Runnable?) {
		synchronized(mGLQueue) {
			mGLQueue.addLast(runnable)
		}
	}
}