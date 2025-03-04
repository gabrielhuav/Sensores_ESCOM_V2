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

class MapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val mapResourceId: Int = R.drawable.escom_mapa
) : View(context, attrs), OnlineServerManager.WebSocketListener {

    companion object {
        fun createWithCustomMap(context: Context, mapResourceId: Int): MapView {
            return MapView(context, mapResourceId = mapResourceId)
        }
    }

    private val renderer = MapRenderer()
    private val gestureHandler = MapGestureHandler(this)
    val playerManager = PlayerManager()
    private val mapState = MapState()

    // Actualizamos para usar la nueva implementación de MapMatrix
    private var currentMapId = MapMatrixProvider.MAP_MAIN
    private var mapMatrix = MapMatrix(currentMapId)

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
    private var transitionListener: MapTransitionListener? = null

    // Interfaz para notificar transiciones de mapa
    interface MapTransitionListener {
        fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>)
    }

    init {
        isClickable = true
        isFocusable = true
        loadMapBitmap()
        setupGestureHandler()
    }

    // Método para establecer el listener de transición
    fun setMapTransitionListener(listener: MapTransitionListener) {
        transitionListener = listener
    }

    // Método para cambiar el mapa actual
    fun setCurrentMap(mapId: String, resourceId: Int) {
        if (currentMapId != mapId) {
            currentMapId = mapId
            mapMatrix = MapMatrix(mapId)
            // Cargar el nuevo bitmap del mapa
            loadMapBitmap(resourceId)

            // Actualizar el mapa actual en el playerManager
            playerManager.setCurrentMap(mapId)

            // Actualizar el estado visual
            post {
                adjustMapToScreen()
                invalidate()
            }

            Log.d("MapView", "Mapa cambiado a: $mapId con recurso: $resourceId")
        }
    }

    private fun loadMapBitmap(resourceId: Int = mapResourceId) {
        try {
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inMutable = true
            }
            val bitmap = BitmapFactory.decodeResource(resources, resourceId, options)
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

                    // Asegurarse de que minX <= maxX y minY <= maxY
                    val safeMinX = minOf(minX, maxX)
                    val safeMaxX = maxOf(minX, maxX)
                    val safeMinY = minOf(minY, maxY)
                    val safeMaxY = maxOf(minY, maxY)

                    mapState.offsetX = (mapState.offsetX + offsetX).coerceIn(safeMinX, safeMaxX)
                    mapState.offsetY = (mapState.offsetY + offsetY).coerceIn(safeMinY, safeMaxY)
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

                    // Asegurarse de que los rangos sean válidos
                    val minX = width - scaledWidth - OFFSET_MARGIN
                    val maxX = LEFT_MARGIN
                    val minY = height - scaledHeight - OFFSET_MARGIN
                    val maxY = OFFSET_MARGIN

                    // Asegurarse de que minX <= maxX y minY <= maxY
                    val safeMinX = minOf(minX, maxX)
                    val safeMaxX = maxOf(minX, maxX)
                    val safeMinY = minOf(minY, maxY)
                    val safeMaxY = maxOf(minY, maxY)

                    mapState.offsetX = ((width - scaledWidth) / 2 + LEFT_MARGIN/2).coerceIn(
                        safeMinX, safeMaxX
                    )
                    mapState.offsetY = ((height - scaledHeight) / 2).coerceIn(
                        safeMinY, safeMaxY
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
                val cellWidth = scaledWidth / MapMatrixProvider.MAP_WIDTH.toFloat()
                val cellHeight = scaledHeight / MapMatrixProvider.MAP_HEIGHT.toFloat()

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

                // Asegurarse de que los rangos sean válidos
                val safeMinX = minOf(minX, maxX)
                val safeMaxX = maxOf(minX, maxX)
                val safeMinY = minOf(minY, maxY)
                val safeMaxY = maxOf(minY, maxY)

                // Ajustamos los offsets con los límites
                mapState.offsetX = targetOffsetX.coerceIn(safeMinX, safeMaxX)
                mapState.offsetY = targetOffsetY.coerceIn(safeMinY, safeMaxY)

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
        // Posponemos el ajuste de la pantalla para asegurarnos de que las dimensiones ya estén actualizadas
        post {
            adjustMapToScreen()
        }
    }

    private fun adjustMapToScreen() {
        if (width <= 0 || height <= 0) {
            // No hacer nada si la vista aún no tiene dimensiones
            return
        }

        mapState.backgroundBitmap?.let { bitmap ->
            val minScale = calculateMinScale()
            mapState.scaleFactor = minScale

            val scaledWidth = bitmap.width * mapState.scaleFactor
            val scaledHeight = bitmap.height * mapState.scaleFactor

            // Calcular los límites
            val minX = width - scaledWidth - OFFSET_MARGIN
            val maxX = LEFT_MARGIN
            val minY = height - scaledHeight - OFFSET_MARGIN
            val maxY = OFFSET_MARGIN

            // Verificar y corregir rangos inválidos
            val safeMinX = minOf(minX, maxX)
            val safeMaxX = maxOf(minX, maxX)
            val safeMinY = minOf(minY, maxY)
            val safeMaxY = maxOf(minY, maxY)

            // Calcular offset centrado
            val offsetX = ((width - scaledWidth) / 2 + LEFT_MARGIN/2)
            val offsetY = ((height - scaledHeight) / 2)

            // Aplicar límites seguros
            mapState.offsetX = offsetX.coerceIn(safeMinX, safeMaxX)
            mapState.offsetY = offsetY.coerceIn(safeMinY, safeMaxY)

            invalidate()

            Log.d("MapView", "Ajustando mapa: width=$width, height=$height, " +
                    "minX=$minX, maxX=$maxX, minY=$minY, maxY=$maxY, " +
                    "safeMinX=$safeMinX, safeMaxX=$safeMaxX, safeMinY=$safeMinY, safeMaxY=$safeMaxY")
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

        // Usar la matriz del mapa actual para el dibujado
        mapState.setMapMatrix(mapMatrix)
        renderer.draw(canvas, mapState, playerManager)
    }

    // Este método ahora solo devuelve si hay una transición, no la activa
    fun checkForMapTransition(position: Pair<Int, Int>): String? {
        return mapMatrix.isMapTransitionPoint(position.first, position.second)
    }

    // Llamar a este método solo cuando el usuario presione el botón A
    fun initiateMapTransition(targetMap: String) {
        val initialPosition = MapMatrixProvider.getInitialPositionForMap(targetMap)
        transitionListener?.onMapTransitionRequested(targetMap, initialPosition)
    }

    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        playerManager.updateLocalPlayerPosition(position)

        // Ya no llamamos a checkForMapTransition aquí
        // Solo centramos el mapa en el jugador
        if (position != null) {
            centerMapOnPlayer()
        }
        invalidate()
    }

    fun updateRemotePlayerPosition(playerId: String, position: Pair<Int, Int>, map: String) {
        playerManager.updateRemotePlayerPosition(playerId, position, map)
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
        return mapMatrix.isValidPosition(x, y)
    }

    fun getValueAt(x: Int, y: Int): Int {
        return mapMatrix.getValueAt(x, y)
    }

    fun isInteractivePosition(x: Int, y: Int): Boolean {
        return mapMatrix.isInteractivePosition(x, y)
    }

    fun getMapTransitionPoint(x: Int, y: Int): String? {
        return mapMatrix.isMapTransitionPoint(x, y)
    }

    fun getCurrentMapId(): String {
        return currentMapId
    }

    private var isBluetoothServer = false

    override fun onMessageReceived(message: String) {
        playerManager.handleWebSocketMessage(message)
        invalidate()
    }

    fun setBluetoothServerMode(isServer: Boolean) {
        isBluetoothServer = isServer
    }
}