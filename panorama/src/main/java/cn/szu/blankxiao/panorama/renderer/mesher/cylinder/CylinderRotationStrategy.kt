package cn.szu.blankxiao.panorama.renderer.mesher.cylinder

import android.opengl.Matrix
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaRotationStrategy

/**
 * 圆柱体旋转策略
 * 只允许水平旋转（yaw），不支持垂直旋转
 *
 * @author BlankXiao
 */
class CylinderRotationStrategy : PanoramaRotationStrategy {

	// 触摸开始时的 yaw 矩阵（从 rotationMatrix * biasMatrix 提取）
	private var touchBaseYawMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

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
		if (Math.abs(touchYawDelta) > 0.01f) {
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
		touchYawDelta = 0f
	}

	override fun onTouchMove(deltaX: Float, deltaY: Float, sensitivity: Float) {
		// 圆柱体模式：只累积水平旋转（yaw），忽略垂直旋转
		val deltaYaw = -deltaX * sensitivity
		touchYawDelta += deltaYaw
	}

	override fun onTouchEnd(rotationMatrix: FloatArray, biasMatrix: FloatArray): FloatArray {
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

		val currentCombined = FloatArray(16)
		Matrix.multiplyMM(currentCombined, 0, rotationMatrix, 0, biasMatrix, 0)
		val currentYawMatrix = extractYawRotation(currentCombined)

		val invCurrentYawMatrix = FloatArray(16)
		Matrix.invertM(invCurrentYawMatrix, 0, currentYawMatrix, 0)
		val deltaYawMatrix = FloatArray(16)
		Matrix.multiplyMM(deltaYawMatrix, 0, invCurrentYawMatrix, 0, finalYawMatrix, 0)

		val deltaYawRad = Math.atan2(deltaYawMatrix[1].toDouble(), deltaYawMatrix[0].toDouble())
		var deltaYaw = Math.toDegrees(deltaYawRad).toFloat()
		while (deltaYaw > 180f) deltaYaw -= 360f
		while (deltaYaw < -180f) deltaYaw += 360f

		val newBiasMatrix = if (Math.abs(deltaYaw) > 0.01f) {
			val deltaMatrix = FloatArray(16)
			Matrix.setIdentityM(deltaMatrix, 0)
			Matrix.rotateM(deltaMatrix, 0, deltaYaw, 0f, 1f, 0f)
			val result = FloatArray(16)
			Matrix.multiplyMM(result, 0, biasMatrix, 0, deltaMatrix, 0)
			result
		} else {
			biasMatrix.copyOf()
		}

		touchYawDelta = 0f

		return newBiasMatrix
	}

	/**
	 * 从旋转矩阵中提取仅包含 yaw（绕 Y 轴旋转）的矩阵
	 */
	private fun extractYawRotation(inputMatrix: FloatArray): FloatArray {
		val yaw = Math.atan2(inputMatrix[1].toDouble(), inputMatrix[0].toDouble())

		val result = FloatArray(16)
		Matrix.setIdentityM(result, 0)
		Matrix.rotateM(result, 0, Math.toDegrees(yaw).toFloat(), 0f, 1f, 0f)

		return result
	}
}

