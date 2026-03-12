package cn.szu.blankxiao.panorama.controller.internal

import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.controller.AngleOfViewController
import cn.szu.blankxiao.panorama.controller.internal.TouchRotationController

/**
 * 旋转控制器
 * 负责触摸状态管理、朝向数据接入以及相机视图矩阵更新
 */
interface RotationController : AngleOfViewController, LifecycleController, TouchRotationController {
	/**
	 * 调整视图矩阵
	 */
	fun updateCameraView(camera: Camera)
}