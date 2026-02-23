package cn.szu.blankxiao.panorama.renderer.mesher.sphere

import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaRotationStrategy
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaMesher

/**
 * 球体 Mesher
 * 通过 Kotlin 委托组合球体几何体和球体旋转策略
 *
 * @author BlankXiao
 */
class SphereMesher(
	mesh: PanoramaMesh = Sphere.getDefault(),
	rotation: PanoramaRotationStrategy = SphereRotationStrategy()
) : PanoramaMesher, PanoramaMesh by mesh, PanoramaRotationStrategy by rotation

