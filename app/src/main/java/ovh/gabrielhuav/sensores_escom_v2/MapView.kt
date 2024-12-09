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
        style = Paint.Style.STROKE
    }
    private val paintLocalPlayer = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
    private val paintRemotePlayer = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private var offsetX = 0f
    private var offsetY = 0f
    var scaleFactor: Float = 1.0f
    private var localPlayerPosition: Pair<Int, Int>? = null
    private var remotePlayerPosition: Pair<Int, Int>? = null

    private val backgroundBitmap: Bitmap? = try {
        BitmapFactory.decodeResource(resources, R.drawable.escom_mapa)
    } catch (e: Exception) {
        null
    }

    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private val minZoom = 0.2f
    private val maxZoom = 3.0f

    private val numCellsX = 20
    private val numCellsY = 20

    private var playerOrientation: Float = 0f // Dirección de la brújula
    private var lastPlayerPosition: Pair<Int, Int>? = null // Para calcular la dirección del movimiento

    init {
        initializeDetectors()
    }

    private fun initializeDetectors() {
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!::scaleGestureDetector.isInitialized || !scaleGestureDetector.isInProgress) {
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
                var newScaleFactor = scaleFactor * detector.scaleFactor
                newScaleFactor = newScaleFactor.coerceIn(minZoom, maxZoom)

                if (newScaleFactor != scaleFactor) {
                    val focusX = detector.focusX
                    val focusY = detector.focusY

                    scaleFactor = newScaleFactor
                    offsetX += (focusX - offsetX) * (1 - detector.scaleFactor)
                    offsetY += (focusY - offsetY) * (1 - detector.scaleFactor)

                    constrainOffset()
                    invalidate()
                }

                return true
            }
        })
    }

    private fun constrainOffset() {
        if (backgroundBitmap == null) return

        val maxOffsetX = -(backgroundBitmap.width * scaleFactor - width)
        val maxOffsetY = -(backgroundBitmap.height * scaleFactor - height)

        offsetX = offsetX.coerceIn(maxOffsetX, 0f)
        offsetY = offsetY.coerceIn(maxOffsetY, 0f)
    }

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

        val cellWidth = backgroundBitmap.width / numCellsX.toFloat()
        val cellHeight = backgroundBitmap.height / numCellsY.toFloat()

        for (i in 0 until numCellsX) {
            canvas.drawLine(i * cellWidth, 0f, i * cellWidth, backgroundBitmap.height.toFloat(), paintGrid)
        }
        for (i in 0 until numCellsY) {
            canvas.drawLine(0f, i * cellHeight, backgroundBitmap.width.toFloat(), i * cellHeight, paintGrid)
        }

        localPlayerPosition?.let {
            val playerX = it.first * cellWidth + cellWidth / 2
            val playerY = it.second * cellHeight + cellHeight / 2
            canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintLocalPlayer)
        }

        remotePlayerPosition?.let {
            val playerX = it.first * cellWidth + cellWidth / 2
            val playerY = it.second * cellHeight + cellHeight / 2
            canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintRemotePlayer)
        }

        // Dibujar la brújula
        drawCompass(canvas)

        canvas.restore()
    }

    private fun drawCompass(canvas: Canvas) {
        val compassRadius = 100f
        val compassCenterX = width - 150f
        val compassCenterY = 150f

        val compassPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        canvas.drawCircle(compassCenterX, compassCenterY, compassRadius, compassPaint)

        val arrowLength = compassRadius * 0.8f
        val angleRad = Math.toRadians(playerOrientation.toDouble()).toFloat()
        val arrowEndX = compassCenterX + arrowLength * Math.cos(angleRad.toDouble()).toFloat()
        val arrowEndY = compassCenterY + arrowLength * Math.sin(angleRad.toDouble()).toFloat()

        val arrowPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 8f
        }
        canvas.drawLine(compassCenterX, compassCenterY, arrowEndX, arrowEndY, arrowPaint)
    }

    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        if (position != null && lastPlayerPosition != null) {
            val deltaX = position.first - lastPlayerPosition!!.first
            val deltaY = position.second - lastPlayerPosition!!.second

            playerOrientation = when {
                deltaX > 0 -> 0f // Este
                deltaX < 0 -> 180f // Oeste
                deltaY > 0 -> 90f // Sur
                deltaY < 0 -> 270f // Norte
                else -> playerOrientation // Sin cambio
            }
        }

        lastPlayerPosition = position
        localPlayerPosition = position
        invalidate()
    }

    fun updateRemotePlayerPosition(position: Pair<Int, Int>?) {
        remotePlayerPosition = position
        invalidate()
    }

    fun updateScaleFactor(scale: Float) {
        scaleFactor = scale.coerceIn(minZoom, maxZoom)
        constrainOffset()
        invalidate()
    }
}
