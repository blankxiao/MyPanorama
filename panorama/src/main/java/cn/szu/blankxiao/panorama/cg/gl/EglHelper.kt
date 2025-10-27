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
	private var mEgl: EGL10? = null

	// 显示设备的句柄 操作物理设备的媒介
	private var mEGLDisplay: EGLDisplay? = EGL10.EGL_NO_DISPLAY

	// 渲染上下文
	private var mEGLContext: EGLContext? = EGL10.EGL_NO_CONTEXT

	//	private EGLConfig[] mEGLConfig = new EGLConfig[1];
	// TODO 帧缓冲区配置??
	private var mEGLConfig: EGLConfig? = null

	// 渲染表面 EGL层面
	private var mEglSurface: EGLSurface? = null

	// 渲染目标 安卓层面
	private var mSurfaceTexture: SurfaceTexture? = null

	fun initEGLContext(surfaceTexture: SurfaceTexture?) {
		// 获取TextureView内置的SurfaceTexture作为EGL的绘图表面，也就是跟系统屏幕打交道
		if (surfaceTexture == null) {
			return
		}
		mSurfaceTexture = surfaceTexture

		// 获取系统的EGL对象
		mEgl = EGLContext.getEGL() as EGL10?
		if (mEgl == null) {
		}

		// 获取显示设备
		mEGLDisplay = mEgl!!.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
		if (mEGLDisplay === EGL10.EGL_NO_DISPLAY) {
			return
		}

		// version中存放当前的EGL版本号，版本号即为version[0].version[1]，如1.0
		val version = IntArray(2)

		// 初始化EGL
		if (!mEgl!!.eglInitialize(mEGLDisplay, version)) {
			return
		}

		// GLSurfaceView 中的代码，configChooser 可能会适配各种机型的 EGLConfig
		// Create a config chooser
		val configChooser: AndroidConfigChooser =
			AndroidConfigChooser(AndroidConfigChooser.ConfigType.FASTEST)
		configChooser.setClientOpenGLESVersion(2)
		if (configChooser.findConfig(mEgl!!, mEGLDisplay)) {
			mEGLConfig = configChooser.chooseConfig(mEgl, mEGLDisplay)
		}

		// 根据SurfaceTexture创建EGL绘图表面
		mEglSurface = mEgl!!.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, mSurfaceTexture, null)
		if (mEglSurface === EGL10.EGL_NO_SURFACE) {
			return
		}

		// 指定哪个版本的OpenGL ES上下文，本文为OpenGL ES 2.0
		val contextAttribs = intArrayOf(
			EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
			EGL10.EGL_NONE
		)

		// 创建上下文，EGL10.EGL_NO_CONTEXT表示不和别的上下文共享资源
		mEGLContext =
			mEgl!!.eglCreateContext(mEGLDisplay, mEGLConfig, EGL10.EGL_NO_CONTEXT, contextAttribs)
		if (mEGLContext === EGL10.EGL_NO_CONTEXT) {
			return
		}

		// 指定mEGLContext为当前系统的EGL上下文，你可能发现了使用两个mEglSurface，第一个表示绘图表面，第二个表示读取表面
		if (!mEgl!!.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext)) {
			return
		}
	}

	fun refreshSurfaceTexture(surfaceTexture: SurfaceTexture) {
		mSurfaceTexture = surfaceTexture
		// 根据SurfaceTexture创建EGL绘图表面
		mEglSurface = mEgl!!.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, mSurfaceTexture, null)
		if (mEglSurface === EGL10.EGL_NO_SURFACE) {
			throw RuntimeException("eglCreateWindowSurface failed! " + mEgl!!.eglGetError())
		}

		// 指定哪个版本的OpenGL ES上下文，本文为OpenGL ES 2.0
		val contextAttribs = intArrayOf(
			EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
			EGL10.EGL_NONE
		)
		// 创建上下文，EGL10.EGL_NO_CONTEXT表示不和别的上下文共享资源
		mEGLContext =
			mEgl!!.eglCreateContext(mEGLDisplay, mEGLConfig, EGL10.EGL_NO_CONTEXT, contextAttribs)
		if (mEGLContext === EGL10.EGL_NO_CONTEXT) {
			throw RuntimeException("eglCreateContext fail failed! " + mEgl!!.eglGetError())
		}

		// 指定mEGLContext为当前系统的EGL上下文，你可能发现了使用两个mEglSurface，第一个表示绘图表面，第二个表示读取表面
		if (!mEgl!!.eglMakeCurrent(mEGLDisplay, mEglSurface, mEglSurface, mEGLContext)) {
			throw RuntimeException("eglMakeCurrent failed! " + mEgl!!.eglGetError())
		}
	}

	fun swapBuffers() {
		mEgl!!.eglSwapBuffers(mEGLDisplay, mEglSurface)
	}

	fun releaseEGLContext() {
		mEgl!!.eglDestroyContext(mEGLDisplay, mEGLContext)
		mEgl!!.eglDestroySurface(mEGLDisplay, mEglSurface)
		mEGLContext = EGL10.EGL_NO_CONTEXT
		mEglSurface = EGL10.EGL_NO_SURFACE
		mSurfaceTexture!!.release()
	}

}