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
	private var mMVPMatrixHandle = 0

	/**
	 * vertex shader uniform mat4 u_MVMatrix
	 */
	private var mMVMatrixHandle = 0

	/**
	 * vertex shader attribute vec4 a_Position
	 */
	private var mPositionHandle = 0

	/**
	 * vertex shader attribute vec4 a_Color
	 */
	private var mColorHandle = 0

	/**
	 * vertex shader attribute vec2 a_TextureCoordinates
	 */
	private var mTextureCoordinatesHandle = 0

	/**
	 * fragment shader uniform sampler2D u_TextureUnit
	 */
	private var mTextureSamplerHandle = 0

	fun bindVertexBuffer(programHandle: Int, positionAttributeName: String, positionCoordinateSize: Int, vertexBuffer: Buffer) {
		mPositionHandle = GLES20.glGetAttribLocation(programHandle, positionAttributeName)
		GLES20.glVertexAttribPointer(mPositionHandle, positionCoordinateSize, GLES20.GL_FLOAT, false, 0, vertexBuffer)
		GLES20.glEnableVertexAttribArray(mPositionHandle)
	}

	fun bindColorBuffer(programHandle: Int, colorAttributeName: String, colorCoordinateSize: Int, colorBuffer: Buffer) {
		mColorHandle = GLES20.glGetAttribLocation(programHandle, colorAttributeName)
		GLES20.glVertexAttribPointer(mColorHandle, colorCoordinateSize, GLES20.GL_FLOAT, false, 0, colorBuffer)
		GLES20.glEnableVertexAttribArray(mColorHandle)
	}

	fun bindTextureCoordinatesBuffer(programHandle: Int, texCoordinatesAttributeName: String, textureCoordinatesSize: Int, textureCoordinatesBuffer: Buffer) {
		mTextureCoordinatesHandle = GLES20.glGetAttribLocation(programHandle, texCoordinatesAttributeName)
		GLES20.glVertexAttribPointer(mTextureCoordinatesHandle, textureCoordinatesSize, GLES20.GL_FLOAT, false, 0, textureCoordinatesBuffer)
		GLES20.glEnableVertexAttribArray(mTextureCoordinatesHandle)
	}

	fun bindMVPMatrix(programHandle: Int, MVPMatrixUniformName: String, MVPMatrix: FloatArray) {
		mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, MVPMatrixUniformName)
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, MVPMatrix, 0)
	}

	fun bindMVMatrix(programHandle: Int, MVMatrixUniformName: String, MVMatrix: FloatArray) {
		mMVMatrixHandle = GLES20.glGetUniformLocation(programHandle, MVMatrixUniformName)
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, MVMatrix, 0)
	}

	fun bindTextureSampler2D(programHandle: Int, textureSamplerUniformName: String, textureName: Int) {
		mTextureSamplerHandle = GLES20.glGetUniformLocation(programHandle, textureSamplerUniformName)
		GLES20.glUniform1i(mTextureSamplerHandle, textureName)
	}

	fun getMVPMatrixHandle(): Int {
		return mMVPMatrixHandle
	}

	fun getMVMatrixHandle(): Int {
		return mMVMatrixHandle
	}

	fun getPositionHandle(): Int {
		return mPositionHandle
	}

	fun getColorHandle(): Int {
		return mColorHandle
	}

	fun getTextureCoordinatesHandle(): Int {
		return mTextureCoordinatesHandle
	}

	fun getTextureSamplerHandle(): Int {
		return mTextureSamplerHandle
	}

	fun disableAllAttrbHandle() {
		GLES20.glDisableVertexAttribArray(mPositionHandle)
		GLES20.glDisableVertexAttribArray(mColorHandle)
		GLES20.glDisableVertexAttribArray(mTextureCoordinatesHandle)
	}
}