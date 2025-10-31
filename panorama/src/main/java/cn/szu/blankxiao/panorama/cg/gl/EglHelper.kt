package cn.szu.blankxiao.panorama.cg.gl

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import cn.szu.blankxiao.panorama.cg.glcontext.chooser.AndroidConfigChooser
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

/**
 * @author BlankXiao
 * @description EglHelper
 * @date 2025-10-26 23:51
 */
class EglHelper {

	// EGL context
	// EGL接口对象 调用EGL提供的函数
	private var curEGL: EGL10? = null

	// 显示设备的句柄 操作物理设备的媒介
	private var curEGLDisplay: EGLDisplay? = EGL10.EGL_NO_DISPLAY

	// 渲染上下文
	private var curEGLContext: EGLContext? = EGL10.EGL_NO_CONTEXT

	//	private EGLConfig[] mEGLConfig = new EGLConfig[1];
	// TODO 帧缓冲区配置??
	private var curEGLConfig: EGLConfig? = null

	// 渲染表面 EGL层面
	private var curEGLSurface: EGLSurface? = null

	// 渲染目标 安卓层面
	private var targetSurfaceTexture: SurfaceTexture? = null

	fun initEGLContext(surfaceTexture: SurfaceTexture?) {
		// 获取TextureView内置的SurfaceTexture作为EGL的绘图表面，也就是跟系统屏幕打交道
		if (surfaceTexture == null) {
			return
		}
		targetSurfaceTexture = surfaceTexture

		// 获取系统的EGL对象
		curEGL = EGLContext.getEGL() as EGL10?
		if (curEGL == null) {
			return
		}

		// 获取显示设备
		curEGLDisplay = curEGL!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
		if (curEGLDisplay === EGL10.EGL_NO_DISPLAY) {
			return
		}

		// version中存放当前的EGL版本号，版本号即为version[0].version[1]，如1.0
		val version = IntArray(2)

		// 初始化EGL
		if (!curEGL!!.eglInitialize(curEGLDisplay, version)) {
			return
		}

		// GLSurfaceView 中的代码，configChooser 可能会适配各种机型的 EGLConfig
		// Create a config chooser
		val configChooser = AndroidConfigChooser(AndroidConfigChooser.ConfigType.FASTEST)
		configChooser.setClientOpenGLESVersion(2)
		if (configChooser.findConfig(curEGL!!, curEGLDisplay)) {
			curEGLConfig = configChooser.chooseConfig(curEGL, curEGLDisplay)
		}

		// 根据SurfaceTexture创建EGL绘图表面
		curEGLSurface = curEGL!!.eglCreateWindowSurface(
			curEGLDisplay, curEGLConfig,
			targetSurfaceTexture, null
		)
		if (curEGLSurface === EGL10.EGL_NO_SURFACE) {
			return
		}

		// 指定哪个版本的OpenGL ES上下文，本文为OpenGL ES 2.0
		val contextAttribs = intArrayOf(
			EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
			EGL10.EGL_NONE
		)

		// 创建上下文，EGL10.EGL_NO_CONTEXT表示不和别的上下文共享资源
		curEGLContext =
			curEGL!!.eglCreateContext(
				curEGLDisplay,
				curEGLConfig,
				EGL10.EGL_NO_CONTEXT,
				contextAttribs
			)
		if (curEGLContext === EGL10.EGL_NO_CONTEXT) {
			return
		}

		// 指定mEGLContext为当前系统的EGL上下文，你可能发现了使用两个mEglSurface，第一个表示绘图表面，第二个表示读取表面
		if (!curEGL!!.eglMakeCurrent(curEGLDisplay, curEGLSurface, curEGLSurface, curEGLContext)) {
			return
		}
	}

	fun refreshSurfaceTexture(surfaceTexture: SurfaceTexture) {
		targetSurfaceTexture = surfaceTexture
		// 根据SurfaceTexture创建EGL绘图表面
		curEGLSurface = curEGL!!.eglCreateWindowSurface(
			curEGLDisplay, curEGLConfig,
			targetSurfaceTexture, null
		)
		if (curEGLSurface === EGL10.EGL_NO_SURFACE) {
			throw RuntimeException("eglCreateWindowSurface failed! " + curEGL!!.eglGetError())
		}

		// 指定哪个版本的OpenGL ES上下文，本文为OpenGL ES 2.0
		val contextAttribs = intArrayOf(
			EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
			EGL10.EGL_NONE
		)
		// 创建上下文，EGL10.EGL_NO_CONTEXT表示不和别的上下文共享资源
		curEGLContext =
			curEGL!!.eglCreateContext(
				curEGLDisplay,
				curEGLConfig,
				EGL10.EGL_NO_CONTEXT,
				contextAttribs
			)
		if (curEGLContext === EGL10.EGL_NO_CONTEXT) {
			throw RuntimeException("eglCreateContext fail failed! " + curEGL!!.eglGetError())
		}

		// 指定mEGLContext为当前系统的EGL上下文，你可能发现了使用两个mEglSurface，第一个表示绘图表面，第二个表示读取表面
		if (!curEGL!!.eglMakeCurrent(curEGLDisplay, curEGLSurface, curEGLSurface, curEGLContext)) {
			throw RuntimeException("eglMakeCurrent failed! " + curEGL!!.eglGetError())
		}
	}

	fun swapBuffers() {
		curEGL!!.eglSwapBuffers(curEGLDisplay, curEGLSurface)
	}

	fun releaseEGLContext() {
		curEGL!!.eglDestroyContext(curEGLDisplay, curEGLContext)
		curEGL!!.eglDestroySurface(curEGLDisplay, curEGLSurface)
		curEGLContext = EGL10.EGL_NO_CONTEXT
		curEGLSurface = EGL10.EGL_NO_SURFACE
		targetSurfaceTexture!!.release()
	}

}