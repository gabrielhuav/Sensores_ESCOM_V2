package ovh.gabrielhuav.sensores_escom_v2.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager

class MapView(context: Context, attrs: AttributeSet? = null) : View(context, attrs), OnlineServerManager.WebSocketListener {

    public var mapMatrix = Array(40) { Array(40) { 2 } }.apply {
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

    // Método para configurar la matriz del edificio
    fun setBuildingMatrix() {
        mapMatrix = Array(20) { Array(20) { 2 } }.apply {
            for (i in 0 until 20) {
                for (j in 0 until 20) {
                    this[i][j] = when {
                        i == 0 || i == 19 || j == 0 || j == 19 -> 1 // Bordes del edificio
                        i % 4 == 0 && j % 4 == 0 -> 0 // Lugares interactivos del edificio
                        i % 2 == 0 && j % 2 == 0 -> 3 // Zonas inaccesibles del edificio
                        else -> 2 // Caminos libres del edificio
                    }
                }
            }
        }

        try {
            val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.escom_edificio3)
            if (originalBitmap != null) {
                backgroundBitmap = originalBitmap
                scaleBitmapToMatrix() // Ajustar el mapa al tamaño de la matriz
            } else {
                Log.e("MapView", "Error: escom_edificio3.png no encontrado.")
            }
        } catch (e: Exception) {
            Log.e("MapView", "Error al cargar el mapa del edificio: ${e.message}")
        }

        invalidate() // Redibujar la vista
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
    var localPlayerPosition: Pair<Int, Int>? = null
    private val remotePlayerPositions = mutableMapOf<String, Pair<Int, Int>>()
    var localPlayerId: String = "player_local" // Identificador único del jugador local

    private var backgroundBitmap: Bitmap? = null

    private fun scaleBitmapToMatrix() {
        if (backgroundBitmap == null) return

        val maxBitmapSize = 2048 // Tamaño máximo permitido por muchos dispositivos
        val matrixAspectRatio = mapMatrix[0].size.toFloat() / mapMatrix.size
        val bitmapAspectRatio = backgroundBitmap!!.width.toFloat() / backgroundBitmap!!.height

        val scaledWidth: Int
        val scaledHeight: Int

        if (matrixAspectRatio > bitmapAspectRatio) {
            scaledWidth = maxBitmapSize
            scaledHeight = (maxBitmapSize / matrixAspectRatio).toInt()
        } else {
            scaledWidth = (maxBitmapSize * bitmapAspectRatio).toInt()
            scaledHeight = maxBitmapSize
        }

        backgroundBitmap = Bitmap.createScaledBitmap(
            backgroundBitmap!!,
            scaledWidth.coerceAtMost(maxBitmapSize),
            scaledHeight.coerceAtMost(maxBitmapSize),
            true
        )
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (backgroundBitmap == null) {
            Log.w("MapView", "El mapa no está configurado. Asegúrese de llamar a setMapResource primero.")
            return
        }

        try {
            // Escalar el mapa al tamaño de la vista
            val scaledWidth = minOf(backgroundBitmap!!.width, w)
            val scaledHeight = minOf(backgroundBitmap!!.height, h)

            backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap!!, scaledWidth, scaledHeight, true)
        } catch (e: Exception) {
            Log.e("MapView", "Error al ajustar el mapa: ${e.message}")
        }

        invalidate() // Redibujar la vista
    }

    fun initializeMap(matrix: Array<Array<Int>>, resourceId: Int, initialPosition: Pair<Int, Int>) {
        mapMatrix = matrix
        setMapResource(resourceId)
        updateLocalPlayerPosition(initialPosition)
    }


    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private val onlineServerManager = OnlineServerManager(context)

    init {
        initializeDetectors()
        setMapResource(R.drawable.escom_mapa)
        onlineServerManager.connectToServer("ws://192.168.1.17:3000/socket")
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

        val scaledWidth = backgroundBitmap!!.width * scaleFactor
        val scaledHeight = backgroundBitmap!!.height * scaleFactor

        val maxOffsetX = -(scaledWidth - width)
        val maxOffsetY = -(scaledHeight - height)

        offsetX = offsetX.coerceIn(maxOffsetX, 0f)
        offsetY = offsetY.coerceIn(maxOffsetY, 0f)
    }

    // Paints for different cell types
    private val paintInteractive = Paint().apply { color = Color.YELLOW }
    private val paintWall = Paint().apply { color = Color.BLACK }
    private val paintPath = Paint().apply { color = Color.WHITE }
    private val paintInaccessible = Paint().apply { color = Color.DKGRAY }

    private fun drawMapMatrix(canvas: Canvas) {
        if (backgroundBitmap == null) return

        val cellWidth = backgroundBitmap!!.width / mapMatrix[0].size.toFloat()
        val cellHeight = backgroundBitmap!!.height / mapMatrix.size.toFloat()

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
    }

    override fun onDraw(canvas: Canvas) {
        val cellWidth = backgroundBitmap!!.width / mapMatrix[0].size.toFloat()
        val cellHeight = backgroundBitmap!!.height / mapMatrix.size.toFloat()
        super.onDraw(canvas)

        // Dibujar el mapa de fondo (tu código existente)
        backgroundBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        // Dibujar la matriz (tu código existente)
        if (backgroundBitmap == null) {
            canvas.drawColor(Color.RED)
            val errorPaint = Paint().apply {
                color = Color.WHITE
                textSize = 32f
            }
            canvas.drawText("Error: Mapa no cargado", 50f, 50f, errorPaint)
            return
        }

        val maxCanvasSize = 2048
        if (backgroundBitmap!!.width > maxCanvasSize || backgroundBitmap!!.height > maxCanvasSize) {
            Log.e("MapView", "Bitmap demasiado grande para el canvas.")
            return
        }

        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)
        canvas.translate(offsetX / scaleFactor, offsetY / scaleFactor)

        drawMapMatrix(canvas)
        canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)


        // Dibujar al jugador local
        localPlayerPosition?.let {
            val playerX = it.first * cellWidth + cellWidth / 2
            val playerY = it.second * cellHeight + cellHeight / 2
            canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintLocalPlayer)
        }

        // Dibujar a los jugadores remotos
        for ((id, position) in remotePlayerPositions) {
            if (id == localPlayerId) continue
            val remotePlayerX = position.first * cellWidth + cellWidth / 2
            val remotePlayerY = position.second * cellHeight + cellHeight / 2
            canvas.drawCircle(remotePlayerX, remotePlayerY, cellWidth / 4f, paintRemotePlayer)
        }

        // Dibujar al jugador Bluetooth
        bluetoothPlayerPosition?.let {
            val bluetoothX = it.first * cellWidth + cellWidth / 2
            val bluetoothY = it.second * cellHeight + cellHeight / 2
            canvas.drawCircle(bluetoothX, bluetoothY, cellWidth / 4f, paintBluetoothPlayer)
        }
    }

    private fun drawPlayers(canvas: Canvas) {
        val cellWidth = backgroundBitmap!!.width / mapMatrix[0].size.toFloat()
        val cellHeight = backgroundBitmap!!.height / mapMatrix.size.toFloat()

        // Dibujar al jugador local
        localPlayerPosition?.let {
            val playerX = it.first * cellWidth + cellWidth / 2
            val playerY = it.second * cellHeight + cellHeight / 2
            canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintLocalPlayer)
        }

        // Dibujar a los jugadores remotos
        for ((id, position) in remotePlayerPositions) {
            if (id == localPlayerId) continue // No dibujar al jugador local como remoto
            val remotePlayerX = position.first * cellWidth + cellWidth / 2
            val remotePlayerY = position.second * cellHeight + cellHeight / 2
            canvas.drawCircle(remotePlayerX, remotePlayerY, cellWidth / 4f, paintRemotePlayer)
        }
    }


    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        localPlayerPosition = position
        invalidate()
    }

    fun updateRemotePlayerPositions(positions: Map<String, Pair<Int, Int>>) {
        remotePlayerPositions.clear()
        remotePlayerPositions.putAll(positions.filterKeys { it != localPlayerId })
        invalidate() // Redibuja el mapa
    }

    fun setMapResource(resourceId: Int) {
        try {
            val originalBitmap = BitmapFactory.decodeResource(resources, resourceId)
            if (originalBitmap != null) {
                backgroundBitmap = originalBitmap
                scaleBitmapToMatrix()
            } else {
                Log.e("MapView", "Error: Mapa no encontrado en el recurso $resourceId.")
            }
        } catch (e: Exception) {
            Log.e("MapView", "Error al cargar el mapa: ${e.message}")
        }

        invalidate() // Redibujar el mapa
    }

    // Para el jugador Bluetooth
    private var isBluetoothServer = false
    private var bluetoothPlayerPosition: Pair<Int, Int>? = null
    private val paintBluetoothPlayer = Paint().apply {
        color = Color.GREEN  // Color para jugador Bluetooth
        style = Paint.Style.FILL
    }

    fun setBluetoothServerMode(isServer: Boolean) {
        isBluetoothServer = isServer
        // Cambiar color según si es servidor o cliente
        paintBluetoothPlayer.color = if (isServer) Color.YELLOW else Color.GREEN
        invalidate()
    }

    fun updateBluetoothPlayerPosition(position: Pair<Int, Int>?) {
        bluetoothPlayerPosition = position
        invalidate()
    }

    fun removeRemotePlayer(playerId: String) {
        // Eliminar al jugador remoto identificado por su ID
        remotePlayerPositions.remove(playerId)
        invalidate()
    }

    fun updateRemotePlayerPosition(playerId: String, position: Pair<Int, Int>?) {
        if (playerId != localPlayerId) { // Excluir al jugador local
            if (position != null) {
                remotePlayerPositions[playerId] = position
            } else {
                remotePlayerPositions.remove(playerId)
            }
            invalidate()
        }
    }

    override fun onMessageReceived(message: String) {
        val jsonObject = JSONObject(message)
        if (jsonObject.getString("type") == "positions") {
            val playerId = jsonObject.getString("id") // Asegúrate de que el ID del remitente esté en el JSON
            Log.d("MapView", "Mensaje recibido de $playerId: $message")

            // Enviar confirmación al remitente
            val ackMessage = JSONObject().apply {
                put("type", "ack")
                put("id", playerId)
                put("status", "received")
            }
            onlineServerManager.queueMessage(ackMessage.toString())
        }
    }


}