package cn.szu.blankxiao.panorama.renderer.mesher.sphere

import android.opengl.Matrix
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaRotationStrategy

/**
 * 球体旋转策略
 * 支持完整的 3D 旋转（yaw + pitch）
 *
 * @author BlankXiao
 */
class SphereRotationStrategy : PanoramaRotationStrategy {

	// 触摸旋转矩阵（累积的触摸旋转）
	private var touchRotationMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

	// 触摸旋转的初始基准矩阵（触摸开始时保存的当前旋转状态）
	private var touchBaseMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

	override fun applyGyroRotation(camera: Camera, rotationMatrix: FloatArray, biasMatrix: FloatArray) {
		// 球体模式：完整的 3D 旋转
		camera.rotate(rotationMatrix)
		camera.rotate(biasMatrix)
	}

	override fun applyTouchRotation(camera: Camera) {
		// 球体模式：基准矩阵 + 触摸旋转矩阵
		val combinedMatrix = FloatArray(16)
		Matrix.multiplyMM(combinedMatrix, 0, touchBaseMatrix, 0, touchRotationMatrix, 0)
		camera.rotate(combinedMatrix)
	}

	override fun onTouchStart(rotationMatrix: FloatArray, biasMatrix: FloatArray) {
		// 球体模式：保存完整矩阵
		Matrix.multiplyMM(touchBaseMatrix, 0, rotationMatrix, 0, biasMatrix, 0)
		Matrix.setIdentityM(touchRotationMatrix, 0)
	}

	override fun onTouchMove(deltaX: Float, deltaY: Float, sensitivity: Float) {
		// 转换为旋转角度（度）
		val deltaYaw = -deltaX * sensitivity
		val deltaPitch = -deltaY * sensitivity

		// 创建增量旋转矩阵
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

	override fun onTouchEnd(rotationMatrix: FloatArray, biasMatrix: FloatArray): FloatArray {
		// 计算触摸结束时的最终旋转矩阵
		val finalTouchMatrix = FloatArray(16)
		Matrix.multiplyMM(finalTouchMatrix, 0, touchBaseMatrix, 0, touchRotationMatrix, 0)

		// 将触摸旋转合并到 biasMatrix
		val invRotationMatrix = FloatArray(16)
		Matrix.invertM(invRotationMatrix, 0, rotationMatrix, 0)
		val newBiasMatrix = FloatArray(16)
		Matrix.multiplyMM(newBiasMatrix, 0, invRotationMatrix, 0, finalTouchMatrix, 0)

		// 重置触摸旋转矩阵
		Matrix.setIdentityM(touchRotationMatrix, 0)

		return newBiasMatrix
	}
}

