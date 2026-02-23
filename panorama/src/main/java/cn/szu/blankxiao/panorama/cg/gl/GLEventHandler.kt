package cn.szu.blankxiao.panorama.cg.gl

import java.util.LinkedList

/**
 * @author BlankXiao
 * @description GLEventHandler
 * @date 2025-10-26 23:49
 */
class GLEventHandler {
	// 事件队列 存储执行的任务
	private val curGLQueue = LinkedList<Runnable?>()

	fun dequeueEventAndRun() {
		synchronized(curGLQueue) {
			while (!curGLQueue.isEmpty()) {
				curGLQueue.removeFirst()!!.run()
			}
		}
	}

	fun enqueueEvent(runnable: Runnable?) {
		synchronized(curGLQueue) {
			curGLQueue.addLast(runnable)
		}
	}
}