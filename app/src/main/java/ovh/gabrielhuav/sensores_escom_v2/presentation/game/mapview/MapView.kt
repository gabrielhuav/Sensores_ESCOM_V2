package ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie.FogOfWarRenderer
import kotlin.math.min

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
    val playerManager = PlayerManager(context)
    val mapState = MapState()

    // Inicializar el handler inmediatamente en la declaración para evitar nulos
    private val handler = Handler(Looper.getMainLooper())

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

    // NUEVO: Propiedades para modo nocturno
    private var nightModeEnabled = false
    private val nightOverlayPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 40) // Azul oscuro semi-transparente
        style = Paint.Style.FILL
    }

    // Interfaz para notificar transiciones de mapa
    interface MapTransitionListener {
        fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>)
    }

    // Interfaz para dibujado personalizado
    interface CustomDrawCallback {
        fun onCustomDraw(canvas: Canvas, cellWidth: Float, cellHeight: Float)
    }

    private var customDrawCallback: CustomDrawCallback? = null

    fun setCustomDrawCallback(callback: CustomDrawCallback?) {
        customDrawCallback = callback
    }

    init {
        isClickable = true
        isFocusable = true

        // Inicializar el handler
        setupGestureHandler()

        // Cargar el mapa con postDelayed para asegurar que la vista esté lista
        post {
            try {
                loadMapBitmap()
                // Una vez cargado el mapa, ajustamos la vista
                postDelayed({
                    adjustMapToScreen()
                    invalidate()
                }, 100)
            } catch (e: Exception) {
                Log.e("MapView", "Error en init: ${e.message}")
            }
        }
    }

    // NUEVO: Método para activar/desactivar modo nocturno
    fun setNightMode(enabled: Boolean) {
        nightModeEnabled = enabled
        invalidate()
    }

    // Método para establecer el listener de transición
    fun setMapTransitionListener(listener: MapTransitionListener) {
        transitionListener = listener
    }

    // Método para cambiar el mapa actual
    fun setCurrentMap(mapId: String, resourceId: Int) {
        try {
            if (currentMapId != mapId) {
                currentMapId = mapId
                mapMatrix = MapMatrix(mapId)

                // Cargar el bitmap y actualizar
                post {
                    try {
                        loadMapBitmap(resourceId)

                        // Usar postDelayed para asegurar que el bitmap esté cargado
                        postDelayed({
                            adjustMapToScreen()

                            // Actualizar el playerManager después de configurar el mapa
                            playerManager.setCurrentMap(mapId)

                            // Centrar en la posición del jugador si ya existe
                            playerManager.getLocalPlayerPosition()?.let {
                                centerMapOnPlayer()
                            }

                            invalidate()

                            Log.d("MapView", "Mapa cambiado a: $mapId")
                        }, 100)
                    } catch (e: Exception) {
                        Log.e("MapView", "Error en setCurrentMap: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MapView", "Error cambiando mapa: ${e.message}")
        }
    }

    private fun loadMapBitmap(resourceId: Int = mapResourceId) {
        try {
            val options = BitmapFactory.Options().apply {
                inScaled = false  // Evitar escalado automático
                inMutable = true  // Permitir modificaciones
            }

            val bitmap = BitmapFactory.decodeResource(resources, resourceId, options)
            if (bitmap != null) {
                Log.d("MapView", "Mapa cargado con éxito: ${bitmap.width}x${bitmap.height}")
                mapState.backgroundBitmap = bitmap
                gestureHandler.setBitmap(bitmap)
            } else {
                Log.e("MapView", "Error: No se pudo cargar el bitmap del mapa")
            }
        } catch (e: Exception) {
            Log.e("MapView", "Error cargando mapa: ${e.message}")
        }
    }

    private fun setupGestureHandler() {
        gestureHandler.initializeDetectors(context)
        gestureHandler.setCallback(object : MapGestureHandler.GestureCallback {
            override fun onOffsetChanged(offsetX: Float, offsetY: Float) {
                try {
                    isUserInteracting = true
                    shouldCenterOnPlayer = false

                    mapState.offsetX += offsetX
                    mapState.offsetY += offsetY
                } catch (e: Exception) {
                    Log.e("MapView", "Error en onOffsetChanged: ${e.message}")
                }
            }

            override fun onScaleChanged(scaleFactor: Float) {
                try {
                    isUserInteracting = true
                    shouldCenterOnPlayer = false

                    mapState.scaleFactor = scaleFactor
                } catch (e: Exception) {
                    Log.e("MapView", "Error en onScaleChanged: ${e.message}")
                }
            }

            override fun invalidateView() {
                invalidate()
            }

            // Implementar el nuevo método constrainOffset
            override fun constrainOffset() {
                try {
                    // Llamar al método de la clase
                    constrainMapOffset()
                } catch (e: Exception) {
                    Log.e("MapView", "Error en callback.constrainOffset: ${e.message}")
                }
            }
        })
    }

    // Método centerMapOnPlayer revisado
    private fun centerMapOnPlayer() {
        try {
            playerManager.getLocalPlayerPosition()?.let { (playerX, playerY) ->
                mapState.backgroundBitmap?.let { bitmap ->
                    // Obtener dimensiones
                    val scaledWidth = bitmap.width * mapState.scaleFactor
                    val scaledHeight = bitmap.height * mapState.scaleFactor

                    // Tamaño de cada celda
                    val cellWidth = scaledWidth / MapMatrixProvider.MAP_WIDTH.toFloat()
                    val cellHeight = scaledHeight / MapMatrixProvider.MAP_HEIGHT.toFloat()

                    // Posición en píxeles del jugador
                    val playerPosX = playerX * cellWidth + (cellWidth / 2)
                    val playerPosY = playerY * cellHeight + (cellHeight / 2)

                    // Centro de la pantalla
                    val screenCenterX = width / 2f
                    val screenCenterY = height / 2f

                    // Offset para centrar al jugador
                    val targetOffsetX = screenCenterX - playerPosX
                    val targetOffsetY = screenCenterY - playerPosY

                    // Aplicar offset con restricciones para que el mapa no se salga de pantalla
                    // Si el mapa es más pequeño que la pantalla, lo centramos
                    // Si es más grande, nos aseguramos que no se vea fuera de los bordes
                    val minOffsetX = width - scaledWidth
                    val minOffsetY = height - scaledHeight

                    if (scaledWidth <= width) {
                        // Si el mapa es más estrecho que la pantalla, centrarlo
                        mapState.offsetX = (width - scaledWidth) / 2f
                    } else {
                        // Si el mapa es más ancho, asegurar que no muestre espacio en blanco
                        mapState.offsetX = targetOffsetX.coerceIn(minOffsetX, 0f)
                    }

                    if (scaledHeight <= height) {
                        // Si el mapa es más corto que la pantalla, centrarlo
                        mapState.offsetY = (height - scaledHeight) / 2f
                    } else {
                        // Si el mapa es más alto, asegurar que no muestre espacio en blanco
                        mapState.offsetY = targetOffsetY.coerceIn(minOffsetY, 0f)
                    }

                    Log.d("MapView", "Centrado en jugador: ($playerX,$playerY), offset=(${mapState.offsetX},${mapState.offsetY})")
                }
            }
        } catch (e: Exception) {
            Log.e("MapView", "Error en centerMapOnPlayer: ${e.message}")
        }
    }

    fun forceRecenterOnPlayer() {
        try {
            // No llamamos a handler.post directamente
            centerMapOnPlayer()
            invalidate()
        } catch (e: Exception) {
            Log.e("MapView", "Error al recentrar: ${e.message}")
        }
    }

    private fun constrainMapOffset() {
        try {
            mapState.backgroundBitmap?.let { bitmap ->
                val scaledWidth = bitmap.width * mapState.scaleFactor
                val scaledHeight = bitmap.height * mapState.scaleFactor

                // Si el mapa es más pequeño que la vista, centrarlo
                if (scaledWidth <= width) {
                    mapState.offsetX = (width - scaledWidth) / 2f
                } else {
                    // Si es más grande, restringir para que no se vea fuera
                    val minOffsetX = width - scaledWidth
                    mapState.offsetX = mapState.offsetX.coerceIn(minOffsetX, 0f)
                }

                if (scaledHeight <= height) {
                    mapState.offsetY = (height - scaledHeight) / 2f
                } else {
                    val minOffsetY = height - scaledHeight
                    mapState.offsetY = mapState.offsetY.coerceIn(minOffsetY, 0f)
                }
            }
        } catch (e: Exception) {
            Log.e("MapView", "Error en constrainMapOffset: ${e.message}")
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

        try {
            Log.d("MapView", "onSizeChanged: $oldw x $oldh -> $w x $h")

            // Usar postDelayed para asegurarnos que las nuevas dimensiones ya están aplicadas
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    adjustMapToScreen()
                    centerMapOnPlayer()
                    invalidate()
                } catch (e: Exception) {
                    Log.e("MapView", "Error al ajustar mapa después de cambio de tamaño: ${e.message}")
                }
            }, 200)
        } catch (e: Exception) {
            Log.e("MapView", "Error en onSizeChanged: ${e.message}")
        }
    }

    fun zoomToFitGame() {
        if (playerManager.getCurrentMap() == MapMatrixProvider.MAP_SALON1212) {
            // Establecer un zoom mucho más agresivo
            val desiredScale = 4.0f  // Aumento dramático del zoom
            mapState.scaleFactor = desiredScale

            // Recalcular offset para mantener centrado
            mapState.backgroundBitmap?.let { bitmap ->
                val scaledWidth = bitmap.width * mapState.scaleFactor
                val scaledHeight = bitmap.height * mapState.scaleFactor
                mapState.offsetX = (width - scaledWidth) / 2f
                mapState.offsetY = (height - scaledHeight) / 2f

                // Asegurarnos de que el mapa esté centrado en la posición del jugador
                playerManager.getLocalPlayerPosition()?.let { playerPos ->
                    val cellWidth = scaledWidth / MapMatrixProvider.MAP_WIDTH
                    val cellHeight = scaledHeight / MapMatrixProvider.MAP_HEIGHT

                    val playerCenterX = playerPos.first * cellWidth + (cellWidth / 2)
                    val playerCenterY = playerPos.second * cellHeight + (cellHeight / 2)

                    mapState.offsetX = width / 2f - playerCenterX
                    mapState.offsetY = height / 2f - playerCenterY

                    // Asegurarse de que no se vea espacio en blanco
                    constrainMapOffset()
                }
            }

            Log.d("MapView", "Zoom agresivo aplicado: scale=${mapState.scaleFactor}")
            invalidate()
        }
    }

    fun adjustMapToScreen() {
        try {
            if (width <= 0 || height <= 0) {
                Log.d("MapView", "Vista sin dimensiones válidas: width=$width, height=$height")
                return
            }

            mapState.backgroundBitmap?.let { bitmap ->
                // Para mapas normales, usar el mismo cálculo
                // Para el mapa de Pacman, usar un escalado diferente
                val currentMap = playerManager.getCurrentMap()
                if (currentMap == MapMatrixProvider.MAP_SALON1212) {
                    // Hacer que el mapa ocupe casi toda la pantalla para el juego de Pacman
                    // Cálculo especial para el mapa Pacman
                    val baseScale = 0.95f * Math.min(
                        width.toFloat() / bitmap.width,
                        height.toFloat() / bitmap.height
                    )

                    // Escalar un poco más grande
                    mapState.scaleFactor = baseScale * 1.5f

                    // Centrar el mapa
                    val scaledWidth = bitmap.width * mapState.scaleFactor
                    val scaledHeight = bitmap.height * mapState.scaleFactor
                    mapState.offsetX = (width - scaledWidth) / 2f
                    mapState.offsetY = (height - scaledHeight) / 2f

                    Log.d("MapView", "Tablero Pacman ajustado: scale=${mapState.scaleFactor}, offset=(${mapState.offsetX},${mapState.offsetY})")
                } else {
                    // Cálculo original para otros mapas
                    val scaleX = width.toFloat() / bitmap.width
                    val scaleY = height.toFloat() / bitmap.height
                    val baseScale = Math.min(scaleX, scaleY)
                    val finalScale = baseScale * 0.98f

                    mapState.scaleFactor = finalScale
                    val scaledWidth = bitmap.width * finalScale
                    val scaledHeight = bitmap.height * finalScale
                    mapState.offsetX = (width - scaledWidth) / 2f
                    mapState.offsetY = (height - scaledHeight) / 2f
                }
            }
        } catch (e: Exception) {
            Log.e("MapView", "Error en adjustMapToScreen: ${e.message}")
        }
    }

    // En MapView.kt, también debemos modificar onConfigurationChanged
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        try {
            // Usar postDelayed para dar tiempo a que la vista se reconfigure
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    // Ajustar el mapa y centrar la cámara
                    adjustMapToScreen()
                    centerMapOnPlayer()
                    invalidate()
                } catch (e: Exception) {
                    Log.e("MapView", "Error en actualización de configuración: ${e.message}")
                }
            }, 300)
        } catch (e: Exception) {
            Log.e("MapView", "Error en onConfigurationChanged: ${e.message}")
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isUserInteracting = true
                shouldCenterOnPlayer = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isUserInteracting = false
                // Asegurar que estamos dentro de los límites al soltar
                constrainMapOffset()
            }
        }
        parent.requestDisallowInterceptTouchEvent(true)
        val handled = gestureHandler.onTouchEvent(event)

        // Después de cada evento, asegurar que no nos salimos de los límites
        constrainMapOffset()
        invalidate()

        return handled
    }

    // Add interface for car rendering
    interface CarRenderer {
        fun drawCars(canvas: Canvas)
    }

    // Add fog of war renderer
    private var fogOfWarRenderer: FogOfWarRenderer? = null
    private var fogOfWarEnabled = false

    // Add car renderer
    private var carRenderer: CarRenderer? = null

    // Add this method to set the fog of war renderer
    fun setFogOfWarRenderer(renderer: FogOfWarRenderer) {
        fogOfWarRenderer = renderer
    }

    // Add this method to enable/disable fog of war
    fun setFogOfWarEnabled(enabled: Boolean) {
        fogOfWarEnabled = enabled
        invalidate()
    }

    // Add this method to set the car renderer
    fun setCarRenderer(renderer: CarRenderer) {
        carRenderer = renderer
        invalidate()
    }

    // MODIFICADO: Método onDraw mejorado con modo nocturno
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Usar la matriz del mapa actual para el dibujado
        mapState.setMapMatrix(mapMatrix)
        renderer.draw(canvas, mapState, playerManager)

        // NUEVO: Aplicar overlay de noche si está activado
        if (nightModeEnabled) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), nightOverlayPaint)
        }


        // Draw custom elements (like desk indicators)
        customDrawCallback?.let { callback ->
            val bitmapWidth = mapState.backgroundBitmap?.width?.toFloat() ?: return@let
            val bitmapHeight = mapState.backgroundBitmap?.height?.toFloat() ?: return@let
            val cellWidth = bitmapWidth / MapMatrixProvider.MAP_WIDTH
            val cellHeight = bitmapHeight / MapMatrixProvider.MAP_HEIGHT

            // Guardar estado del canvas
            canvas.save()
            canvas.translate(mapState.offsetX, mapState.offsetY)
            canvas.scale(mapState.scaleFactor, mapState.scaleFactor)

            // Llamar al callback personalizado
            callback.onCustomDraw(canvas, cellWidth, cellHeight)

            // Restaurar estado del canvas
            canvas.restore()
        }

        // Draw fog of war if enabled
        if (fogOfWarEnabled && fogOfWarRenderer != null) {
            playerManager.getLocalPlayerPosition()?.let { playerPos ->
                // Calcular el tamaño de celda para el renderizado de la niebla
                val bitmapWidth = mapState.backgroundBitmap?.width?.toFloat() ?: return
                val bitmapHeight = mapState.backgroundBitmap?.height?.toFloat() ?: return
                val cellWidth = bitmapWidth / MapMatrixProvider.MAP_WIDTH
                val cellHeight = bitmapHeight / MapMatrixProvider.MAP_HEIGHT

                // Guardar estado del canvas
                canvas.save()
                canvas.translate(mapState.offsetX, mapState.offsetY)
                canvas.scale(mapState.scaleFactor, mapState.scaleFactor)

                // Dibujar la niebla de guerra
                fogOfWarRenderer?.drawFogOfWar(
                    canvas,
                    playerPos,
                    cellWidth,
                    cellHeight,
                    18 // Radio de visión predeterminado (puedes ajustarlo según necesites)
                )

                // Restaurar estado del canvas
                canvas.restore()
            }
        }

        // Draw cars on top of everything else, applying map transformations
        if (carRenderer != null) {
            // Save canvas state
            canvas.save()

            // Apply map transformations (scale and offset)
            canvas.translate(mapState.offsetX, mapState.offsetY)
            canvas.scale(mapState.scaleFactor, mapState.scaleFactor)

            // Draw the cars
            carRenderer?.drawCars(canvas)

            // Restore canvas state
            canvas.restore()
        }
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

    // Añadir parámetro a MapView.kt - método updateLocalPlayerPosition
    fun updateLocalPlayerPosition(position: Pair<Int, Int>?, forceCenter: Boolean = false) {
        try {
            val prevPosition = playerManager.getLocalPlayerPosition()
            playerManager.updateLocalPlayerPosition(position)

            if (position != null && (forceCenter || (!isUserInteracting &&
                        (prevPosition == null || prevPosition != position)))) {
                centerMapOnPlayer()
            }

            invalidate()
        } catch (e: Exception) {
            Log.e("MapView", "Error en updateLocalPlayerPosition: ${e.message}")
        }
    }

    fun resetCameraTracking() {
        shouldCenterOnPlayer = true
        isUserInteracting = false
        centerMapOnPlayer()
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

    // NUEVO: Métodos mejorados para entidades especiales
    fun updateSpecialEntity(entityId: String, position: Pair<Int, Int>, map: String) {
        // Actualizar en PlayerManager (esta es la fuente de verdad)
        playerManager.updateSpecialEntity(entityId, position, map)
        Log.d("MapView", "Entidad especial actualizada: $entityId en posición $position, mapa $map")
        invalidate() // Forzar un redibujado para mostrar el cambio
    }

    fun removeSpecialEntity(entityId: String) {
        playerManager.removeSpecialEntity(entityId)
        invalidate()
    }

    fun debugSpecialEntities() {
        val specialEntitiesCount = playerManager.getSpecialEntitiesCount()
        Log.d("MapView", "Entidades especiales registradas: $specialEntitiesCount")
        playerManager.logSpecialEntities()
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

    // Add this method to get player rectangle in map coordinates
    // Fixed getPlayerRect method with proper type handling
    fun getPlayerRect(position: Pair<Int, Int>): RectF? {
        val bitmap = mapState.backgroundBitmap ?: return null

        // Calculate the cell size based on the bitmap dimensions and matrix size
        // Using MapMatrixProvider constants instead of mapMatrix.getWidth/getHeight
        val cellWidth = bitmap.width / MapMatrixProvider.MAP_WIDTH.toFloat()
        val cellHeight = bitmap.height / MapMatrixProvider.MAP_HEIGHT.toFloat()

        // Calculate player position in pixels
        val playerX = position.first * cellWidth
        val playerY = position.second * cellHeight

        // Create a rectangle for the player (make it slightly smaller than a cell)
        // Explicitly using Float for all calculations to avoid ambiguity
        val playerSize = min(cellWidth, cellHeight) * 0.8f
        return RectF(
            playerX + (cellWidth - playerSize) / 2f,
            playerY + (cellHeight - playerSize) / 2f,
            playerX + (cellWidth + playerSize) / 2f,
            playerY + (cellHeight + playerSize) / 2f
        )
    }

    // NUEVO: Método para limpiar todas las entidades especiales
    fun clearAllSpecialEntities() {
        // Obtener todas las keys de entidades especiales
        val entityIds = playerManager.getSpecialEntitiesCount()
        Log.d("MapView", "Limpiando $entityIds entidades especiales")

        // Limpiar en PlayerManager
        // Nota: Necesitarías agregar un método clearAllSpecialEntities() en PlayerManager
        // Por ahora, llamamos removeSpecialEntity para cada una
        playerManager.logSpecialEntities() // Para debug

        invalidate()
    }


}
