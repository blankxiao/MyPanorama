package cn.szu.blankxiao.panorama.cg.mesh

import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * @author BlankXiao
 * @description AbstractMesh
 * @date 2025-10-26 18:00
 */
abstract class AbstractMesh {
	// 顶点数据
	lateinit var vertexBuffer: FloatBuffer
	// 颜色数据
	lateinit var colorBuffer: FloatBuffer
	// 纹理分量数据
	lateinit var textureCoordinateBuffer: FloatBuffer
	// 索引数据
	lateinit var indicesBuffer: ShortBuffer

	companion object{
		// 顶点分量
		val COORDINATES_PER_VERTEX = 3
		// 颜色分量
		val COORDINATES_PER_COLOR = 4
		// 纹理分量 (u,v)
		val COORDINATES_PER_TEXTURE_COORDINATES = 2
	}

}