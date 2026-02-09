package cn.szu.blankxiao.panorama.sphere

import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh
import cn.szu.blankxiao.panorama.utils.ListBuilder
import cn.szu.blankxiao.panorama.utils.OpenGLUtil
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 圆柱体模型
 * 用于圆柱形全景图的渲染，水平方向 360°，垂直方向线性
 * @author BlankXiao
 */
class Cylinder : PanoramaMesh {
    private val positions = ListBuilder<Float>()
    private val textureCoordinates = ListBuilder<Float>()
    private val colors = ListBuilder<Float>()
    private val indices = ListBuilder<Short>()

    private lateinit var positionFloats: FloatArray
    private lateinit var colorFloats: FloatArray
    private lateinit var textureFloats: FloatArray
    private lateinit var indicesShorts: ShortArray

    // PanoramaMesh 接口实现
    override lateinit var vertexBuffer: FloatBuffer
    override lateinit var colorBuffer: FloatBuffer
    override lateinit var textureCoordinateBuffer: FloatBuffer
    override lateinit var indicesBuffer: ShortBuffer
    override var indicesCount: Int = 0
        private set
    override val meshType: MeshType = MeshType.CYLINDER

    /**
     * 生成圆柱体几何数据
     * @param radius 圆柱半径
     * @param height 圆柱高度
     * @param widthSegments 水平分段数（圆周方向）
     * @param heightSegments 垂直分段数
     * @param phiStart 起始角度（弧度）
     * @param phiLength 角度范围（弧度，默认 2π 为完整圆柱）
     */
    fun generate(
        radius: Float,
        height: Float,
        widthSegments: Int,
        heightSegments: Int,
        phiStart: Double = 0.0,
        phiLength: Double = PI * 2
    ) {
        var index = 0
        val vertices = ArrayList<ArrayList<Int>>()

        // 生成顶点
        for (y in 0..heightSegments) {
            val verticesRow = ArrayList<Int>()
            val v = y / heightSegments.toFloat()

            for (x in 0..widthSegments) {
                val u = x / widthSegments.toFloat()
                val phi = phiStart + u * phiLength

                // 圆柱坐标计算
                // x = r * cos(φ)
                // y = h * (v - 0.5)  线性高度，居中于原点
                // z = r * sin(φ)
                val px = (radius * cos(phi)).toFloat()
                val py = height * (v - 0.5f)
                val pz = (radius * sin(phi)).toFloat()

                positions.add(px, py, pz)
                textureCoordinates.add(u, v)
                colors.add(u, v, u, 1f)

                verticesRow.add(index)
                index++
            }
            vertices.add(verticesRow)
        }

        // 生成三角形索引
        // 圆柱体没有极点退化问题，所有网格都生成两个三角形
        for (y in 0 until heightSegments) {
            for (x in 0 until widthSegments) {
                val v1 = vertices[y][x + 1]
                val v2 = vertices[y][x]
                val v3 = vertices[y + 1][x]
                val v4 = vertices[y + 1][x + 1]

                // 两个三角形组成一个四边形
                indices.add(v1.toShort(), v4.toShort(), v2.toShort())
                indices.add(v2.toShort(), v4.toShort(), v3.toShort())
            }
        }

        // 转换为 Buffer
        positionFloats = OpenGLUtil.toPrimitiveArray(positions.list, FloatArray::class.java)
        vertexBuffer = OpenGLUtil.floatArray2FloatBuffer(positionFloats)

        colorFloats = OpenGLUtil.toPrimitiveArray(colors.list, FloatArray::class.java)
        colorBuffer = OpenGLUtil.floatArray2FloatBuffer(colorFloats)

        textureFloats = OpenGLUtil.toPrimitiveArray(textureCoordinates.list, FloatArray::class.java)
        textureCoordinateBuffer = OpenGLUtil.floatArray2FloatBuffer(textureFloats)

        indicesShorts = OpenGLUtil.toPrimitiveArray(indices.list, ShortArray::class.java)
        indicesBuffer = OpenGLUtil.shortArray2ShortBuffer(indicesShorts)
        indicesCount = indicesShorts.size
    }

    companion object {
        /**
         * 创建默认圆柱体
         * 半径 5，高度根据 FOV 90° 计算以填满视口
         * 在距离 5（半径）处，视口高度 = 2 * 5 * tan(45°) = 10
         * 增加高度到 12 以确保没有黑边
         */
        fun getDefault(): Cylinder {
            val cylinder = Cylinder()
            cylinder.generate(
                radius = 5f,
                height = 12f,  // 增加高度以避免顶部/底部黑边
                widthSegments = 48,
                heightSegments = 24
            )
            return cylinder
        }

        /**
         * 创建自定义圆柱体
         */
        fun create(
            radius: Float = 5f,
            height: Float = 10f,
            widthSegments: Int = 48,
            heightSegments: Int = 24
        ): Cylinder {
            val cylinder = Cylinder()
            cylinder.generate(radius, height, widthSegments, heightSegments)
            return cylinder
        }
    }
}

