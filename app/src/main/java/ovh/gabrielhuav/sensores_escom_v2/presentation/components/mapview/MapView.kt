package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager

class MapView(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs),
    OnlineServerManager.WebSocketListener {

    private val renderer = MapRenderer()
    private val gestureHandler = MapGestureHandler(this)
    private val playerManager = PlayerManager()
    private val mapState = MapState()
    val mapMatrixManager = MapMatrixManager()

    private val MARGIN_FACTOR = 1.1f
    private val OFFSET_MARGIN = 80f
    private val LEFT_MARGIN: Float
        get() = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            1750f
        } else {
            50f
        }

    private var isUserInteracting = false
    private var shouldCenterOnPlayer = true

    init {
        isClickable = true
        isFocusable = true
        loadMapBitmap()
        setupGestureHandler()
    }

    private fun loadMapBitmap() {
        try {
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inMutable = true
            }
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.escom_mapa, options)
            mapState.backgroundBitmap = bitmap
            gestureHandler.setBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("MapView", "Error loading map: ${e.message}")
        }
    }

    private fun setupGestureHandler() {
        gestureHandler.initializeDetectors(context)
        gestureHandler.setCallback(object : MapGestureHandler.GestureCallback {
            override fun onOffsetChanged(offsetX: Float, offsetY: Float) {
                isUserInteracting = true
                shouldCenterOnPlayer = false

                mapState.backgroundBitmap?.let { bitmap ->
                    val scaledWidth = bitmap.width * mapState.scaleFactor
                    val scaledHeight = bitmap.height * mapState.scaleFactor

                    val minX = width - scaledWidth - OFFSET_MARGIN
                    val minY = height - scaledHeight - OFFSET_MARGIN
                    val maxX = LEFT_MARGIN
                    val maxY = OFFSET_MARGIN

                    mapState.offsetX = (mapState.offsetX + offsetX).coerceIn(minX, maxX)
                    mapState.offsetY = (mapState.offsetY + offsetY).coerceIn(minY, maxY)
                }
                invalidate()
            }

            override fun onScaleChanged(scaleFactor: Float) {
                isUserInteracting = true
                shouldCenterOnPlayer = false

                val prevScale = mapState.scaleFactor
                val minScale = calculateMinScale()
                mapState.scaleFactor = (prevScale * scaleFactor).coerceIn(minScale, minScale * 3)

                mapState.backgroundBitmap?.let { bitmap ->
                    val scaledWidth = bitmap.width * mapState.scaleFactor
                    val scaledHeight = bitmap.height * mapState.scaleFactor

                    mapState.offsetX = ((width - scaledWidth) / 2 + LEFT_MARGIN/2).coerceIn(
                        width - scaledWidth - OFFSET_MARGIN,
                        LEFT_MARGIN
                    )
                    mapState.offsetY = ((height - scaledHeight) / 2).coerceIn(
                        height - scaledHeight - OFFSET_MARGIN,
                        OFFSET_MARGIN
                    )
                }
                invalidate()
            }

            override fun invalidateView() {
                invalidate()
            }
        })
    }

    private fun centerMapOnPlayer() {
        playerManager.getLocalPlayerPosition()?.let { (playerX, playerY) ->
            mapState.backgroundBitmap?.let { bitmap ->
                val scaledWidth = bitmap.width * mapState.scaleFactor
                val scaledHeight = bitmap.height * mapState.scaleFactor

                // Calculamos el tamaño de cada celda
                val cellWidth = scaledWidth / 40f
                val cellHeight = scaledHeight / 40f

                // Calculamos la posición del jugador en píxeles
                val playerPosX = playerX * cellWidth + (cellWidth / 2)
                val playerPosY = playerY * cellHeight + (cellHeight / 2)

                // Ajustamos los offsets considerando los márgenes
                val effectiveWidth = width
                val effectiveHeight = height - (2 * OFFSET_MARGIN)

                // Calculamos el centro efectivo de la pantalla considerando los márgenes
                val centerX = LEFT_MARGIN + (effectiveWidth / 2)
                val centerY = OFFSET_MARGIN + (effectiveHeight / 2)

                // Calculamos el offset necesario para centrar al jugador
                val targetOffsetX = centerX - playerPosX
                val targetOffsetY = centerY - playerPosY

                // Aplicar límites considerando los márgenes
                val minX = width - scaledWidth - OFFSET_MARGIN
                val minY = height - scaledHeight - OFFSET_MARGIN
                val maxX = LEFT_MARGIN
                val maxY = OFFSET_MARGIN

                // Ajustamos los offsets con los límites
                mapState.offsetX = targetOffsetX.coerceIn(minX, maxX)
                mapState.offsetY = targetOffsetY.coerceIn(minY, maxY)

                shouldCenterOnPlayer = true
                invalidate()
            }
        }
    }


    private fun calculateMinScale(): Float {
        val bitmap = mapState.backgroundBitmap ?: return 1f
        val viewWidth = width.toFloat() - (OFFSET_MARGIN + LEFT_MARGIN)
        val viewHeight = height.toFloat() - (OFFSET_MARGIN * 2)
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val widthRatio = viewWidth / bitmapWidth
        val heightRatio = viewHeight / bitmapHeight

        return maxOf(widthRatio, heightRatio) * MARGIN_FACTOR
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        adjustMapToScreen()
    }

    private fun adjustMapToScreen() {
        mapState.backgroundBitmap?.let { bitmap ->
            val minScale = calculateMinScale()
            mapState.scaleFactor = minScale

            val scaledWidth = bitmap.width * mapState.scaleFactor
            val scaledHeight = bitmap.height * mapState.scaleFactor

            mapState.offsetX = ((width - scaledWidth) / 2 + LEFT_MARGIN/2).coerceIn(
                width - scaledWidth - OFFSET_MARGIN,
                LEFT_MARGIN
            )
            mapState.offsetY = ((height - scaledHeight) / 2).coerceIn(
                height - scaledHeight - OFFSET_MARGIN,
                OFFSET_MARGIN
            )

            invalidate()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        post { adjustMapToScreen() }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isUserInteracting = true
                shouldCenterOnPlayer = false
            }
            MotionEvent.ACTION_UP -> {
                isUserInteracting = false
            }
        }
        parent.requestDisallowInterceptTouchEvent(true)
        return gestureHandler.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, mapState, playerManager)
    }

    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        playerManager.updateLocalPlayerPosition(position)
        // Siempre centramos cuando hay un movimiento del jugador local
        if (position != null) {
            centerMapOnPlayer()
        }
        invalidate()
    }

    fun updateRemotePlayerPosition(playerId: String, position: Pair<Int, Int>?) {
        playerManager.updateRemotePlayerPosition(playerId, position)
        invalidate()
    }

    fun updateRemotePlayerPositions(positions: Map<String, Pair<Int, Int>>) {
        playerManager.updateRemotePlayerPositions(positions)
        invalidate()
    }

    fun removeRemotePlayer(playerId: String) {
        playerManager.removeRemotePlayer(playerId)
        invalidate()
    }

    fun isValidPosition(x: Int, y: Int): Boolean {
        return mapMatrixManager.isValidPosition(x, y)
    }

    fun getValueAt(x: Int, y: Int): Int {
        return mapMatrixManager.getValueAt(x, y)
    }

    fun isInteractivePosition(x: Int, y: Int): Boolean {
        return mapMatrixManager.isInteractivePosition(x, y)
    }
    private var isBluetoothServer = false

    override fun onMessageReceived(message: String) {
        playerManager.handleWebSocketMessage(message)
        invalidate()
    }

    fun setBluetoothServerMode(b: Boolean) {

    }
}
