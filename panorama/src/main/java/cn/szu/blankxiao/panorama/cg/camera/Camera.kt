package cn.szu.blankxiao.panorama.cg.camera

import android.opengl.Matrix
import kotlin.math.tan

/**
 * @author BlankXiao
 * @description 作为视点 用于视图转换
 * @date 2025-10-26 17:20
 */
class Camera(val fov: FOV, val screenRatio: Float) {

	// model view projection 顶点变换
	// 原三维顶点 * mvpMatrix 即为二维坐标
	// viewMatrix * projectionMatrix
	private var mvpMatrix = FloatArray(16)

	// 投影矩阵
	private var projectionMatrix = FloatArray(16)

	// 视图矩阵
	private var viewMatrix = FloatArray(16)

	constructor(screenRatio: Float) : this(FOV, screenRatio) {
		var top: Float = (tan((fov.viewAngle * Math.PI / 360.0f).toDouble()) * fov.zNear).toFloat()
		var bottom = -top
		var left = screenRatio * bottom
		var right = screenRatio * top

		// 透视矩阵
		Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, fov.zNear, fov.zFar)
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
	fun rotate(angle: Float, axis: Axis){
		when (axis){
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

	/**
	 * field of view
	 */
	companion object FOV {
		private val zNear = 0.1f
		private val zFar = 100.0f
		private val viewAngle = 90.0f
	}

}