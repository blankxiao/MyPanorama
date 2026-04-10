package cn.szu.blankxiao.panorama.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * 指南针叠加层：显示当前全景朝向。
 * 指针指向「北」（yaw=0），随 yaw 旋转。
 *
 * @author BlankXiao
 */
class CompassOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 当前 yaw 角度（度），0=北，90=东，顺时针为正 */
    var yawDegrees: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                // 可能从 GL 线程调用，使用 postInvalidate 保证线程安全
                postInvalidate()
            }
        }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0x80FFFFFF.toInt()
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x40000000.toInt()
    }

    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private val northPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFE53935.toInt()
    }

    private val pointerPath = Path()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = DEFAULT_SIZE
        val w = resolveSize(size, widthMeasureSpec)
        val h = resolveSize(size, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) / 2f) - 4f

        canvas.drawCircle(cx, cy, r, fillPaint)
        canvas.drawCircle(cx, cy, r, circlePaint)

        val angleRad = Math.toRadians(yawDegrees.toDouble())
        val pointerLength = r * 0.7f
        val tipX = cx + (pointerLength * sin(angleRad)).toFloat()
        val tipY = cy - (pointerLength * cos(angleRad)).toFloat()

        pointerPath.reset()
        val halfWidth = 8f
        val backLength = 14f
        // 垂直于指针方向的单位向量（用于三角形底边）
        val perpX = (halfWidth * cos(angleRad)).toFloat()
        val perpY = (halfWidth * sin(angleRad)).toFloat()
        val backX = cx - (backLength * sin(angleRad)).toFloat()
        val backY = cy + (backLength * cos(angleRad)).toFloat()

        pointerPath.moveTo(tipX, tipY)
        pointerPath.lineTo(backX + perpX, backY + perpY)
        pointerPath.lineTo(backX - perpX, backY - perpY)
        pointerPath.close()

        canvas.drawPath(pointerPath, pointerPaint)
        canvas.drawCircle(cx, cy, 3f, northPaint)
    }

    companion object {
        private const val DEFAULT_SIZE = 128
    }
}
