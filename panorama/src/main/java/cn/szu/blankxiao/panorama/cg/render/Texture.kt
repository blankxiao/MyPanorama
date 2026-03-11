package cn.szu.blankxiao.panorama.cg.render

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils

/**
 * @author BlankXiao
 * @description Texture
 * @date 2025-10-26 22:21
 */
class Texture {
	var textureName: Int = 0
	var textureUnit: Int = 0


	fun create() {
		// 生成纹理id
		val array = IntArray(1)
		GLES20.glGenTextures(1, array, 0)
		textureName = array[0]
		// 激活纹理单元
		textureUnit = GLES20.GL_TEXTURE0 + textureName
		GLES20.glActiveTexture(textureUnit)
	}

	fun loadBitmapToGLTexture(bitmap: Bitmap?) {
		if (bitmap == null || bitmap.isRecycled) return
		if (!isAvailable()) {
			throw RuntimeException("your opengl texture is not available or your bitmap is recycled")
		}
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureName)

		// 缩小/放大时的过滤方式（决定模糊程度 / aliasing）
		// 设置缩小的情况下过滤方式
		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_MIN_FILTER,
			GLES20.GL_LINEAR
		)
		// 设置放大的情况下过滤方式
		GLES20.glTexParameteri(
			GLES20.GL_TEXTURE_2D,
			GLES20.GL_TEXTURE_MAG_FILTER,
			GLES20.GL_LINEAR
		)

		// S/T 方向（水平/垂直）超出 [0,1] 时重复
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

		// 把 Bitmap 像素数据上传到当前绑定的纹理对象里
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
	}

	fun bindSampler(textureHandler: Int) {
		GLES20.glUniform1i(textureHandler, textureName)
	}

	fun destroy() {
		GLES20.glDeleteTextures(1, IntArray(1) { textureName }, 0);
	}

	fun isAvailable() = textureName != 0

}