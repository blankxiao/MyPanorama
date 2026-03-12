package cn.szu.blankxiao.panorama.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix

/**
 * 基于陀螺仪（TYPE_ROTATION_VECTOR）的朝向提供者
 * 维护 rotationMatrix / biasMatrix，供渲染器每帧组装 View 矩阵
 *
 * @author BlankXiao
 */
class GyroOrientationProvider(private val context: Context) : OrientationProvider, SensorEventListener {

	private var rotationMatrix = floatArrayOf(
		1f, 0f, 0f, 0f,
		0f, 1f, 0f, 0f,
		0f, 0f, 1f, 0f,
		0f, 0f, 0f, 1f
	)

	private var biasMatrix = FloatArray(16)

	private val sensorManager: SensorManager =
		context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

	private val sensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!!

	private var isFirstFrame = true
	private var isGyroTrackingEnabled = true
	private var rotVecValues: FloatArray? = null
	private val rotationQuaternion = FloatArray(4)

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

	override fun onSensorChanged(event: SensorEvent?) {
		if (event?.sensor?.type != Sensor.TYPE_ROTATION_VECTOR) return

		if (isFirstFrame) {
			isFirstFrame = false
			val orientationMatrix = FloatArray(16)
			Matrix.setIdentityM(orientationMatrix, 0)

			if (rotVecValues == null) {
				rotVecValues = FloatArray(event.values.size)
			}
			for (i in rotVecValues!!.indices) {
				rotVecValues!![i] = event.values[i]
			}

			SensorManager.getQuaternionFromVector(rotationQuaternion, rotVecValues)
			SensorManager.getRotationMatrixFromVector(orientationMatrix, rotVecValues)
			rotationMatrix = orientationMatrix

			val invertMatrix = FloatArray(16)
			Matrix.invertM(invertMatrix, 0, orientationMatrix, 0)
			biasMatrix = invertMatrix
			return
		}

		if (isGyroTrackingEnabled) {
			for (i in rotVecValues?.indices!!) {
				rotVecValues?.set(i, event.values[i])
			}
			if (rotVecValues != null) {
				SensorManager.getQuaternionFromVector(rotationQuaternion, rotVecValues)
				SensorManager.getRotationMatrixFromVector(rotationMatrix, rotVecValues)
			}
		}
	}
}
