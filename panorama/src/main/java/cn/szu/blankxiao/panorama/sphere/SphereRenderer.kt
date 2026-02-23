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
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_COLOR
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_TEXTURE_COORDINATES
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_VERTEX
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

	// 当前使用的几何体模型
	private var mesh: PanoramaMesh = Sphere.getDefault()
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

	/**
	 * 触摸旋转相关
	 */
	// 触摸旋转矩阵（累积的触摸旋转）
	private var touchRotationMatrix = FloatArray(16)

	// 触摸旋转的初始基准矩阵（触摸开始时保存的当前旋转状态）
	private var touchBaseMatrix = FloatArray(16)

	// 圆柱体模式专用：触摸开始时的 yaw 矩阵（从 rotationMatrix * biasMatrix 提取）
	private var touchBaseYawMatrix = FloatArray(16)

	// 圆柱体模式专用：累积的触摸 yaw 角度（度）
	private var touchYawDelta: Float = 0f

	// 是否正在触摸
	private var isTouchActive = false

	// 触摸灵敏度（默认值，可通过接口调整）
	var touchSensitivity: Float = 0.5f

	override fun onGLContextAvailable() {
		texture = Texture()
		texture.create()

		compileShaders()
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
		
		// 初始化触摸旋转矩阵为单位矩阵
		Matrix.setIdentityM(touchRotationMatrix, 0)
		Matrix.setIdentityM(touchBaseMatrix, 0)
		Matrix.setIdentityM(touchBaseYawMatrix, 0)
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
	 * 开始触摸旋转
	 * 保存当前旋转状态作为基准，重置触摸旋转矩阵
	 */
	fun startTouchRotation() {
		isTouchActive = true
		
		if (currentMeshType == MeshType.CYLINDER) {
			// 圆柱体模式：保存当前的 yaw 矩阵（从 rotationMatrix * biasMatrix 提取）
			// 这样可以保持与 rotationMatrix 的一致性
			val combinedMatrix = FloatArray(16)
			Matrix.multiplyMM(combinedMatrix, 0, rotationMatrix, 0, biasMatrix, 0)
			touchBaseYawMatrix = extractYawRotation(combinedMatrix)
			touchYawDelta = 0f
		} else {
			// 球体模式：保存完整矩阵
			Matrix.multiplyMM(touchBaseMatrix, 0, rotationMatrix, 0, biasMatrix, 0)
		}
		
		// 重置触摸旋转矩阵
		Matrix.setIdentityM(touchRotationMatrix, 0)
	}

	/**
	 * 应用触摸旋转增量
	 * @param deltaX 水平移动距离（像素）
	 * @param deltaY 垂直移动距离（像素）
	 */
	fun applyTouchRotation(deltaX: Float, deltaY: Float) {
		if (!isTouchActive) return

		// 转换为旋转角度（度）
		// 水平滑动：左右旋转（yaw，绕 Y 轴）
		// 垂直滑动：上下旋转（pitch，绕 X 轴，仅球体模式）
		val deltaYaw = -deltaX * touchSensitivity
		val deltaPitch = if (currentMeshType == MeshType.SPHERE) {
			-deltaY * touchSensitivity
		} else {
			0f  // 圆柱体模式不支持垂直旋转
		}

		if (currentMeshType == MeshType.CYLINDER) {
			// 圆柱体模式：直接累积 yaw 角度，避免矩阵提取的不稳定性
			touchYawDelta += deltaYaw
		} else {
			// 球体模式：创建增量旋转矩阵并累积
			// 旋转顺序：先 yaw（水平旋转，绕 Y 轴），再 pitch（垂直旋转，绕 X 轴）
			val deltaMatrix = FloatArray(16)
			Matrix.setIdentityM(deltaMatrix, 0)

			// 先应用 yaw（绕 Y 轴）
			if (deltaYaw != 0f) {
				Matrix.rotateM(deltaMatrix, 0, deltaYaw, 0f, 1f, 0f)
			}
			// 再应用 pitch（绕 X 轴）
			if (deltaPitch != 0f) {
				val pitchMatrix = FloatArray(16)
				Matrix.setIdentityM(pitchMatrix, 0)
				Matrix.rotateM(pitchMatrix, 0, deltaPitch, 1f, 0f, 0f)
				Matrix.multiplyMM(deltaMatrix, 0, deltaMatrix, 0, pitchMatrix, 0)
			}

			// 累积到触摸旋转矩阵
			val newTouchMatrix = FloatArray(16)
			Matrix.multiplyMM(newTouchMatrix, 0, touchRotationMatrix, 0, deltaMatrix, 0)
			touchRotationMatrix = newTouchMatrix
		}
	}

	/**
	 * 结束触摸旋转
	 * 将触摸旋转合并到 biasMatrix 中，使后续陀螺仪更新时保持触摸旋转
	 */
	fun endTouchRotation() {
		if (!isTouchActive) return
		
		isTouchActive = false
		
		if (currentMeshType == MeshType.CYLINDER) {
			// 圆柱体模式：将触摸旋转合并到 biasMatrix
			// 目标：使得 rotationMatrix * newBiasMatrix 提取的 yaw = touchBaseYawMatrix 应用 touchYawDelta 后的 yaw
			
			// 计算触摸结束时的最终 yaw 矩阵
			val finalYawMatrix = if (Math.abs(touchYawDelta) > 0.01f) {
				val deltaYawMatrix = FloatArray(16)
				Matrix.setIdentityM(deltaYawMatrix, 0)
				Matrix.rotateM(deltaYawMatrix, 0, touchYawDelta, 0f, 1f, 0f)
				val result = FloatArray(16)
				Matrix.multiplyMM(result, 0, touchBaseYawMatrix, 0, deltaYawMatrix, 0)
				result
			} else {
				touchBaseYawMatrix
			}
			
			// 计算新的 biasMatrix，使得 rotationMatrix * newBiasMatrix 提取的 yaw = finalYawMatrix
			// 方法：newBiasMatrix = rotationMatrix^(-1) * finalYawMatrix
			// 但这样不对，因为 rotationMatrix 可能包含 pitch/roll
			// 正确方法：计算当前 rotationMatrix * biasMatrix 的 yaw，然后计算增量
			
			// 获取当前的组合矩阵并提取 yaw
			val currentCombined = FloatArray(16)
			Matrix.multiplyMM(currentCombined, 0, rotationMatrix, 0, biasMatrix, 0)
			val currentYawMatrix = extractYawRotation(currentCombined)
			
			// 计算从 currentYawMatrix 到 finalYawMatrix 的增量
			// 方法：finalYawMatrix = currentYawMatrix * deltaYawMatrix
			// 因此：deltaYawMatrix = currentYawMatrix^(-1) * finalYawMatrix
			val invCurrentYawMatrix = FloatArray(16)
			Matrix.invertM(invCurrentYawMatrix, 0, currentYawMatrix, 0)
			val deltaYawMatrix = FloatArray(16)
			Matrix.multiplyMM(deltaYawMatrix, 0, invCurrentYawMatrix, 0, finalYawMatrix, 0)
			
			// 从 deltaYawMatrix 提取角度
			val deltaYawRad = Math.atan2(deltaYawMatrix[1].toDouble(), deltaYawMatrix[0].toDouble())
			var deltaYaw = Math.toDegrees(deltaYawRad).toFloat()
			// 归一化到 [-180, 180]
			while (deltaYaw > 180f) deltaYaw -= 360f
			while (deltaYaw < -180f) deltaYaw += 360f
			
			// 应用增量到 biasMatrix
			if (Math.abs(deltaYaw) > 0.01f) {
				val deltaMatrix = FloatArray(16)
				Matrix.setIdentityM(deltaMatrix, 0)
				Matrix.rotateM(deltaMatrix, 0, deltaYaw, 0f, 1f, 0f)
				val newBiasMatrix = FloatArray(16)
				Matrix.multiplyMM(newBiasMatrix, 0, biasMatrix, 0, deltaMatrix, 0)
				biasMatrix = newBiasMatrix
			}
			
			// 重置触摸 yaw
			touchYawDelta = 0f
		} else {
			// 球体模式：计算触摸结束时的最终旋转矩阵
			val finalTouchMatrix = FloatArray(16)
			Matrix.multiplyMM(finalTouchMatrix, 0, touchBaseMatrix, 0, touchRotationMatrix, 0)
			
			// 将触摸旋转合并到 biasMatrix
			// 目标：rotationMatrix * newBiasMatrix = finalTouchMatrix
			// 因此：newBiasMatrix = rotationMatrix^(-1) * finalTouchMatrix
			val invRotationMatrix = FloatArray(16)
			Matrix.invertM(invRotationMatrix, 0, rotationMatrix, 0)
			Matrix.multiplyMM(biasMatrix, 0, invRotationMatrix, 0, finalTouchMatrix, 0)
		}
		
		// 重置触摸旋转矩阵
		Matrix.setIdentityM(touchRotationMatrix, 0)
	}

	/**
	 * 设置全景图模型类型
	 * @param meshType 模型类型（SPHERE 或 CYLINDER）
	 */
	fun setMeshType(meshType: MeshType) {
		if (currentMeshType != meshType) {
			currentMeshType = meshType
			mesh = when (meshType) {
				MeshType.SPHERE -> Sphere.getDefault()
				MeshType.CYLINDER -> Cylinder.getDefault()
			}
		}
	}

	/**
	 * 获取当前模型类型
	 */
	fun getMeshType(): MeshType = currentMeshType


	/**
	 * 初始化相机
	 */
	private fun controlCamera() {
		camera.rebuildViewMatrix()
		
		if (isTouchActive) {
			// 触摸模式：使用触摸旋转
			when (currentMeshType) {
				MeshType.SPHERE -> {
					// 球体模式：基准矩阵 + 触摸旋转矩阵
					val combinedMatrix = FloatArray(16)
					Matrix.multiplyMM(combinedMatrix, 0, touchBaseMatrix, 0, touchRotationMatrix, 0)
					camera.rotate(combinedMatrix)
				}
				MeshType.CYLINDER -> {
					// 圆柱体模式：从基准 yaw 矩阵应用触摸增量
					// touchBaseYawMatrix 是从 rotationMatrix * biasMatrix 提取的 yaw 矩阵
					// 应用 touchYawDelta 增量到这个矩阵
					if (Math.abs(touchYawDelta) > 0.01f) {
						val deltaYawMatrix = FloatArray(16)
						Matrix.setIdentityM(deltaYawMatrix, 0)
						Matrix.rotateM(deltaYawMatrix, 0, touchYawDelta, 0f, 1f, 0f)
						// 将增量应用到基准矩阵：baseYawMatrix * deltaYawMatrix
						val finalYawMatrix = FloatArray(16)
						Matrix.multiplyMM(finalYawMatrix, 0, touchBaseYawMatrix, 0, deltaYawMatrix, 0)
						camera.rotate(finalYawMatrix)
					} else {
						camera.rotate(touchBaseYawMatrix)
					}
				}
			}
		} else {
			// 陀螺仪模式：使用传感器旋转
			when (currentMeshType) {
				MeshType.SPHERE -> {
					// 球体模式：完整的 3D 旋转
					camera.rotate(rotationMatrix)
					camera.rotate(biasMatrix)
				}
				MeshType.CYLINDER -> {
					// 圆柱体模式：只允许水平旋转（限制俯仰角）
					// 关键：先组合两个矩阵，再从组合结果中提取 yaw
					val combinedMatrix = FloatArray(16)
					Matrix.multiplyMM(combinedMatrix, 0, rotationMatrix, 0, biasMatrix, 0)
					
					// 从组合矩阵中提取 yaw 并应用
					val yawOnlyMatrix = extractYawRotation(combinedMatrix)
					camera.rotate(yawOnlyMatrix)
				}
			}
		}
		
		camera.rotate(-90.0f, Axis.AXIS_Y)
	}

	/**
	 * 从旋转矩阵中提取仅包含 yaw（绕 Y 轴旋转）的矩阵
	 * 用于圆柱体模式，限制只能水平旋转
	 * 
	 * Android 传感器坐标系：
	 * - 世界坐标系：X 指东，Y 指北，Z 指天
	 * - 设备坐标系：X 向右，Y 向上，Z 向用户
	 * 
	 * 旋转矩阵将设备坐标转换为世界坐标
	 * 我们需要提取绕世界 Z 轴（垂直轴）的旋转作为 yaw
	 * 
	 * Android Matrix 是列主序 (column-major)：
	 * [0]  [4]  [8]   [12]
	 * [1]  [5]  [9]   [13]
	 * [2]  [6]  [10]  [14]
	 * [3]  [7]  [11]  [15]
	 */
	private fun extractYawRotation(inputMatrix: FloatArray): FloatArray {
		// 从组合旋转矩阵中提取绕 Z 轴的旋转角度（传感器世界坐标系中 Z 是垂直轴）
		// ZYX 欧拉角分解：yaw = atan2(R[1][0], R[0][0])
		// 列主序下：R[1][0] = index 1, R[0][0] = index 0
		val yaw = Math.atan2(inputMatrix[1].toDouble(), inputMatrix[0].toDouble())
		
		// 映射到 OpenGL 坐标系：绕 Y 轴旋转（OpenGL 中 Y 是垂直轴）
		val result = FloatArray(16)
		Matrix.setIdentityM(result, 0)
		Matrix.rotateM(result, 0, Math.toDegrees(yaw).toFloat(), 0f, 1f, 0f)
		
		return result
	}

	private fun renderSphere() {
		GLES20.glUseProgram(programHandle)

		vertexShader.bindVertexBuffer(
			programHandle,
			"a_Position",
			COORDINATES_PER_VERTEX,
			mesh.vertexBuffer
		)
		vertexShader.bindColorBuffer(
			programHandle,
			"a_Color",
			COORDINATES_PER_COLOR,
			mesh.colorBuffer
		)

		vertexShader.bindTextureCoordinatesBuffer(
			programHandle,
			"a_TextureCoordinates",
			COORDINATES_PER_TEXTURE_COORDINATES,
			mesh.textureCoordinateBuffer
		)

		fragmentShader.bindTextureSampler2D(programHandle, "u_Texture", texture.textureName)

		vertexShader.bindMVPMatrix(programHandle, "u_MVPMatrix", camera.getMVPMatrix())

		texture.bindSampler(fragmentShader.getTextureSamplerHandle())

		GLES20.glDrawElements(
			GLES20.GL_TRIANGLES,
			mesh.indicesCount,
			GLES20.GL_UNSIGNED_SHORT,
			mesh.indicesBuffer
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