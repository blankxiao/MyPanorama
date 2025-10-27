package cn.szu.blankxiao.panorama.cg.glcontext.chooser

import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLSurfaceView.EGLConfigChooser
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/**
 * @author BlankXiao
 * @description BaseConfigChooser
 * @date 2025-10-26 23:40
 */

abstract class BaseConfigChooser(configSpec: IntArray) : EGLConfigChooser {
	private var mEGLContextClientVersion = 0

	fun setEGLContextClientVersion(clientVersion: Int) {
		mEGLContextClientVersion = clientVersion
	}

	override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
		val num_config = IntArray(1)
		require(
			egl.eglChooseConfig(
				display, mConfigSpec, null, 0,
				num_config
			)
		) { "eglChooseConfig failed" }

		val numConfigs = num_config[0]

		require(numConfigs > 0) { "No configs match configSpec" }

		val configs = arrayOfNulls<EGLConfig>(numConfigs)
		require(
			egl.eglChooseConfig(
				display, mConfigSpec, configs, numConfigs,
				num_config
			)
		) { "eglChooseConfig#2 failed" }
		val config = chooseConfig(egl, display, configs)
		requireNotNull(config) { "No config chosen" }
		return config
	}

	abstract fun chooseConfig(
		egl: EGL10, display: EGLDisplay,
		configs: Array<EGLConfig?>
	): EGLConfig?

	protected var mConfigSpec: IntArray?

	init {
		mConfigSpec = filterConfigSpec(configSpec)
	}

	private fun filterConfigSpec(configSpec: IntArray): IntArray {
		if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
			return configSpec
		}
		/* We know none of the subclasses define EGL_RENDERABLE_TYPE.
		 * And we know the configSpec is well formed.
		 */
		val len = configSpec.size
		val newConfigSpec = IntArray(len + 2)
		System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1)
		newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE
		if (mEGLContextClientVersion == 2) {
			newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT /* EGL_OPENGL_ES2_BIT */
		} else {
			newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR /* EGL_OPENGL_ES3_BIT_KHR */
		}
		newConfigSpec[len + 1] = EGL10.EGL_NONE
		return newConfigSpec
	}
}
