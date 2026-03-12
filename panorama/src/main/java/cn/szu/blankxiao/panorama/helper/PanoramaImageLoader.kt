package cn.szu.blankxiao.panorama.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import cn.szu.blankxiao.panorama.renderer.TextureUpdateRenderDriver
import cn.szu.blankxiao.panorama.utils.ImageUtil
import androidx.core.graphics.createBitmap

/**
 * 管理 PanoramaView 的图片来源与加载策略：
 * - 本地 Bitmap
 * - URL 缓存读取
 * - URL 网络回源
 */
class PanoramaImageLoader(
	private val context: Context,
	private val renderDriver: TextureUpdateRenderDriver,
	private val enqueueToGl: (() -> Unit) -> Unit,
	private val isRenderReady: () -> Boolean
) {
	private val placeHolder: Bitmap by lazy {
		createBitmap(1, 1).apply {
			eraseColor(Color.BLACK)
		}
	}

	private var currentBitmapUrl: String? = null
	private var localBitmap: Bitmap? = null

	fun setBitmapUrl(url: String) {
		currentBitmapUrl = url
	}

	fun setBitmap(bitmap: Bitmap) {
		localBitmap = bitmap
		currentBitmapUrl = null
		if (isRenderReady()) {
			enqueueToGl { renderDriver.loadBitmap(bitmap) }
		}
	}

	fun loadForFirstSurface() {
		val local = localBitmap
		if (local != null) {
			enqueueToGl { renderDriver.loadBitmap(local) }
			return
		}

		val url = currentBitmapUrl
		if (url == null) {
			enqueueToGl { renderDriver.loadBitmap(placeHolder) }
			return
		}

		val bitmap = ImageUtil.Companion.loadBitmapFromCache(context, url)
		if (bitmap != null) {
			enqueueToGl { renderDriver.loadBitmap(bitmap) }
		} else {
			enqueueToGl { renderDriver.loadBitmap(placeHolder) }
			ImageUtil.Companion.loadBitmapFromNetwork(context, url) { networkBitmap ->
				if (networkBitmap != null && isRenderReady()) {
					enqueueToGl { renderDriver.loadBitmap(networkBitmap) }
				}
			}
		}
	}

	fun reloadForExistingSurface() {
		val local = localBitmap
		if (local != null) {
			enqueueToGl { renderDriver.changeTextureBitmap(local) }
			return
		}

		val url = currentBitmapUrl
		if (url == null) {
			enqueueToGl { renderDriver.loadBitmap(placeHolder) }
			return
		}

		val bitmap = ImageUtil.Companion.loadBitmapFromCache(context, url)
		if (bitmap != null) {
			enqueueToGl { renderDriver.changeTextureBitmap(bitmap) }
		} else {
			enqueueToGl { renderDriver.loadBitmap(placeHolder) }
			ImageUtil.Companion.loadBitmapFromNetwork(context, url) { networkBitmap ->
				if (networkBitmap != null && isRenderReady()) {
					enqueueToGl { renderDriver.changeTextureBitmap(networkBitmap) }
				}
			}
		}
	}
}