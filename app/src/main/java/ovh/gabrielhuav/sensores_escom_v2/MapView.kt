package ovh.gabrielhuav.sensores_escom_v2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat

class MapView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paintGrid = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
    }
    private val paintPlayer = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private var playerPosition: Pair<Int, Int> = Pair(0, 0)
    private var offsetX = 0f
    private var offsetY = 0f
    var scaleFactor: Float = 1.0f

    private val backgroundBitmap: Bitmap? = try {
        BitmapFactory.decodeResource(resources, R.drawable.escom_mapa)
    } catch (e: Exception) {
        null
    }

    private val gestureDetector: GestureDetectorCompat
    private lateinit var scaleGestureDetector: ScaleGestureDetector // Declaración como lateinit

    init {
        // Inicializa gestureDetector
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // Solo permite desplazamiento si no está haciendo zoom
                if (!scaleGestureDetector.isInProgress) {
                    offsetX -= distanceX / scaleFactor
                    offsetY -= distanceY / scaleFactor

                    // Limita los movimientos
                    constrainOffset()

                    invalidate()
                }
                return true
            }
        })

        // Inicializa scaleGestureDetector
        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Calcula el nuevo factor de zoom
                scaleFactor *= detector.scaleFactor

                // Limita el nivel de zoom
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)

                // Ajusta el desplazamiento para centrar en el punto del gesto
                val focusX = detector.focusX
                val focusY = detector.focusY

                offsetX += (focusX - offsetX) * (1 - detector.scaleFactor)
                offsetY += (focusY - offsetY) * (1 - detector.scaleFactor)

                constrainOffset()

                invalidate()
                return true
            }
        })
    }

    private fun constrainOffset() {
        if (backgroundBitmap == null) return

        val maxOffsetX = -(backgroundBitmap.width.toFloat() * scaleFactor - width)
        val maxOffsetY = -(backgroundBitmap.height.toFloat() * scaleFactor - height)

        offsetX = offsetX.coerceIn(maxOffsetX, 0f)
        offsetY = offsetY.coerceIn(maxOffsetY, 0f)
    }

    val cellSize: Float
        get() = backgroundBitmap?.width?.div(10f) ?: 50f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (backgroundBitmap == null) {
            canvas.drawColor(Color.RED)
            val errorPaint = Paint().apply {
                color = Color.WHITE
                textSize = 32f
            }
            canvas.drawText(
                "Error: Mapa no encontrado",
                50f,
                50f,
                errorPaint
            )
            return
        }

        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)
        canvas.translate(offsetX / scaleFactor, offsetY / scaleFactor)

        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)

        val cellWidth = cellSize
        val cellHeight = backgroundBitmap.height / 10f
        for (i in 0..10) {
            canvas.drawLine(i * cellWidth, 0f, i * cellWidth, backgroundBitmap.height.toFloat(), paintGrid)
            canvas.drawLine(0f, i * cellHeight, backgroundBitmap.width.toFloat(), i * cellHeight, paintGrid)
        }

        val playerX = playerPosition.first * cellWidth + cellWidth / 2
        val playerY = playerPosition.second * cellHeight + cellHeight / 2
        canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintPlayer)

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Maneja tanto scroll como gestos de zoom
        var handled = scaleGestureDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled || super.onTouchEvent(event)
    }

    fun updatePlayerPosition(position: Pair<Int, Int>) {
        playerPosition = position
        centerOnPlayer()
        invalidate()
    }

    fun updateScaleFactor(scale: Float) {
        scaleFactor = scale.coerceIn(0.5f, 3.0f)
        constrainOffset()
        invalidate()
    }

    private fun centerOnPlayer() {
        if (backgroundBitmap == null) return

        val cellWidth = cellSize
        val cellHeight = backgroundBitmap.height / 10f
        val playerX = playerPosition.first * cellWidth + cellWidth / 2
        val playerY = playerPosition.second * cellHeight + cellHeight / 2

        offsetX = (width / 2f - playerX * scaleFactor)
        offsetY = (height / 2f - playerY * scaleFactor)

        constrainOffset()
    }
}
