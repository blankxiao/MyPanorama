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
 * 球体模型
 * 360° x 180° 视角
 * @author BlankXiao
 */
class Sphere : PanoramaMesh {
	private val positions = ListBuilder<Float>()
	// 纹理坐标数组
	private val textureCoordinatesData = ListBuilder<Float>()
	private val colorsData = ListBuilder<Float>()
	private val indicesData = ListBuilder<Short>()

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
	override val meshType: MeshType = MeshType.SPHERE

	/**
	 * 生成球体几何数据
	 * @param radius 球体半径
	 * @param widthSegments 水平分段数（经度方向）
	 * @param heightSegments 垂直分段数（纬度方向）
	 * @param phiStart 起始经度（弧度）
	 * @param phiLength 经度范围（弧度）
	 * @param thetaStart 起始纬度（弧度）
	 * @param thetaLength 纬度范围（弧度）
	 */
	fun generate(
		radius: Float, widthSegments: Int, heightSegments: Int,
		phiStart: Double, phiLength: Double, thetaStart: Double, thetaLength: Double
	) {
		val thetaEnd = thetaStart + thetaLength

		var index = 0
		val vertices = ArrayList<ArrayList<Int>>()

		// 垂直分段(纬度)
		for (y in 0..heightSegments) {
			val verticesRow = ArrayList<Int>()
			val v = y / heightSegments.toFloat()

			// 水平分段(经度)
			for (x in 0..widthSegments) {
				val u = x / widthSegments.toFloat()
				
				// 球坐标 → 笛卡尔坐标
				val px = (radius * cos(phiStart + u * phiLength) * sin(thetaStart + v * thetaLength)).toFloat()
				val py = (radius * cos(thetaStart + v * thetaLength)).toFloat()
				val pz = (radius * sin(phiStart + u * phiLength) * sin(thetaStart + v * thetaLength)).toFloat()

				positions.add(px, py, pz)
				textureCoordinatesData.add(u, v)
				colorsData.add(u, v, u, 1f)

				verticesRow.add(index)
				index++
			}
			vertices.add(verticesRow)
		}

		// 生成三角形索引（处理极点退化）
		for (y in 0 until heightSegments) {
			for (x in 0 until widthSegments) {
				val v1 = vertices[y][x + 1]
				val v2 = vertices[y][x]
				val v3 = vertices[y + 1][x]
				val v4 = vertices[y + 1][x + 1]

				// 避免北极点退化三角形
				if (y != 0 || thetaStart > 0) {
					indicesData.add(v1.toShort(), v4.toShort(), v2.toShort())
				}
				// 避免南极点退化三角形
				if (y != heightSegments - 1 || thetaEnd < PI) {
					indicesData.add(v2.toShort(), v4.toShort(), v3.toShort())
				}
			}
		}

		// 转换为 Buffer
		positionFloats = OpenGLUtil.toPrimitiveArray(positions.list, FloatArray::class.java)
		vertexBuffer = OpenGLUtil.floatArray2FloatBuffer(positionFloats)

		colorFloats = OpenGLUtil.toPrimitiveArray(colorsData.list, FloatArray::class.java)
		colorBuffer = OpenGLUtil.floatArray2FloatBuffer(colorFloats)

		textureFloats = OpenGLUtil.toPrimitiveArray(textureCoordinatesData.list, FloatArray::class.java)
		textureCoordinateBuffer = OpenGLUtil.floatArray2FloatBuffer(textureFloats)

		indicesShorts = OpenGLUtil.toPrimitiveArray(indicesData.list, ShortArray::class.java)
		indicesBuffer = OpenGLUtil.shortArray2ShortBuffer(indicesShorts)
		indicesCount = indicesShorts.size
	}

	companion object {
		/**
		 * 创建默认球体
		 * 半径 5，48x48 分段，完整球面
		 */
		fun getDefault(): Sphere {
			val sphere = Sphere()
			sphere.generate(
				5f,
				48, 48,
				0.0, Math.PI * 2,
				0.0, Math.PI
			)
			return sphere
		}

		/**
		 * 创建自定义球体
		 */
		fun create(
			radius: Float = 5f,
			widthSegments: Int = 48,
			heightSegments: Int = 48
		): Sphere {
			val sphere = Sphere()
			sphere.generate(
				radius,
				widthSegments, heightSegments,
				0.0, Math.PI * 2,
				0.0, Math.PI
			)
			return sphere
		}
	}


}