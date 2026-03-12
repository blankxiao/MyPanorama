package cn.szu.blankxiao.panorama.api

import android.graphics.Bitmap
import cn.szu.blankxiao.panorama.cg.mesh.MeshType

/**
 * Panorama 对外能力入口。
 * 业务层应优先面向该接口调用，避免直接依赖具体 View 实现细节。
 */
interface PanoramaController {
	fun setGyroTrackingEnabled(enabled: Boolean)
	fun reCenter()
	fun setBitmapUrl(url: String)
	fun setBitmap(bitmap: Bitmap)

	fun setMeshType(meshType: MeshType)
	fun getMeshType(): MeshType

	fun setTouchSensitivity(sensitivity: Float)
	fun getTouchSensitivity(): Float

	fun setFov(fovDegrees: Float)
	fun getFov(): Float

	var onFovChangedListener: ((Float) -> Unit)?
	var onDoubleTapListener: (() -> Unit)?
}
