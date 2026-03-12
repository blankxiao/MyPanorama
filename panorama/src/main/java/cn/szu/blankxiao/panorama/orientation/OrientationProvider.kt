package cn.szu.blankxiao.panorama.orientation

/**
 * 朝向数据提供者
 * 负责陀螺仪传感器 → 旋转矩阵 / 偏移矩阵，供渲染器每帧组装 View 矩阵
 *
 * @author BlankXiao
 */
interface OrientationProvider {

	/**
	 * 当前陀螺仪旋转矩阵（4×4，列主序）
	 * 由 TYPE_ROTATION_VECTOR 传感器通过 getRotationMatrixFromVector 得到
	 */
	fun getRotationMatrix(): FloatArray

	/**
	 * 当前偏移矩阵（4×4），用于 reCenter 或触摸结束后合并
	 * 渲染时通常使用 rotationMatrix × biasMatrix 作为朝向
	 */
	fun getBiasMatrix(): FloatArray

	/**
	 * 设置偏移矩阵（触摸结束时由 Renderer 将 mesher 合并结果写回）
	 */
	fun setBiasMatrix(matrix: FloatArray)

	/**
	 * 回正：将当前旋转矩阵的逆设为 biasMatrix，使当前朝向变为“正面”
	 */
	fun reCenter()

	/**
	 * 是否启用陀螺仪跟踪
	 */
	fun setGyroTrackingEnabled(enabled: Boolean)

	/**
	 * 与 View 生命周期绑定：注册传感器监听
	 */
	fun onAttached()

	/**
	 * 与 View 生命周期绑定：注销传感器监听
	 */
	fun onDetached()
}
