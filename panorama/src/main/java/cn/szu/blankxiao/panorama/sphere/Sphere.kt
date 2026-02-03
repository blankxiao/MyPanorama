package cn.szu.blankxiao.panorama.sphere

import cn.szu.blankxiao.panorama.cg.mesh.AbstractMesh
import cn.szu.blankxiao.panorama.utils.ListBuilder
import cn.szu.blankxiao.panorama.utils.OpenGLUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author BlankXiao
 * @description Sphere
 * @date 2025-10-26 21:16
 */
class Sphere : AbstractMesh() {
	val positons = ListBuilder<Float>()
	// 纹理坐标数组
	val textureCoordinates = ListBuilder<Float>()
	val colors = ListBuilder<Float>()
	val indices = ListBuilder<Short>()

	lateinit var positionFloats: FloatArray
	lateinit var colorFloats: FloatArray
	// 纹理坐标数组
	lateinit var textureFloats: FloatArray
	// 索引数组
	lateinit var indicesShorts: ShortArray

	/**
	 * 生成球体 生成坐标/颜色/纹理数据
	 * @param radius 球体半径
	 * @param widthSegments 水平分段数
	 * @param heightSegments 垂直分段数
	 * @param phiStart 起始经度
	 * @param phiLength 经度长度
	 * @param thetaStart 起始纬度
	 * @param thetaLength 纬度长度
	 */
	fun generate(
		radius: Float, widthSegments: Int, heightSegments: Int,
		phiStart: Double, phiLength: Double, thetaStart: Double, thetaLength: Double
	) {
		val thetaEnd = thetaStart + thetaLength

		var index = 0
		// 顶点数组
		val vertices = ArrayList<ArrayList<Int>>()

		// 垂直分段(纬度)
		for (y in 0..heightSegments) {
			val verticesRow = ArrayList<Int>()
			val v = y / heightSegments.toFloat()

			// 水平分段(经度)
			for (x in 0..widthSegments) {
				val u = x / widthSegments.toFloat()
				val px =
					(radius * cos(phiStart + u * phiLength) * sin(thetaStart + v * thetaLength)).toFloat()
				val py = (radius * cos(thetaStart + v * thetaLength)).toFloat()
				val pz =
					(radius * sin(phiStart + u * phiLength) * sin(thetaStart + v * thetaLength)).toFloat()

				positons.add(px, py, pz)
				textureCoordinates.add(u, v)
				colors.add(u, v, u, 1f)

				verticesRow.add(index)
				index++
			}
			vertices.add(verticesRow)
		}

		for (y in 0 until heightSegments) {
			for (x in 0 until widthSegments) {
				val v1 = vertices[y][x + 1]
				val v2 = vertices[y][x]
				val v3 = vertices[y + 1][x]
				val v4 = vertices[y + 1][x + 1]

				if (y != 0 || thetaStart > 0) {
					indices.add(v1.toShort(), v4.toShort(), v2.toShort())
				}
				if (y != heightSegments - 1 || thetaEnd < PI) {
					indices.add(v2.toShort(), v4.toShort(), v3.toShort())
				}
			}
		}

		// 将坐标信息转换为buffer
		positionFloats = OpenGLUtil.toPrimitiveArray(positons.list, FloatArray::class.java)
		vertexBuffer = OpenGLUtil.floatArray2FloatBuffer(positionFloats)

		colorFloats = OpenGLUtil.toPrimitiveArray(colors.list, FloatArray::class.java)
		colorBuffer = OpenGLUtil.floatArray2FloatBuffer(colorFloats)

		textureFloats = OpenGLUtil.toPrimitiveArray(textureCoordinates.list, FloatArray::class.java)
		textureCoordinateBuffer = OpenGLUtil.floatArray2FloatBuffer(textureFloats)

		indicesShorts = OpenGLUtil.toPrimitiveArray(indices.list, ShortArray::class.java)
		indicesBuffer = OpenGLUtil.shortArray2ShortBuffer(indicesShorts)
	}

	companion object {
		fun getDefaultSphere(): Sphere {
			val sphere = Sphere()
			sphere.generate(
				5f,
				48, 48,
				0.0, Math.PI * 2,
				0.0, Math.PI
			)
			return sphere
		}
	}


}