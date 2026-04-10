package cn.szu.blankxiao.panorama.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.Matrix
import kotlin.math.atan2
import cn.szu.blankxiao.panorama.R
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_COLOR
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_TEXTURE_COORDINATES
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_VERTEX
import cn.szu.blankxiao.panorama.cg.render.GLTextureRenderer
import cn.szu.blankxiao.panorama.cg.render.Shader
import cn.szu.blankxiao.panorama.cg.render.Texture
import cn.szu.blankxiao.panorama.controller.AngleOfViewController
import cn.szu.blankxiao.panorama.controller.CameraController
import cn.szu.blankxiao.panorama.controller.internal.LifecycleController
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaMesher
import cn.szu.blankxiao.panorama.controller.internal.RotationController
import cn.szu.blankxiao.panorama.controller.internal.TouchRotationController
import cn.szu.blankxiao.panorama.utils.OpenGLUtil

/**
 * 全景渲染器
 * 负责 OpenGL ES 渲染和触摸交互；朝向数据由 [OrientationProvider] 提供（陀螺仪 + 偏移）
 * 通过 PanoramaMesher 委托形状相关的几何体和旋转逻辑
 *
 * @author BlankXiao
 */
class Renderer(
	private val context: Context,
	private val rotationController: RotationController,
	private var onYawChanged: ((Float) -> Unit)? = null
) : GLTextureRenderer,
	CameraController,
	TextureUpdateRenderDriver,
	AngleOfViewController by rotationController,
	LifecycleController by rotationController,
	TouchRotationController by rotationController
{

	// 渲染目标
	// TextureView 作为openGL渲染内容的显示载体 是当前Frame布局的唯一子view
	// 内部使用了SurfaceTexture 是opengl的直接渲染目标
	private lateinit var texture: Texture

	/**
	 * 句柄
	 */
	private var vertexShaderHandle = 0
	private var fragmentShaderHandle = 0
	private var programHandle = 0

	// 相机
	private lateinit var camera: Camera

	// 顶点着色器
	private var vertexShader = Shader()
	// 片段着色器
	private var fragmentShader: Shader = Shader()

	/**
	 * 当前全景 Mesher（封装几何体 + 旋转策略）
	 */
	private var mesher: PanoramaMesher = PanoramaMesher.create(MeshType.SPHERE)
	private var currentMeshType: MeshType = MeshType.SPHERE

	private var frameCountForYaw = 0

	override fun onGLContextAvailable() {
		// 注册绑定纹理
		texture = Texture()
		texture.create()

		compileShaders()
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
	}

	override fun onSurfaceChanged(width: Int, height: Int) {
		GLES20.glViewport(0, 0, width, height)
		val ratio = width.toFloat() / height
		camera = Camera(ratio)
	}

	override fun onDrawFrame() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
		// 重建相机朝向
		rotationController.updateCameraView(camera)
		// 刷新页面
		renderMesh()

		// 指南针：每 4 帧提取一次 yaw，减少主线程压力
		onYawChanged?.let { callback ->
			if (++frameCountForYaw % 4 == 0) {
				val v = camera.getViewMatrix()
				// 从 view 矩阵提取相机朝向：第三行 (v[2],v[6],v[10]) 为 view -Z 在世界坐标的方向
				val forwardX = v[2]
				val forwardZ = v[10]
				val yawRad = atan2(forwardX, forwardZ)
				val yawDegrees = Math.toDegrees(yawRad.toDouble()).toFloat()
				callback(yawDegrees)
			}
		}
	}

	override fun loadBitmap(bitmap: Bitmap?) {
		texture.loadBitmapToGLTexture(bitmap)
	}

	override fun changeTextureBitmap(bitmap: Bitmap?) {
		texture.destroy()
		texture.create()
		renderMesh()
		texture.loadBitmapToGLTexture(bitmap)
	}

	/**
	 * 设置全景图模型类型
	 * 通过 PanoramaMesher 工厂方法一次性切换几何体和旋转策略
	 * @param meshType 模型类型（SPHERE 或 CYLINDER）
	 */
	fun setMeshType(meshType: MeshType) {
		if (currentMeshType != meshType) {
			currentMeshType = meshType
			mesher = PanoramaMesher.create(meshType)
		}
	}

	/**
	 * 获取当前模型类型
	 */
	fun getMeshType(): MeshType = currentMeshType

	internal fun getMesher(): PanoramaMesher = mesher

	/**
	 * 设置 FOV（视场角）
	 * @param fovDegrees 视场角（度），范围 [Camera.MIN_FOV, Camera.MAX_FOV]
	 */
	override fun setFov(fovDegrees: Float) {
		if (::camera.isInitialized) {
			camera.updateProjectionMatrix(fovDegrees)
		}
	}

	/**
	 * 获取当前 FOV
	 */
	override fun getFov(): Float = if (::camera.isInitialized) camera.currentFov else Camera.DEFAULT_FOV

	/**
	 * 绑定gl的各个属性
	 */
	private fun renderMesh() {
		// 激活当前的shader program
		GLES20.glUseProgram(programHandle)
		// 绑定顶点属性
		bindMeshAttributes()
		// 绑定纹理属性
		bindTextureSampler()
		// 绑定mvp矩阵
		bindMvpMatrix()
		drawMesh()
		vertexShader.disableAllAttrbHandle()
	}

	private fun bindMeshAttributes() {
		vertexShader.bindVertexBuffer(
			programHandle,
			"a_Position",
			COORDINATES_PER_VERTEX,
			mesher.vertexBuffer
		)
		vertexShader.bindColorBuffer(
			programHandle,
			"a_Color",
			COORDINATES_PER_COLOR,
			mesher.colorBuffer
		)

		vertexShader.bindTextureCoordinatesBuffer(
			programHandle,
			"a_TextureCoordinates",
			COORDINATES_PER_TEXTURE_COORDINATES,
			mesher.textureCoordinateBuffer
		)
	}

	private fun bindTextureSampler() {
		fragmentShader.bindTextureSampler2D(programHandle, "u_Texture", texture.textureName)
		texture.bindSampler(fragmentShader.getTextureSamplerHandle())
	}

	private fun bindMvpMatrix() {
		vertexShader.bindMVPMatrix(programHandle, "u_MVPMatrix", camera.getMVPMatrix())
	}

	private fun drawMesh() {
		GLES20.glDrawElements(
			GLES20.GL_TRIANGLES,
			mesher.indicesCount,
			GLES20.GL_UNSIGNED_SHORT,
			mesher.indicesBuffer
		)
	}

	private fun compileShaders() {
		vertexShaderHandle = OpenGLUtil.loadAndCompileShader(
			context,
			R.raw.sphere_vertex_shader,
			GLES20.GL_VERTEX_SHADER
		)
		fragmentShaderHandle = OpenGLUtil.loadAndCompileShader(
			context,
			R.raw.sphere_fragment_shader,
			GLES20.GL_FRAGMENT_SHADER
		)
		programHandle = OpenGLUtil.createAndLinkProgram(
			vertexShaderHandle, fragmentShaderHandle,
			arrayOf("a_Position", "a_Color", "a_TextureCoordinates")
		)
	}
}

