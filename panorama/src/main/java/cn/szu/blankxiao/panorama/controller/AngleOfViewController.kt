package cn.szu.blankxiao.panorama.controller

/**
 * 陀螺仪跟踪开关能力。
 * 与 OrientationProvider 分离，避免将控制职责混入姿态数据读取接口。
 */
interface AngleOfViewController {
	fun setGyroTrackingEnabled(enabled: Boolean)

	fun reCenter()
}