package cn.szu.blankxiao.panorama.renderer

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
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_COLOR
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_TEXTURE_COORDINATES
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_VERTEX
import cn.szu.blankxiao.panorama.cg.render.GLTextureRenderer
import cn.szu.blankxiao.panorama.cg.render.Shader
import cn.szu.blankxiao.panorama.cg.render.Texture
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaMesher
import cn.szu.blankxiao.panorama.utils.OpenGLUtil

/**
 * 全景渲染器
 * 负责 OpenGL ES 渲染、传感器监听和触摸交互
 * 通过 PanoramaMesher 委托形状相关的几何体和旋转逻辑
 *
 * @author BlankXiao
 */
class Renderer(val context: Context) :
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

	/**
	 * 当前全景 Mesher（封装几何体 + 旋转策略）
	 * 通过 PanoramaMesher 接口统一访问 PanoramaMesh 和 PanoramaRotationStrategy
	 */
	private var mesher: PanoramaMesher = PanoramaMesher.create(MeshType.SPHERE)
	private var currentMeshType: MeshType = MeshType.SPHERE

	/**
	 * 偏移矩阵
	 */
	// 旋转矩阵
	private var rotationMatrix = floatArrayOf(
		1f, 0f, 0f, 0f,
		0f, 1f, 0f, 0f,
		0f, 0f, 1f, 0f,
		0f, 0f, 0f, 1f
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

	// 是否正在触摸
	private var isTouchActive = false

	// 触摸灵敏度（默认值，可通过接口调整）
	var touchSensitivity: Float = 0.5f

	override fun onGLContextAvailable() {
		// 注册绑定纹理
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
		// 渲染模型
		renderMesh()
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
		renderMesh()
		texture.loadBitmapToGLTexture(bitmap)
	}


	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

	override fun onSensorChanged(event: SensorEvent?) {
		if (event!!.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

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
	 * 开始触摸旋转
	 * 委托给 mesher 保存当前状态
	 */
	fun startTouchRotation() {
		isTouchActive = true
		mesher.onTouchStart(rotationMatrix, biasMatrix)
	}

	/**
	 * 应用触摸旋转增量
	 * 委托给 mesher 处理
	 * @param deltaX 水平移动距离（像素）
	 * @param deltaY 垂直移动距离（像素）
	 */
	fun applyTouchRotation(deltaX: Float, deltaY: Float) {
		if (!isTouchActive) return
		mesher.onTouchMove(deltaX, deltaY, touchSensitivity)
	}

	/**
	 * 结束触摸旋转
	 * 委托给 mesher 将触摸旋转合并到 biasMatrix
	 */
	fun endTouchRotation() {
		if (!isTouchActive) return
		isTouchActive = false
		biasMatrix = mesher.onTouchEnd(rotationMatrix, biasMatrix)
	}

	/**
	 * 设置全景图模型类型
	 * 通过 PanoramaMesher 工厂方法一次性切换几何体和旋转策略
	 * @param meshType 模型类型（SPHERE 或 CYLINDER）
	 */
	fun setMeshType(meshType: MeshType) {
		if (currentMeshType != meshType) {
			currentMeshType = meshType
			mesher = PanoramaMesher.create(meshType)
		}
	}

	/**
	 * 获取当前模型类型
	 */
	fun getMeshType(): MeshType = currentMeshType

	/**
	 * 设置 FOV（视场角）
	 * @param fovDegrees 视场角（度），范围 [Camera.MIN_FOV, Camera.MAX_FOV]
	 */
	fun setFov(fovDegrees: Float) {
		if (::camera.isInitialized) {
			camera.updateProjectionMatrix(fovDegrees)
		}
	}

	/**
	 * 获取当前 FOV
	 */
	fun getFov(): Float = if (::camera.isInitialized) camera.currentFov else Camera.DEFAULT_FOV


	/**
	 * 控制相机旋转
	 * 委托给 mesher 处理不同模型类型的旋转逻辑
	 */
	private fun controlCamera() {
		camera.rebuildViewMatrix()

		if (isTouchActive) {
			// 触摸模式：委托给 mesher
			mesher.applyTouchRotation(camera)
		} else {
			// 陀螺仪模式：委托给 mesher
			mesher.applyGyroRotation(camera, rotationMatrix, biasMatrix)
		}

		camera.rotate(-90.0f, Axis.AXIS_Y)
	}

	private fun renderMesh() {
		GLES20.glUseProgram(programHandle)

		vertexShader.bindVertexBuffer(
			programHandle,
			"a_Position",
			COORDINATES_PER_VERTEX,
			mesher.vertexBuffer
		)
		vertexShader.bindColorBuffer(
			programHandle,
			"a_Color",
			COORDINATES_PER_COLOR,
			mesher.colorBuffer
		)

		vertexShader.bindTextureCoordinatesBuffer(
			programHandle,
			"a_TextureCoordinates",
			COORDINATES_PER_TEXTURE_COORDINATES,
			mesher.textureCoordinateBuffer
		)

		fragmentShader.bindTextureSampler2D(programHandle, "u_Texture", texture.textureName)

		vertexShader.bindMVPMatrix(programHandle, "u_MVPMatrix", camera.getMVPMatrix())
		// TODO?
		texture.bindSampler(fragmentShader.getTextureSamplerHandle())

		GLES20.glDrawElements(
			GLES20.GL_TRIANGLES,
			mesher.indicesCount,
			GLES20.GL_UNSIGNED_SHORT,
			mesher.indicesBuffer
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

