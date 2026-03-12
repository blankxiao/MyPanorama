package cn.szu.blankxiao.panorama.controller.internal

/**
 * @author BlankXiao
 * @description ViewController
 * @date 2026-03-13 0:08
 */
interface LifecycleController {
	/**
	 * 与 View 生命周期绑定：注册传感器监听
	 */
	fun onAttached()

	/**
	 * 与 View 生命周期绑定：注销传感器监听
	 */
	fun onDetached()
}