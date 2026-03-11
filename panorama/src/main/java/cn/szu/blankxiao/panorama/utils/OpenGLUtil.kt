package cn.szu.blankxiao.panorama.utils

import android.content.Context
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*

/**
 * @author BlankXiao
 * @description OpenGLUtil
 * @date 2025-10-26 21:40
 */
object OpenGLUtil {

	/**
	 * 传入 shader 文件 resId，获得 shaderHandle
	 *
	 * @param context
	 * @param rawRes
	 * @param shaderType GLES20.GL_VERTEX_SHADER or GLES20.GL_FRAGMENT_SHADER
	 * @return shaderHandle
	 */
	fun loadAndCompileShader(context: Context, rawRes: Int, shaderType: Int): Int {
		val shaderSource = FileUtil.readFileFromRawResource(context, rawRes)
		var shaderHandle = GLES20.glCreateShader(shaderType)

		if (shaderHandle != 0) {
			// Pass in the shader source.
			GLES20.glShaderSource(shaderHandle, shaderSource)

			// Compile the shader.
			GLES20.glCompileShader(shaderHandle)

			// Get the compilation status.
			val compileStatus = IntArray(1)
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

			// If the compilation failed, delete the shader.
			if (compileStatus[0] == 0) {
				GLES20.glDeleteShader(shaderHandle)
				shaderHandle = 0
			}
		}

		if (shaderHandle == 0) {
			throw RuntimeException("Error creating shader.")
		}

		return shaderHandle
	}

	/**
	 * Helper function to compile and link a program.
	 *
	 * @param vertexShaderHandle   An OpenGL handle to an already-compiled vertex shader.
	 * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
	 * @param attributes           Attributes that need to be bound to the program.
	 * @return An OpenGL handle to the program.
	 */
	fun createAndLinkProgram(
		vertexShaderHandle: Int,
		fragmentShaderHandle: Int,
		attributes: Array<String>?,
	): Int {
		var programHandle = GLES20.glCreateProgram()

		if (programHandle != 0) {
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle)

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle)

			// Bind attributes
			if (attributes != null) {
				for (i in 0 until attributes.size) {
					GLES20.glBindAttribLocation(programHandle, i, attributes[i])
				}
			}

			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle)

			// Get the link status.
			val linkStatus = IntArray(1)
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)

			// If the link failed, delete the program.
			if (linkStatus[0] == 0) {
				GLES20.glDeleteProgram(programHandle)
				programHandle = 0
			}
		}

		if (programHandle == 0) {
			throw RuntimeException("Error creating program.")
		}

		return programHandle
	}

	fun floatArray2FloatBuffer(floats: FloatArray): FloatBuffer {
		val bb = ByteBuffer.allocateDirect(floats.size * 4)
		bb.order(ByteOrder.nativeOrder())
		val floatBuffer = bb.asFloatBuffer()
		floatBuffer.put(floats)
		floatBuffer.position(0)
		return floatBuffer
	}

	fun shortArray2ShortBuffer(shorts: ShortArray): ShortBuffer {
		val bb = ByteBuffer.allocateDirect(shorts.size * 2)
		bb.order(ByteOrder.nativeOrder())
		val shortBuffer = bb.asShortBuffer()
		shortBuffer.put(shorts)
		shortBuffer.position(0)
		return shortBuffer
	}

	fun <P> toPrimitiveArray(list: List<*>, arrayType: Class<P>): P {
		if (!arrayType.isArray) {
			throw IllegalArgumentException(arrayType.toString())
		}
		val primitiveType = arrayType.componentType
		if (!primitiveType.isPrimitive) {
			throw IllegalArgumentException(primitiveType.toString())
		}

		val array = arrayType.cast(java.lang.reflect.Array.newInstance(primitiveType, list.size))

		for (i in list.indices) {
			java.lang.reflect.Array.set(array, i, list[i])
		}

		return array
	}
}