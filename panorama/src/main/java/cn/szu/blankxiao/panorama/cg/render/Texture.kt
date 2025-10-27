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


	fun create(){
		val array = IntArray(1)
		GLES20.glGenTextures(1, array, 0)
		textureName = array[0]

		textureUnit = GLES20.GL_TEXTURE0 + textureName
		GLES20.glActiveTexture(textureUnit)
	}

	fun loadBitmapToGLTexture(bitmap: Bitmap?) {
		if (bitmap == null) return
		if (bitmap.isRecycled) return
		if (isAvailable()) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureName)

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

			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

		} else {
			throw RuntimeException("your opengl texture is not available or your bitmap is recycled")
		}
	}

	fun bindSampler(textureHandler: Int){
		GLES20.glUniform1f(textureHandler, textureName.toFloat())
	}

	fun destroy(){
		GLES20.glDeleteTextures(1, IntArray(1) {textureName}, 0);
	}

	fun isAvailable() = textureName != 0

}