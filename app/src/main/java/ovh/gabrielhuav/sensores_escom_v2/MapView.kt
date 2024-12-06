package ovh.gabrielhuav.sensores_escom_v2

import android.app.AlertDialog
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
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    // Zonas de interés
    private val zonesOfInterest = listOf(
        Zone(1, 1, "Laboratorio", Color.BLUE),
        Zone(3, 4, "Cafetería", Color.GREEN),
        Zone(7, 8, "Auditorio", Color.YELLOW)
    )

    data class Zone(val x: Int, val y: Int, val name: String, val color: Int)

    init {
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!scaleGestureDetector.isInProgress) {
                    offsetX -= distanceX / scaleFactor
                    offsetY -= distanceY / scaleFactor

                    constrainOffset()
                    invalidate()
                }
                return true
            }
        })

        scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)

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
            canvas.drawText("Error: Mapa no encontrado", 50f, 50f, errorPaint)
            return
        }

        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)
        canvas.translate(offsetX / scaleFactor, offsetY / scaleFactor)

        // Escala el mapa para que sea más pequeño si es necesario
        val scaledBitmap = Bitmap.createScaledBitmap(
            backgroundBitmap,
            (backgroundBitmap.width * 0.8).toInt(), // Reduce el tamaño del mapa al 80%
            (backgroundBitmap.height * 0.8).toInt(),
            true
        )
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)

        val cellWidth = cellSize
        val cellHeight = scaledBitmap.height / 10f
        for (i in 0..10) {
            canvas.drawLine(i * cellWidth, 0f, i * cellWidth, scaledBitmap.height.toFloat(), paintGrid)
            canvas.drawLine(0f, i * cellHeight, scaledBitmap.width.toFloat(), i * cellHeight, paintGrid)
        }

        // Ajusta el tamaño del punto rojo
        val playerX = playerPosition.first * cellWidth + cellWidth / 2
        val playerY = playerPosition.second * cellHeight + cellHeight / 2
        canvas.drawCircle(playerX, playerY, cellWidth / 10f, paintPlayer) // Cambié a cellWidth / 6f para hacerlo más pequeño

        canvas.restore()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled || super.onTouchEvent(event)
    }

    fun updatePlayerPosition(position: Pair<Int, Int>) {
        playerPosition = position
        centerOnPlayer()
        checkZoneOfInterest()
        invalidate()
    }

    private fun checkZoneOfInterest() {
        val zone = zonesOfInterest.find { it.x == playerPosition.first && it.y == playerPosition.second }
        zone?.let {
            showZoneDialog(it)
        }
    }

    private fun showZoneDialog(zone: Zone) {
        AlertDialog.Builder(context)
            .setTitle("Zona de Interés")
            .setMessage("¡Has llegado a ${zone.name}!")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
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

    fun updateScaleFactor(scale: Float) {
        scaleFactor = scale.coerceIn(0.5f, 3.0f)
        constrainOffset()
        invalidate()
    }
}
