package cn.szu.blankxiao.panorama.renderer.mesher

import cn.szu.blankxiao.panorama.cg.camera.Camera

/**
 * 全景旋转策略接口
 * 封装不同 MeshType 下的旋转矩阵计算逻辑（策略模式）
 *
 * @author BlankXiao
 */
interface PanoramaRotationStrategy {
	/**
	 * 陀螺仪模式下应用旋转到相机
	 * @param camera 相机对象
	 * @param rotationMatrix 陀螺仪旋转矩阵
	 * @param biasMatrix 偏移矩阵
	 */
	fun applyGyroRotation(camera: Camera, rotationMatrix: FloatArray, biasMatrix: FloatArray)

	/**
	 * 触摸模式下应用旋转到相机
	 * @param camera 相机对象
	 */
	fun applyTouchRotation(camera: Camera)

	/**
	 * 触摸开始：保存当前旋转状态作为基准
	 * @param rotationMatrix 当前陀螺仪旋转矩阵
	 * @param biasMatrix 当前偏移矩阵
	 */
	fun onTouchStart(rotationMatrix: FloatArray, biasMatrix: FloatArray)

	/**
	 * 触摸移动：累积旋转增量
	 * @param deltaX 水平移动距离（像素）
	 * @param deltaY 垂直移动距离（像素）
	 * @param sensitivity 触摸灵敏度
	 */
	fun onTouchMove(deltaX: Float, deltaY: Float, sensitivity: Float)

	/**
	 * 触摸结束：将触摸旋转合并到 biasMatrix
	 * @param rotationMatrix 当前陀螺仪旋转矩阵
	 * @param biasMatrix 当前偏移矩阵
	 * @return 合并后的新 biasMatrix
	 */
	fun onTouchEnd(rotationMatrix: FloatArray, biasMatrix: FloatArray): FloatArray
}