package cn.szu.blankxiao.panorama.renderer.mesher

import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh
import cn.szu.blankxiao.panorama.renderer.mesher.cylinder.CylinderMesher
import cn.szu.blankxiao.panorama.renderer.mesher.sphere.SphereMesher

/**
 * 全景 Mesher 组合接口
 * 同时具备几何体（PanoramaMesh）和旋转策略（PanoramaRotationStrategy）的能力
 * 通过 Kotlin 委托实现，使得每种全景类型只需一个类来封装所有形状相关逻辑
 *
 * @author BlankXiao
 */
interface PanoramaMesher : PanoramaMesh, PanoramaRotationStrategy {
	companion object {
		/**
		 * 根据 MeshType 创建对应的 Mesher
		 * 整个项目中唯一的 MeshType 分支判断点
		 */
		fun create(meshType: MeshType): PanoramaMesher = when (meshType) {
			MeshType.SPHERE -> SphereMesher()
			MeshType.CYLINDER -> CylinderMesher()
		}
	}
}

