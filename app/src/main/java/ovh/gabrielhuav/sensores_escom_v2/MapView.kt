package ovh.gabrielhuav.sensores_escom_v2

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MapView(context: Context, attrs: AttributeSet? = null) : View(context, attrs), SensorEventListener {
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
    private val paintCompassCircle = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val paintCompassArrow = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 8f
    }
    private val paintCompassText = Paint().apply {
        color = Color.RED
        textSize = 40f
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

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var orientationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var currentOrientation = 0f // Ángulo actual en grados

    private var playerDirection = 0f // Dirección relativa del jugador en el mapa

    init {
        initializeDetectors()
        orientationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
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

    private fun updatePlayerDirection(deltaX: Float, deltaY: Float) {
        // Evitar calcular la dirección si no hay movimiento
        if (deltaX == 0f && deltaY == 0f) return

        // Mapear los movimientos hacia las direcciones cardinales
        when {
            deltaY < 0 -> { // Movimiento hacia arriba (Este)
                playerDirection = 0f
            }
            deltaX > 0 -> { // Movimiento hacia la derecha (Sur)
                playerDirection = 90f
            }
            deltaY > 0 -> { // Movimiento hacia abajo (Oeste)
                playerDirection = 180f
            }
            deltaX < 0 -> { // Movimiento hacia la izquierda (Norte)
                playerDirection = 270f
            }
        }
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

        drawCompass(canvas)
    }

    private fun drawCompass(canvas: Canvas) {
        val centerX = width - 150f
        val centerY = 150f
        val radius = 100f

        // Dibujar círculo de la brújula
        canvas.drawCircle(centerX, centerY, radius, paintCompassCircle)

        // Dibujar flecha de orientación
        val arrowLength = radius - 20
        val endX = centerX + arrowLength * cos(Math.toRadians(playerDirection.toDouble())).toFloat()
        val endY = centerY + arrowLength * sin(Math.toRadians(playerDirection.toDouble())).toFloat()

        canvas.drawLine(centerX, centerY, endX, endY, paintCompassArrow)

        // Dibujar "N" en la brújula
        canvas.drawText("N", centerX - 10, centerY - radius - 10, paintCompassText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
        return handled || super.onTouchEvent(event)
    }

    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        val previousPosition = localPlayerPosition
        localPlayerPosition = position

        // Si hay una posición previa, calcular la dirección del jugador
        previousPosition?.let {
            val deltaX = (position?.first ?: 0) - it.first
            val deltaY = (position?.second ?: 0) - it.second
            updatePlayerDirection(deltaX.toFloat(), deltaY.toFloat())
        }

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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            currentOrientation = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            invalidate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sensorManager.unregisterListener(this)
    }
}

