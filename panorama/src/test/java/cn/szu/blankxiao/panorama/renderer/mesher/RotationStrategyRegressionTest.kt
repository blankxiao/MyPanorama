package cn.szu.blankxiao.panorama.renderer.mesher

import android.opengl.Matrix
import cn.szu.blankxiao.panorama.cg.camera.Camera
import cn.szu.blankxiao.panorama.renderer.mesher.cylinder.CylinderRotationStrategy
import cn.szu.blankxiao.panorama.renderer.mesher.sphere.SphereRotationStrategy
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RotationStrategyRegressionTest {

	@Test
	fun cylinder_touchToGyroTransition_keepsViewContinuous() {
		val strategy = CylinderRotationStrategy()
		val rotation = multiply(rotationZ(30f), rotationX(12f))
		val bias = identityMatrix()
		val deltaX = -20f
		val sensitivity = 0.5f

		// 触摸中渲染出的末帧视角
		strategy.onTouchStart(rotation, bias)
		strategy.onTouchMove(deltaX = deltaX, deltaY = 0f, sensitivity = sensitivity)
		val touchCamera = Camera(1f).apply { rebuildViewMatrix() }
		strategy.applyTouchRotation(touchCamera)
		val touchView = readCameraViewMatrix(touchCamera)

		// 松手后回写 bias，再走 gyro 渲染，视角应无跳变
		val newBias = strategy.onTouchEnd(rotation, bias)
		val gyroCamera = Camera(1f).apply { rebuildViewMatrix() }
		strategy.applyGyroRotation(gyroCamera, rotation, newBias)
		val gyroView = readCameraViewMatrix(gyroCamera)

		assertMatrixClose(gyroView, touchView, 1e-3f)
	}

	@Test
	fun cylinder_onTouchEnd_keepsExpectedYawAfterDrag() {
		val strategy = CylinderRotationStrategy()
		val rotation = rotationZ(30f)
		val bias = identityMatrix()

		strategy.onTouchStart(rotation, bias)
		// deltaYaw = -deltaX * sensitivity = -(-20) * 0.5 = +10
		strategy.onTouchMove(deltaX = -20f, deltaY = 0f, sensitivity = 0.5f)
		val newBias = strategy.onTouchEnd(rotation, bias)

		val actualCombined = multiply(rotation, newBias)
		val expectedCombined = rotationZ(40f)
		assertMatrixClose(actualCombined, expectedCombined, 1e-3f)
	}

	@Test
	fun sphere_onTouchEnd_mergesTouchDeltaIntoBiasWithStableOrder() {
		val strategy = SphereRotationStrategy()
		val rotation = multiply(rotationY(20f), rotationX(15f))
		val bias = rotationZ(5f)

		strategy.onTouchStart(rotation, bias)

		// move1: yaw +5, pitch -2.5
		strategy.onTouchMove(deltaX = -10f, deltaY = 5f, sensitivity = 0.5f)
		// move2: yaw -4, pitch +1.5
		strategy.onTouchMove(deltaX = 8f, deltaY = -3f, sensitivity = 0.5f)

		val newBias = strategy.onTouchEnd(rotation, bias)
		val actualFinal = multiply(rotation, newBias)

		val touchBase = multiply(rotation, bias)
		val touchDelta = multiply(
			sphereDeltaMatrix(deltaX = -10f, deltaY = 5f, sensitivity = 0.5f),
			sphereDeltaMatrix(deltaX = 8f, deltaY = -3f, sensitivity = 0.5f)
		)
		val expectedFinal = multiply(touchBase, touchDelta)

		assertMatrixClose(actualFinal, expectedFinal, 1e-3f)
	}

	private fun identityMatrix(): FloatArray = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

	private fun rotationX(deg: Float): FloatArray = identityMatrix().also {
		Matrix.rotateM(it, 0, deg, 1f, 0f, 0f)
	}

	private fun rotationY(deg: Float): FloatArray = identityMatrix().also {
		Matrix.rotateM(it, 0, deg, 0f, 1f, 0f)
	}

	private fun rotationZ(deg: Float): FloatArray = identityMatrix().also {
		Matrix.rotateM(it, 0, deg, 0f, 0f, 1f)
	}

	private fun multiply(left: FloatArray, right: FloatArray): FloatArray {
		val out = FloatArray(16)
		Matrix.multiplyMM(out, 0, left, 0, right, 0)
		return out
	}

	private fun sphereDeltaMatrix(deltaX: Float, deltaY: Float, sensitivity: Float): FloatArray {
		val deltaYaw = -deltaX * sensitivity
		val deltaPitch = -deltaY * sensitivity
		val delta = identityMatrix()

		if (deltaYaw != 0f) {
			Matrix.rotateM(delta, 0, deltaYaw, 0f, 1f, 0f)
		}
		if (deltaPitch != 0f) {
			val pitch = identityMatrix()
			Matrix.rotateM(pitch, 0, deltaPitch, 1f, 0f, 0f)
			Matrix.multiplyMM(delta, 0, delta, 0, pitch, 0)
		}
		return delta
	}

	private fun readCameraViewMatrix(camera: Camera): FloatArray {
		val field = Camera::class.java.getDeclaredField("viewMatrix")
		field.isAccessible = true
		return (field.get(camera) as FloatArray).copyOf()
	}

	private fun assertMatrixClose(actual: FloatArray, expected: FloatArray, epsilon: Float) {
		for (i in actual.indices) {
			val diff = abs(actual[i] - expected[i])
			assertTrue("matrix[$i] diff=$diff actual=${actual[i]} expected=${expected[i]}", diff <= epsilon)
		}
	}
}
