package cn.szu.blankxiao.panorama.renderer

import android.graphics.Bitmap

/**
 * 纹理更新能力约定，供图片加载模块调用。
 */
interface TextureUpdateRenderDriver {
	fun loadBitmap(bitmap: Bitmap?)
	fun changeTextureBitmap(bitmap: Bitmap?)
}
