package cn.szu.blankxiao.panorama.sphere

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.Matrix
import cn.szu.blankxiao.panorama.R
import cn.szu.blankxiao.panorama.cg.camera.Axis
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.cg.mesh.AbstractMesh
import cn.szu.blankxiao.panorama.cg.mesh.AbstractMesh.Companion.COORDINATES_PER_COLOR
import cn.szu.blankxiao.panorama.cg.mesh.AbstractMesh.Companion.COORDINATES_PER_TEXTURE_COORDINATES
import cn.szu.blankxiao.panorama.cg.mesh.AbstractMesh.Companion.COORDINATES_PER_VERTEX
import cn.szu.blankxiao.panorama.cg.render.GLTextureRenderer
import cn.szu.blankxiao.panorama.cg.render.Shader
import cn.szu.blankxiao.panorama.cg.render.Texture
import cn.szu.blankxiao.panorama.utils.OpenGLUtil

/**
 * @author BlankXiao
 * @description GLRendererListener
 * @date 2025-10-26 22:18
 */
class SphereRenderer(val context: Context) :
	GLTextureRenderer,
	SensorEventListener {


	// 渲染目标
	lateinit var texture: Texture

	/**
	 * 句柄
	 */
	var vertexShaderHandle = 0
	var fragmentShaderHandle = 0
	var programHandle = 0

	lateinit var camera: Camera

	/**
	 * 着色器
	 */
	// 顶点着色器
	var vertexShader = Shader()
	var fragmentShader: Shader = Shader()

	// 球体
	var sphere: Sphere = Sphere.getDefaultSphere()

	/**
	 * 偏移矩阵
	 */
	// 旋转矩阵
	private var rotationMatrix = floatArrayOf(
		1f, 0f, 0f, 0f,
		0f, 1f, 0f, 0f,
		0f, 0f, 1f, 0f,
		0f, 0f, 0f, 0f
	)

	// 偏移矩阵
	private var biasMatrix = FloatArray(16)

	/**
	 * 陀螺仪
	 */
	var sensorManager: SensorManager =
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

	// TODO 可空?
	var sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!!


	var isFirstFrame = true

	var isGyroTrackingEnabled = true

	var rotVecValues: FloatArray? = null

	private val rotationQuaternion = FloatArray(4)

	override fun onGLContextAvailable() {
		texture = Texture()
		texture.create()

		compileShaders()
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
	}

	override fun onSurfaceChanged(width: Int, height: Int) {
		GLES20.glViewport(0, 0, width, height)
		val ratio = width.toFloat() / height
		camera = Camera(ratio)
	}

	override fun onDrawFrame() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
		// 相机初始化
		controlCamera()
		// 渲染球体
		renderSphere()
	}

	override fun onAttached() {
		sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
	}

	override fun onDetached() {
		sensorManager.unregisterListener(this)
	}

	override fun loadBitmap(bitmap: Bitmap?) {
		texture.loadBitmapToGLTexture(bitmap)
	}

	override fun changeTextureBitmap(bitmap: Bitmap?) {
		texture.destroy()
		texture.create()
		renderSphere()
		texture.loadBitmapToGLTexture(bitmap)
	}


	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

	override fun onSensorChanged(event: SensorEvent?) {
		if (event!!.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
			if (isFirstFrame) { // 初始化时，先给一个初始角度，以便能绘制出第一帧的图
				isFirstFrame = false
				val orientationMatrix = FloatArray(16)
				Matrix.setIdentityM(orientationMatrix, 0)

				if (rotVecValues == null) {
					rotVecValues = FloatArray(event.values.size)
				}
				for (i in rotVecValues!!.indices) {
					rotVecValues!![i] = event.values[i]
				}

				SensorManager.getQuaternionFromVector(rotationQuaternion, rotVecValues)
				SensorManager.getRotationMatrixFromVector(orientationMatrix, rotVecValues)
				rotationMatrix = orientationMatrix

				val invertMatrix = FloatArray(16)
				Matrix.invertM(invertMatrix, 0, orientationMatrix, 0)
				biasMatrix = invertMatrix
				return
			}

			if (isGyroTrackingEnabled) {
				for (i in rotVecValues?.indices!!) {
					rotVecValues?.set(i, event.values[i])
				}

				if (rotVecValues != null) {
					SensorManager.getQuaternionFromVector(rotationQuaternion, rotVecValues)
					SensorManager.getRotationMatrixFromVector(rotationMatrix, rotVecValues)
				}
			}
		}
	}

	fun reCenter() {
		val invertMatrix = FloatArray(16)
		// 计算逆矩阵
		Matrix.invertM(invertMatrix, 0, rotationMatrix, 0)
		// TODO 为什么是逆矩阵? 不是单位矩阵?
		biasMatrix = invertMatrix
	}

	fun enableGyroTracking(enabled: Boolean) {
		isGyroTrackingEnabled = enabled
	}


	/**
	 * 初始化相机
	 */
	private fun controlCamera() {
		camera.rebuildViewMatrix()
		camera.rotate(rotationMatrix)
		camera.rotate(biasMatrix)
		camera.rotate(-90.0f, Axis.AXIS_Y)
	}

	private fun renderSphere() {
		GLES20.glUseProgram(programHandle)

		vertexShader.bindVertexBuffer(
			programHandle,
			"a_Position",
			COORDINATES_PER_VERTEX,
			sphere.vertexBuffer
		)
		vertexShader.bindColorBuffer(
			programHandle,
			"a_Color",
			COORDINATES_PER_COLOR,
			sphere.colorBuffer
		)

		vertexShader.bindTextureCoordinatesBuffer(
			programHandle,
			"a_TextureCoordinates",
			COORDINATES_PER_TEXTURE_COORDINATES,
			sphere.textureCoordinateBuffer
		)

		fragmentShader.bindTextureSampler2D(programHandle, "u_Texture", texture.textureName)

		vertexShader.bindMVPMatrix(programHandle, "u_MVPMatrix", camera.getMVPMatrix())

		texture.bindSampler(fragmentShader.getTextureSamplerHandle())

		GLES20.glDrawElements(
			GLES20.GL_TRIANGLES,
			sphere.indicesShorts.size,
			GLES20.GL_UNSIGNED_SHORT,
			sphere.indicesBuffer
		)

		vertexShader.disableAllAttrbHandle()
	}

	private fun compileShaders() {
		vertexShaderHandle = OpenGLUtil.loadAndCompileShader(
			context,
			R.raw.sphere_vertex_shader,
			GLES20.GL_VERTEX_SHADER
		)
		fragmentShaderHandle = OpenGLUtil.loadAndCompileShader(
			context,
			R.raw.sphere_fragment_shader,
			GLES20.GL_FRAGMENT_SHADER
		)
		programHandle = OpenGLUtil.createAndLinkProgram(
			vertexShaderHandle, fragmentShaderHandle,
			arrayOf("a_Position", "a_Color", "a_TexCoordinate")
		)
	}
}