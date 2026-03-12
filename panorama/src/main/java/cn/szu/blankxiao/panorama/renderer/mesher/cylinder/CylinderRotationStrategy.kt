package cn.szu.blankxiao.panorama.renderer.mesher.cylinder

import android.opengl.Matrix
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaRotationStrategy
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.atan2

/**
 * 圆柱体旋转策略
 * 只允许水平旋转（yaw），不支持垂直旋转
 *
 * @author BlankXiao
 */
class CylinderRotationStrategy : PanoramaRotationStrategy {

	// 触摸开始时的 yaw 矩阵（从 rotationMatrix * biasMatrix 提取，OpenGL Y-rotation）
	private var touchBaseYawMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

	// 触摸开始时的 yaw 角度（度，传感器坐标系 Z 轴旋转角）
	private var touchBaseYawAngle: Float = 0f

	// 累积的触摸 yaw 角度（度）
	private var touchYawDelta: Float = 0f

	override fun applyGyroRotation(camera: Camera, rotationMatrix: FloatArray, biasMatrix: FloatArray) {
		// 圆柱体模式：只允许水平旋转（限制俯仰角）
		val combinedMatrix = FloatArray(16)
		Matrix.multiplyMM(combinedMatrix, 0, rotationMatrix, 0, biasMatrix, 0)

		val yawOnlyMatrix = extractYawRotation(combinedMatrix)
		camera.rotate(yawOnlyMatrix)
	}

	override fun applyTouchRotation(camera: Camera) {
		// 圆柱体模式：从基准 yaw 矩阵应用触摸增量
		if (abs(touchYawDelta) > 0.01f) {
			val deltaYawMatrix = FloatArray(16)
			Matrix.setIdentityM(deltaYawMatrix, 0)
			Matrix.rotateM(deltaYawMatrix, 0, touchYawDelta, 0f, 1f, 0f)
			val finalYawMatrix = FloatArray(16)
			Matrix.multiplyMM(finalYawMatrix, 0, touchBaseYawMatrix, 0, deltaYawMatrix, 0)
			camera.rotate(finalYawMatrix)
		} else {
			camera.rotate(touchBaseYawMatrix)
		}
	}

	override fun onTouchStart(rotationMatrix: FloatArray, biasMatrix: FloatArray) {
		val combinedMatrix = FloatArray(16)
		Matrix.multiplyMM(combinedMatrix, 0, rotationMatrix, 0, biasMatrix, 0)
		touchBaseYawMatrix = extractYawRotation(combinedMatrix)
		touchBaseYawAngle = extractYawDegrees(combinedMatrix)
		touchYawDelta = 0f
	}

	override fun onTouchMove(deltaX: Float, deltaY: Float, sensitivity: Float) {
		// 圆柱体模式：只累积水平旋转（yaw），忽略垂直旋转
		val deltaYaw = -deltaX * sensitivity
		touchYawDelta += deltaYaw
	}

	override fun onTouchEnd(rotationMatrix: FloatArray, biasMatrix: FloatArray): FloatArray {
		// 1. 计算期望的最终 yaw 角度（度）
		//    触摸中相机显示的是 rotateY(touchBaseYawAngle + touchYawDelta)
		//    松手后需要保持同一视角
		val finalYawAngleDeg = touchBaseYawAngle + touchYawDelta

		// 2. 构建传感器坐标系中的目标旋转矩阵
		//    传感器坐标系中 yaw = 绕 Z 轴旋转
		//    extractYaw(rotZ(θ)) = atan2(sin(θ), cos(θ)) = θ → rotateY(θ) ✓
		val targetSensorMatrix = FloatArray(16)
		Matrix.setIdentityM(targetSensorMatrix, 0)
		Matrix.rotateM(targetSensorMatrix, 0, finalYawAngleDeg, 0f, 0f, 1f)

		// 3. 反推 newBias = R⁻¹ × targetSensorMatrix
		//    使得 R × newBias = targetSensorMatrix
		//    gyro 模式下 extractYaw(R × newBias) = extractYaw(rotZ(θ)) = rotateY(θ) ✓
		val invRotationMatrix = FloatArray(16)
		Matrix.invertM(invRotationMatrix, 0, rotationMatrix, 0)
		val newBiasMatrix = FloatArray(16)
		Matrix.multiplyMM(newBiasMatrix, 0, invRotationMatrix, 0, targetSensorMatrix, 0)

		touchYawDelta = 0f

		return newBiasMatrix
	}

	/**
	 * 从当前渲染链路使用的旋转矩阵中提取 yaw（弧度）。
	 *
	 * 这里不能直接套用 getOrientation 的下标定义。项目中 rotationMatrix 来自
	 * SensorManager.getRotationMatrixFromVector，而后直接参与 OpenGL 的 multiplyMM。
	 * 实测在这条链路下，水平环绕角应使用 m[1]/m[0] 这组分量提取；否则会出现只能小幅摆动。
	 */
	private fun extractYawRadians(inputMatrix: FloatArray): Double {
		return atan2(inputMatrix[1].toDouble(), inputMatrix[0].toDouble())
	}

	private fun extractYawDegrees(inputMatrix: FloatArray): Float {
		return toDegrees(extractYawRadians(inputMatrix)).toFloat()
	}

	/**
	 * 从旋转矩阵中提取仅包含 yaw（绕 OpenGL Y 轴旋转）的矩阵
	 */
	private fun extractYawRotation(inputMatrix: FloatArray): FloatArray {
		val yawDeg = extractYawDegrees(inputMatrix)
		val result = FloatArray(16)
		Matrix.setIdentityM(result, 0)
		Matrix.rotateM(result, 0, yawDeg, 0f, 1f, 0f)
		return result
	}
}

