package ovh.gabrielhuav.sensores_escom_v2.presentation.components

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
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager

class MapView(context: Context, attrs: AttributeSet? = null) : View(context, attrs), OnlineServerManager.WebSocketListener {

    public val mapMatrix = Array(40) { Array(40) { 2 } }.apply {
    for (i in 0 until 40) {
        for (j in 0 until 40) {
            this[i][j] = when {
                i == 0 || i == 39 || j == 0 || j == 39 -> 1 // Bordes
                i % 5 == 0 && j % 5 == 0 -> 0 // Lugares interactivos
                i % 3 == 0 && j % 3 == 0 -> 3 // Zonas inaccesibles
                else -> 2 // Camino libre
            }
        }
    }
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
    private val remotePlayerPositions = mutableMapOf<String, Pair<Int, Int>>()

    private val backgroundBitmap: Bitmap? = try {
        BitmapFactory.decodeResource(resources, R.drawable.escom_mapa)
    } catch (e: Exception) {
        null
    }

    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private val onlineServerManager = OnlineServerManager(this)

    init {
        initializeDetectors()
        onlineServerManager.connectToServer("ws://example.com/socket") // Replace with your server URL
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

    // Paints for different cell types
    private val paintInteractive = Paint().apply { color = Color.YELLOW }
    private val paintWall = Paint().apply { color = Color.BLACK }
    private val paintPath = Paint().apply { color = Color.WHITE }
    private val paintInaccessible = Paint().apply { color = Color.DKGRAY }

   override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawColor(Color.WHITE)

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

    val cellWidth = backgroundBitmap.width / 40f
    val cellHeight = backgroundBitmap.height / 40f

    for (i in mapMatrix.indices) {
        for (j in mapMatrix[i].indices) {
            val paint = when (mapMatrix[i][j]) {
                0 -> paintInteractive
                1 -> paintWall
                2 -> paintPath
                3 -> paintInaccessible
                else -> paintPath
            }
            canvas.drawRect(
                j * cellWidth,
                i * cellHeight,
                (j + 1) * cellWidth,
                (i + 1) * cellHeight,
                paint
            )
        }
    }

    // Draw grid lines
    for (i in 0..40) {
        canvas.drawLine(i * cellWidth, 0f, i * cellWidth, backgroundBitmap.height.toFloat(), paintGrid)
        canvas.drawLine(0f, i * cellHeight, backgroundBitmap.width.toFloat(), i * cellHeight, paintGrid)
    }

    // Draw local player
    localPlayerPosition?.let {
        val playerX = it.first * cellWidth + cellWidth / 2
        val playerY = it.second * cellHeight + cellHeight / 2
        canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintLocalPlayer)
    }

    // Draw remote players
    remotePlayerPositions.values.forEach { position ->
        val playerX = position.first * cellWidth + cellWidth / 2
        val playerY = position.second * cellHeight + cellHeight / 2
        canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintRemotePlayer)
    }

    canvas.restore()
}


    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        handled = gestureDetector.onTouchEvent(event) || handled
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

    fun updateRemotePlayerPositions(positions: Map<String, Pair<Int, Int>>) {
        remotePlayerPositions.clear()
        remotePlayerPositions.putAll(positions)
        invalidate()
    }

    fun removeRemotePlayer(playerId: String) {
        remotePlayerPositions.remove(playerId)
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

    override fun onMessageReceived(message: String) {
        val jsonObject = JSONObject(message)
        if (jsonObject.getString("type") == "positions") {
            val players = jsonObject.getJSONObject("players")
            val positions = mutableMapOf<String, Pair<Int, Int>>()
            players.keys().forEach { playerId ->
                val position = players.getJSONObject(playerId)
                val x = position.getInt("x")
                val y = position.getInt("y")
                positions[playerId] = Pair(x, y)
            }
            updateRemotePlayerPositions(positions)
        }
    }
}