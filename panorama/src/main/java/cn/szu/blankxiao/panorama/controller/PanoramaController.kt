package cn.szu.blankxiao.panorama.controller

import android.graphics.Bitmap
import cn.szu.blankxiao.panorama.cg.mesh.MeshType

/**
 * Panorama 对外能力入口。
 * 业务层应优先面向该接口调用，避免直接依赖具体 View 实现细节。
 */
interface PanoramaController: AngleOfViewController, CameraController {
	fun setBitmapUrl(url: String)
	fun setBitmap(bitmap: Bitmap)

	fun setMeshType(meshType: MeshType)
	fun getMeshType(): MeshType

	fun setTouchSensitivity(sensitivity: Float)
	fun getTouchSensitivity(): Float

	var onFovChangedListener: ((Float) -> Unit)?
	var onDoubleTapListener: (() -> Unit)?
}
