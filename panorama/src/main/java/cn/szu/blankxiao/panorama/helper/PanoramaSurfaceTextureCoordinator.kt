package cn.szu.blankxiao.panorama.helper

import android.graphics.SurfaceTexture
import android.view.TextureView
import cn.szu.blankxiao.panorama.renderer.RenderSession

/**
 * 协调 SurfaceTexture 生命周期与渲染会话/图片加载。
 * 该类只做流程编排，不承载渲染或图片策略本身。
 */
internal class PanoramaSurfaceTextureCoordinator(
	private val renderSession: RenderSession,
	private val imageLoader: PanoramaImageLoader
) : TextureView.SurfaceTextureListener {

	override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
		// 确保从后台回来的时候只调用一次，只初始化一条 GLProducerThread
		if (!renderSession.isReady()) {
			renderSession.init(surface, width, height) {
				imageLoader.loadForFirstSurface()
			}
		} else {
			renderSession.resume(surface) {
				imageLoader.reloadForExistingSurface()
			}
		}
	}

	override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
		renderSession.resize(width, height)
	}

	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
		renderSession.pause()
		return true
	}

	override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}
