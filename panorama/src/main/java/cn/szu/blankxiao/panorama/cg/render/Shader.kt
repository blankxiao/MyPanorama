package cn.szu.blankxiao.panorama.cg.render

import android.opengl.GLES20
import java.nio.Buffer

/**
 * @author BlankXiao
 * @description Shader
 * @date 2025-10-26 22:21
 */

class Shader {
	/**
	 * vertex shader uniform mat4 u_MVPMatrix
	 */
	private var MVPMatrixHandle = 0

	/**
	 * vertex shader uniform mat4 u_MVMatrix
	 */
	private var mMVMatrixHandle = 0

	/**
	 * vertex shader attribute vec4 a_Position
	 */
	private var positionHandle = 0

	/**
	 * vertex shader attribute vec4 a_Color
	 */
	private var colorHandle = 0

	/**
	 * vertex shader attribute vec2 a_TextureCoordinates
	 */
	private var textureCoordinatesHandle = 0

	/**
	 * fragment shader uniform sampler2D u_TextureUnit
	 */
	private var textureSamplerHandle = 0

	fun bindVertexBuffer(
		programHandle: Int,
		positionAttributeName: String,
		positionCoordinateSize: Int,
		vertexBuffer: Buffer
	) {
		positionHandle = GLES20.glGetAttribLocation(programHandle, positionAttributeName)
		GLES20.glVertexAttribPointer(
			positionHandle, positionCoordinateSize,
			GLES20.GL_FLOAT, false,
			0, vertexBuffer
		)
		GLES20.glEnableVertexAttribArray(positionHandle)
	}

	fun bindColorBuffer(
		programHandle: Int,
		colorAttributeName: String,
		colorCoordinateSize: Int,
		colorBuffer: Buffer
	) {
		colorHandle = GLES20.glGetAttribLocation(programHandle, colorAttributeName)
		GLES20.glVertexAttribPointer(
			colorHandle, colorCoordinateSize,
			GLES20.GL_FLOAT,
			false, 0, colorBuffer
		)
		GLES20.glEnableVertexAttribArray(colorHandle)
	}

	fun bindTextureCoordinatesBuffer(
		programHandle: Int,
		texCoordinatesAttributeName: String,
		textureCoordinatesSize: Int,
		textureCoordinatesBuffer: Buffer
	) {
		textureCoordinatesHandle =
			GLES20.glGetAttribLocation(programHandle, texCoordinatesAttributeName)
		GLES20.glVertexAttribPointer(
			textureCoordinatesHandle, textureCoordinatesSize,
			GLES20.GL_FLOAT, false,
			0, textureCoordinatesBuffer
		)
		GLES20.glEnableVertexAttribArray(textureCoordinatesHandle)
	}

	fun bindMVPMatrix(programHandle: Int, MVPMatrixUniformName: String, MVPMatrix: FloatArray) {
		MVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, MVPMatrixUniformName)
		GLES20.glUniformMatrix4fv(MVPMatrixHandle, 1, false, MVPMatrix, 0)
	}

	fun bindMVMatrix(programHandle: Int, MVMatrixUniformName: String, MVMatrix: FloatArray) {
		mMVMatrixHandle = GLES20.glGetUniformLocation(programHandle, MVMatrixUniformName)
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, MVMatrix, 0)
	}

	fun bindTextureSampler2D(
		programHandle: Int,
		textureSamplerUniformName: String,
		textureName: Int
	) {
		textureSamplerHandle = GLES20.glGetUniformLocation(programHandle, textureSamplerUniformName)
		GLES20.glUniform1i(textureSamplerHandle, textureName)
	}

	fun getTextureSamplerHandle(): Int {
		return textureSamplerHandle
	}

	fun disableAllAttrbHandle() {
		GLES20.glDisableVertexAttribArray(positionHandle)
		GLES20.glDisableVertexAttribArray(colorHandle)
		GLES20.glDisableVertexAttribArray(textureCoordinatesHandle)
	}
}