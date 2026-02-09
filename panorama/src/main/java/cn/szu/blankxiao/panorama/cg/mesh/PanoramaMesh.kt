package cn.szu.blankxiao.panorama.cg.mesh

import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * 全景图几何体接口
 * 定义了所有全景图模型（球体、圆柱体等）需要实现的通用接口
 * @author BlankXiao
 */
interface PanoramaMesh {
    /**
     * 顶点缓冲区
     */
    val vertexBuffer: FloatBuffer

    /**
     * 颜色缓冲区
     */
    val colorBuffer: FloatBuffer

    /**
     * 纹理坐标缓冲区
     */
    val textureCoordinateBuffer: FloatBuffer

    /**
     * 索引缓冲区
     */
    val indicesBuffer: ShortBuffer

    /**
     * 索引数量（用于 glDrawElements）
     */
    val indicesCount: Int

    /**
     * 模型类型
     */
    val meshType: MeshType

    companion object {
        // 顶点分量 (x, y, z)
        const val COORDINATES_PER_VERTEX = 3

        // 颜色分量 (r, g, b, a)
        const val COORDINATES_PER_COLOR = 4

        // 纹理分量 (u, v)
        const val COORDINATES_PER_TEXTURE_COORDINATES = 2
    }
}

