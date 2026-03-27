package ovh.gabrielhuav.sensores_escom_v2.presentation.game.basketball

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class TrajectoryView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        isAntiAlias = true
    }

    private val trajectoryPath = Path()
    private var startX = 0f
    private var startY = 0f
    private var controlX = 0f
    private var controlY = 0f
    private var endX = 0f
    private var endY = 0f
    private var isVisible = false

    fun updateTrajectory(sX: Float, sY: Float, cX: Float, cY: Float, eX: Float, eY: Float) {
        startX = sX
        startY = sY
        controlX = cX
        controlY = cY
        endX = eX
        endY = eY
        isVisible = true
        invalidate()
    }

    fun hide() {
        isVisible = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isVisible) return

        trajectoryPath.reset()
        trajectoryPath.moveTo(startX, startY)
        trajectoryPath.quadTo(controlX, controlY, endX, endY)
        canvas.drawPath(trajectoryPath, paint)
    }
}
