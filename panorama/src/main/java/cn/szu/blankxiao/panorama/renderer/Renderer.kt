package cn.szu.blankxiao.panorama.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import cn.szu.blankxiao.panorama.R
import cn.szu.blankxiao.panorama.orientation.GyroOrientationProvider
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.cg.mesh.MeshType
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_COLOR
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_TEXTURE_COORDINATES
import cn.szu.blankxiao.panorama.cg.mesh.PanoramaMesh.Companion.COORDINATES_PER_VERTEX
import cn.szu.blankxiao.panorama.cg.render.GLTextureRenderer
import cn.szu.blankxiao.panorama.cg.render.Shader
import cn.szu.blankxiao.panorama.cg.render.Texture
import cn.szu.blankxiao.panorama.renderer.mesher.PanoramaMesher
import cn.szu.blankxiao.panorama.renderer.rotation.DefaultRotationController
import cn.szu.blankxiao.panorama.renderer.rotation.RotationController
import cn.szu.blankxiao.panorama.utils.OpenGLUtil

/**
 * 全景渲染器
 * 负责 OpenGL ES 渲染和触摸交互；朝向数据由 [OrientationProvider] 提供（陀螺仪 + 偏移）
 * 通过 PanoramaMesher 委托形状相关的几何体和旋转逻辑
 *
 * @author BlankXiao
 */
class Renderer(private val context: Context) : GLTextureRenderer, UserInteractionRenderDriver, TextureUpdateRenderDriver {

	// 渲染目标
	private lateinit var texture: Texture

	/**
	 * 句柄
	 */
	private var vertexShaderHandle = 0
	private var fragmentShaderHandle = 0
	private var programHandle = 0

	private lateinit var camera: Camera

	/**
	 * 着色器
	 */
	private var vertexShader = Shader()
	private var fragmentShader: Shader = Shader()

	/**
	 * 当前全景 Mesher（封装几何体 + 旋转策略）
	 */
	private var mesher: PanoramaMesher = PanoramaMesher.create(MeshType.SPHERE)
	private var currentMeshType: MeshType = MeshType.SPHERE

	/**
	 * 朝向提供者（陀螺仪 → 旋转矩阵 / 偏移矩阵）
	 */
	private val rotationController: RotationController =
		DefaultRotationController(GyroOrientationProvider(context))

	// 触摸灵敏度（默认值，可通过接口调整）
	private var touchSensitivity: Float = 0.5f

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
		rotationController.updateCameraView(camera, mesher)
		renderMesh()
	}

	override fun onAttached() {
		rotationController.onAttached()
	}

	override fun onDetached() {
		rotationController.onDetached()
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


	fun reCenter() {
		rotationController.reCenter()
	}

	fun enableGyroTracking(enabled: Boolean) {
		rotationController.setGyroTrackingEnabled(enabled)
	}

	/**
	 * 开始触摸旋转
	 * 委托给 mesher 保存当前状态
	 */
	override fun startTouchRotation() {
		rotationController.startTouchRotation(mesher)
	}

	/**
	 * 应用触摸旋转增量
	 * 委托给 mesher 处理
	 * @param deltaX 水平移动距离（像素）
	 * @param deltaY 垂直移动距离（像素）
	 */
	override fun applyTouchRotation(deltaX: Float, deltaY: Float) {
		rotationController.applyTouchRotation(mesher, deltaX, deltaY, touchSensitivity)
	}

	/**
	 * 结束触摸旋转
	 * 委托给 mesher 将触摸旋转合并到 biasMatrix
	 */
	override fun endTouchRotation() {
		rotationController.endTouchRotation(mesher)
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

	fun setTouchSensitivity(sensitivity: Float) {
		touchSensitivity = sensitivity
	}

	fun getTouchSensitivity(): Float = touchSensitivity

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

	private fun renderMesh() {
		GLES20.glUseProgram(programHandle)
		bindMeshAttributes()
		bindTextureSampler()
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
			arrayOf("a_Position", "a_Color", "a_TexCoordinate")
		)
	}
}

