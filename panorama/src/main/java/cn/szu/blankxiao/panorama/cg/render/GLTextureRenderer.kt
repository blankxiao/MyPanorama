package cn.szu.blankxiao.panorama.cg.render

import android.graphics.Bitmap

/**
 * @author BlankXiao
 * @description GLTextureRenderer
 * @date 2025-10-26 22:15
 */
interface GLTextureRenderer {
	/**
	 * surface创建好
	 */
	fun onGLContextAvailable()

	/**
	 * 页面大小更改
	 */
	fun onSurfaceChanged(width: Int, height: Int)

	/**
	 * 绘制帧内容
	 */
	fun onDrawFrame()

	fun onAttached()

	fun onDetached()

	fun loadBitmap(bitmap: Bitmap?)

	fun changeTextureBitmap(bitmap: Bitmap?)
}