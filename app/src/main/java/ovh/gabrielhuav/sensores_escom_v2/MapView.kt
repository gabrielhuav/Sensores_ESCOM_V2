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
    interface OnMapTouchListener {
        fun onMapTouched()
    }

    private var mapTouchListener: OnMapTouchListener? = null

    fun setOnMapTouchListener(listener: OnMapTouchListener) {
        mapTouchListener = listener
    }

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

        val cellWidth = backgroundBitmap.width / 20f // Tamaño dinámico de celdas
        val cellHeight = backgroundBitmap.height / 20f
        for (i in 0..20) {
            canvas.drawLine(i * cellWidth, 0f, i * cellWidth, backgroundBitmap.height.toFloat(), paintGrid)
            canvas.drawLine(0f, i * cellHeight, backgroundBitmap.width.toFloat(), i * cellHeight, paintGrid)
        }

        // Dibujar jugador local (azul)
        localPlayerPosition?.let {
            val playerX = it.first * cellWidth + cellWidth / 2
            val playerY = it.second * cellHeight + cellHeight / 2
            canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintLocalPlayer)
        }
        // Dibujar jugador remoto (rojo)
        remotePlayerPosition?.let {
            val playerX = it.first * cellWidth + cellWidth / 2
            val playerY = it.second * cellHeight + cellHeight / 2
            canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintRemotePlayer)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event) || gestureDetector.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP) {
            val touchX = (event.x - offsetX) / scaleFactor
            val touchY = (event.y - offsetY) / scaleFactor

            val cellWidth = backgroundBitmap?.width?.div(20f) ?: 0f
            val cellHeight = backgroundBitmap?.height?.div(20f) ?: 0f

            val cellX = (touchX / cellWidth).toInt()
            val cellY = (touchY / cellHeight).toInt()

            // Lista de celdas donde se activa el QR
            val qrCells = listOf(Pair(16,4), Pair(11, 9), Pair(17, 17),Pair(2,17),Pair(5,9))

            // Verificar si la celda tocada está en las celdas donde queremos activar el QR
            if (Pair(cellX, cellY) in qrCells) {
                mapTouchListener?.onMapTouched()  //Listener del main
            }
        }
        return handled || super.onTouchEvent(event)
    }




    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        localPlayerPosition = position
        position?.let { centerMapOnPlayer(it) }
        invalidate()
    }

    fun updateRemotePlayerPosition(position: Pair<Int, Int>?) {
        remotePlayerPosition = position
        invalidate()
    }


    fun updateScaleFactor(scale: Float) {
        scaleFactor = scale.coerceIn(0.5f, 3.0f)
        constrainOffset()
        invalidate()
    }

    private fun centerMapOnPlayer(playerPosition: Pair<Int, Int>) {
        if (backgroundBitmap == null) return

        val cellWidth = backgroundBitmap.width / 20f
        val cellHeight = backgroundBitmap.height / 20f

        val playerX = playerPosition.first * cellWidth + cellWidth / 2
        val playerY = playerPosition.second * cellHeight + cellHeight / 2

        offsetX = width / 2 - playerX * scaleFactor
        offsetY = height / 2 - playerY * scaleFactor

        constrainOffset()
    }
}