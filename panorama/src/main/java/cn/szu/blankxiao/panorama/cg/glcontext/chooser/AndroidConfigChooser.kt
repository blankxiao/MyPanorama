package cn.szu.blankxiao.panorama.cg.glcontext.chooser

import android.R.attr.type
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView.EGLConfigChooser
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay

/**
 * @author BlankXiao
 * @description AndroidConfigChooser
 * @date 2025-10-26 23:48
 */

class AndroidConfigChooser(type: ConfigType?) : EGLConfigChooser {
	private var clientOpenGLESVersion: Int = 0
	private var bestConfig: EGLConfig? = null
	private var fastestConfig: EGLConfig? = null
	private var choosenConfig: EGLConfig? = null
	private var pixelFormat: Int = 0
	private var verbose: Boolean = false

	enum class ConfigType {
		/**
		 * RGB565, 0 alpha, 16 depth, 0 stencil
		 */
		FASTEST,

		/**
		 * RGB???, 0 alpha, >=16 depth, 0 stencil
		 */
		BEST,

		/**
		 * Turn off config chooser and use hardcoded
		 * setEGLContextClientVersion(2); setEGLConfigChooser(5, 6, 5, 0, 16,
		 * 0);
		 */
		LEGACY
	}


	/**
	 * Gets called by the GLSurfaceView class to return the best config
	 */
	override fun chooseConfig(egl: EGL10?, display: EGLDisplay?): EGLConfig? {
//		logger.info("GLSurfaceView asks for egl config, returning: ");
//		logEGLConfig(choosenConfig, display, egl);
		return choosenConfig
	}

	/**
	 * findConfig is used to locate the best config and init the chooser with
	 *
	 * @param egl
	 * @param display
	 * @return true if successfull, false if no config was found
	 */
	fun findConfig(egl: EGL10, display: EGLDisplay?): Boolean {
		if (type == ConfigType.BEST.ordinal) {
			var compChooser = ComponentSizeChooser(8, 8, 8, 8, 16, 0)
			choosenConfig = compChooser.chooseConfig(egl, display!!)
			if (choosenConfig == null) {
				compChooser = ComponentSizeChooser(8, 8, 8, 8, 16, 0)
				choosenConfig = compChooser.chooseConfig(egl, display)
				if (choosenConfig == null) {
					compChooser = ComponentSizeChooser(8, 8, 8, 0, 16, 0)
					choosenConfig = compChooser.chooseConfig(egl, display)
				}
			}
			//			logger.info("JME3 using best EGL configuration available here: ");
		} else {
			val compChooser = ComponentSizeChooser(5, 6, 5, 0, 16, 0)
			choosenConfig = compChooser.chooseConfig(egl, display!!)
			//			logger.info("JME3 using fastest EGL configuration available here: ");
		}
		if (choosenConfig != null) {
//			logger.info("JME3 using choosen config: ");
//			logEGLConfig(choosenConfig, display, egl);
			pixelFormat = getPixelFormat(choosenConfig, display, egl)
			clientOpenGLESVersion = getOpenGLVersion(choosenConfig, display, egl)
			return true
		} else {
//			logger.severe("###ERROR### Unable to get a valid OpenGL ES 2.0 config, nether Fastest nor Best found! Bug. Please report this.");
			clientOpenGLESVersion = 1
			pixelFormat = PixelFormat.UNKNOWN
			return false
		}
	}

	private fun getPixelFormat(conf: EGLConfig?, display: EGLDisplay?, egl: EGL10): Int {
		val value = IntArray(1)
		var result = PixelFormat.RGB_565
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RED_SIZE, value)
		if (value[0] == 8) {
			result = PixelFormat.RGBA_8888
			/*
            egl.eglGetConfigAttrib(display, conf, EGL10.EGL_ALPHA_SIZE, value);
            if (value[0] == 8)
            {
                result = PixelFormat.RGBA_8888;
            }
            else
            {
                result = PixelFormat.RGB_888;
            }*/
		}
		if (verbose) {
//			logger.log(Level.INFO, "Using PixelFormat {0}", result);
		}
		// return result; TODO Test pixelformat
		return PixelFormat.TRANSPARENT
	}

	private fun getOpenGLVersion(conf: EGLConfig?, display: EGLDisplay?, egl: EGL10): Int {
		val value = IntArray(1)
		var result = 1
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RENDERABLE_TYPE, value)
		// Check if conf is OpenGL ES 2.0
		if ((value[0] and EGL_OPENGL_ES2_BIT) != 0) {
			result = 2
		}
		return result
	}

	/**
	 * log output with egl config details
	 *
	 * @param conf
	 * @param display
	 * @param egl
	 */
	fun logEGLConfig(conf: EGLConfig?, display: EGLDisplay?, egl: EGL10) {
		val value = IntArray(1)
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RED_SIZE, value)
		//		logger.info(String.format("EGL_RED_SIZE  = %d", value[0]));
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_GREEN_SIZE, value)
		//		logger.info(String.format("EGL_GREEN_SIZE  = %d", value[0]));
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_BLUE_SIZE, value)
		//		logger.info(String.format("EGL_BLUE_SIZE  = %d", value[0]));
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_ALPHA_SIZE, value)
		//		logger.info(String.format("EGL_ALPHA_SIZE  = %d", value[0]));
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_DEPTH_SIZE, value)
		//		logger.info(String.format("EGL_DEPTH_SIZE  = %d", value[0]));
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_STENCIL_SIZE, value)
		//		logger.info(String.format("EGL_STENCIL_SIZE  = %d", value[0]));
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_RENDERABLE_TYPE, value)
		//		logger.info(String.format("EGL_RENDERABLE_TYPE  = %d", value[0]));
		egl.eglGetConfigAttrib(display, conf, EGL10.EGL_SURFACE_TYPE, value)
		//		logger.info(String.format("EGL_SURFACE_TYPE  = %d", value[0]));
	}

	fun getClientOpenGLESVersion(): Int {
		return clientOpenGLESVersion
	}

	fun setClientOpenGLESVersion(clientOpenGLESVersion: Int) {
		this.clientOpenGLESVersion = clientOpenGLESVersion
	}

	fun getPixelFormat(): Int {
		return pixelFormat
	}

	companion object {
		private val TAG: String = AndroidConfigChooser::class.java.getSimpleName()

		private const val EGL_OPENGL_ES2_BIT = 4
	}
}
