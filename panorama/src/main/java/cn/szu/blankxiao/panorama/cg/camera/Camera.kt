package cn.szu.blankxiao.panorama.cg.camera

import android.opengl.Matrix
import kotlin.math.tan

/**
 * @author BlankXiao
 * @description 作为视点 用于视图转换
 * @date 2025-10-26 17:20
 */
class Camera(val screenRatio: Float) {

	// model view projection 顶点变换
	// 原三维顶点 * mvpMatrix 即为二维坐标
	// projectionMatrix * viewMatrix
	// 模型坐标和世界坐标一样 无需处理
	private var mvpMatrix = FloatArray(16)

	// 投影矩阵
	private var projectionMatrix = FloatArray(16)

	// 视图矩阵
	private var viewMatrix = FloatArray(16)

	// 当前 FOV 角度（度）
	var currentFov: Float = DEFAULT_FOV
		private set

	init {
		updateProjectionMatrix(DEFAULT_FOV)
	}

	/**
	 * 更新投影矩阵（修改 FOV）
	 * @param fovDegrees 视场角（度），范围 [MIN_FOV, MAX_FOV]
	 */
	fun updateProjectionMatrix(fovDegrees: Float) {
		currentFov = fovDegrees.coerceIn(MIN_FOV, MAX_FOV)
		val top = (tan(currentFov * Math.PI / 360.0) * Z_NEAR).toFloat()
		val bottom = -top
		val left = screenRatio * bottom
		val right = screenRatio * top
		Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, Z_NEAR, Z_FAR)
	}

	/**
	 * 输出FloatArray(16)作为旋转矩阵
	 */
	fun rotate(rotationMatrix: FloatArray) {
		Matrix.multiplyMM(viewMatrix, 0, viewMatrix, 0, rotationMatrix, 0)
	}

	/**
	 * 旋转角度和方向轴
	 */
	fun rotate(angle: Float, axis: Axis) {
		when (axis) {
			Axis.AXIS_X -> Matrix.rotateM(viewMatrix, 0, angle, 1.0f, 0.0f, 0.0f)
			Axis.AXIS_Y -> Matrix.rotateM(viewMatrix, 0, angle, 0.0f, 1.0f, 0.0f)
			Axis.AXIS_Z -> Matrix.rotateM(viewMatrix, 0, angle, 0.0f, 0.0f, 1.0f)
			else -> throw RuntimeException()
		}
	}

	/**
	 * 视图矩阵重设为单位矩阵
	 */
	fun rebuildViewMatrix() {
		Matrix.setIdentityM(viewMatrix, 0)
	}

	fun getMVPMatrix(): FloatArray {
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
		return mvpMatrix
	}

	companion object {
		private const val Z_NEAR = 0.1f
		private const val Z_FAR = 100.0f
		const val DEFAULT_FOV = 90.0f
		const val MIN_FOV = 30.0f
		const val MAX_FOV = 120.0f
	}
}
