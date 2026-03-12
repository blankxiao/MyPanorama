package cn.szu.blankxiao.panorama.controller

/**
 * @author BlankXiao
 * @description CameraController
 * @date 2026-03-13 1:03
 */
interface CameraController {
	fun getFov(): Float
	fun setFov(fovDegrees: Float)

}