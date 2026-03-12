package cn.szu.blankxiao.panorama.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import cn.szu.blankxiao.panorama.controller.AngleOfViewController
import cn.szu.blankxiao.panorama.controller.internal.LifecycleController
import cn.szu.blankxiao.panorama.controller.internal.RotationController

/**
 * 基于陀螺仪（TYPE_ROTATION_VECTOR）的朝向提供者
 * 维护 rotationMatrix / biasMatrix，供渲染器每帧组装 View 矩阵
 *
 * @author BlankXiao
 */
class GyroOrientationProvider(
	context: Context
) : OrientationProvider, LifecycleController, SensorEventListener, AngleOfViewController {

	// 旋转矩阵 对应姿势
	private var rotationMatrix = FloatArray(16)

	// 偏移矩阵
	private var biasMatrix = FloatArray(16)

	private val sensorManager: SensorManager =
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

	private val sensor: Sensor = requireNotNull(
		sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
	) { "TYPE_ROTATION_VECTOR sensor is not available on this device" }

	private var isFirstFrame = true
	private var isGyroTrackingEnabled = true

	override fun getRotationMatrix(): FloatArray = rotationMatrix
	override fun getBiasMatrix(): FloatArray = biasMatrix

	override fun setBiasMatrix(matrix: FloatArray) {
		matrix.copyInto(biasMatrix)
	}

	override fun reCenter() {
		val invertMatrix = FloatArray(16)
		Matrix.invertM(invertMatrix, 0, rotationMatrix, 0)
		biasMatrix = invertMatrix
	}

	override fun setGyroTrackingEnabled(enabled: Boolean) {
		isGyroTrackingEnabled = enabled
	}

	override fun onAttached() {
		sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
	}

	override fun onDetached() {
		sensorManager.unregisterListener(this)
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

	/**
	 * 处理陀螺仪的关键函数
	 */
	override fun onSensorChanged(event: SensorEvent?) {
		if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return
		val rotationVectorValues = event.values.copyOf()

		if (isFirstFrame) {
			isFirstFrame = false
			initRelateMatrix(rotationVectorValues)
			return
		}

		if (!isGyroTrackingEnabled) return
		SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVectorValues)
	}

	private fun initRelateMatrix(rotationVectorValues: FloatArray) {
		// 旋转变量转为旋转矩阵 输出到rotationVectorValues
		val orientationMatrix = FloatArray(16)
		SensorManager.getRotationMatrixFromVector(orientationMatrix, rotationVectorValues)
		rotationMatrix = orientationMatrix
		// 计算逆矩阵存入biasMatrix
		val invertMatrix = FloatArray(16)
		Matrix.invertM(invertMatrix, 0, orientationMatrix, 0)
		biasMatrix = invertMatrix
	}
}
