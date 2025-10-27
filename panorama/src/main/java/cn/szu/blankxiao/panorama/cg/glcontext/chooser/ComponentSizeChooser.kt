package cn.szu.blankxiao.panorama.cg.glcontext.chooser

import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/**
 * @author BlankXiao
 * @description ComponentSizeChooser
 * @date 2025-10-26 23:40
 */

class ComponentSizeChooser(
	redSize: Int, greenSize: Int, blueSize: Int,
	alphaSize: Int, depthSize: Int, stencilSize: Int
) : BaseConfigChooser(
	intArrayOf(
		EGL10.EGL_RED_SIZE, redSize,
		EGL10.EGL_GREEN_SIZE, greenSize,
		EGL10.EGL_BLUE_SIZE, blueSize,
		EGL10.EGL_ALPHA_SIZE, alphaSize,
		EGL10.EGL_DEPTH_SIZE, depthSize,
		EGL10.EGL_STENCIL_SIZE, stencilSize,
		EGL10.EGL_NONE
	)
) {
	private val mValue: IntArray

	// Subclasses can adjust these values:
	protected var mRedSize: Int
	protected var mGreenSize: Int
	protected var mBlueSize: Int
	protected var mAlphaSize: Int
	protected var mDepthSize: Int
	protected var mStencilSize: Int

	init {
		mValue = IntArray(1)
		mRedSize = redSize
		mGreenSize = greenSize
		mBlueSize = blueSize
		mAlphaSize = alphaSize
		mDepthSize = depthSize
		mStencilSize = stencilSize
	}


	override fun chooseConfig(
		egl: EGL10, display: EGLDisplay,
		configs: Array<EGLConfig?>
	): EGLConfig? {
		for (config in configs) {
			val d = findConfigAttrib(
				egl, display, config,
				EGL10.EGL_DEPTH_SIZE, 0
			)
			val s = findConfigAttrib(
				egl, display, config,
				EGL10.EGL_STENCIL_SIZE, 0
			)
			if ((d >= mDepthSize) && (s >= mStencilSize)) {
				val r = findConfigAttrib(
					egl, display, config,
					EGL10.EGL_RED_SIZE, 0
				)
				val g = findConfigAttrib(
					egl, display, config,
					EGL10.EGL_GREEN_SIZE, 0
				)
				val b = findConfigAttrib(
					egl, display, config,
					EGL10.EGL_BLUE_SIZE, 0
				)
				val a = findConfigAttrib(
					egl, display, config,
					EGL10.EGL_ALPHA_SIZE, 0
				)
				if ((r == mRedSize) && (g == mGreenSize)
					&& (b == mBlueSize) && (a == mAlphaSize)
				) {
					return config
				}
			}
		}
		return null
	}

	private fun findConfigAttrib(
		egl: EGL10, display: EGLDisplay?,
		config: EGLConfig?, attribute: Int, defaultValue: Int
	): Int {
		if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
			return mValue[0]
		}
		return defaultValue
	}
}
