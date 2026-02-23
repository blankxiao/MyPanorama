package cn.szu.blankxiao.panorama.renderer.mesher.cylinder

import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaRotationStrategy
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaMesher

/**
 * 圆柱体 Mesher
 * 通过 Kotlin 委托组合圆柱体几何体和圆柱体旋转策略
 *
 * @author BlankXiao
 */
class CylinderMesher(
	mesh: PanoramaMesh = Cylinder.getDefault(),
	rotation: PanoramaRotationStrategy = CylinderRotationStrategy()
) : PanoramaMesher, PanoramaMesh by mesh, PanoramaRotationStrategy by rotation

