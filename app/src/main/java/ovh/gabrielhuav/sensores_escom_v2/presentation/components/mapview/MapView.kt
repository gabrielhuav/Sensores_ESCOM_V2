package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
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

    // Interfaz para notificar transiciones de mapa
    interface MapTransitionListener {
        fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>)
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

    // Método para establecer el listener de transición
    fun setMapTransitionListener(listener: MapTransitionListener) {
        transitionListener = listener
    }

    // Método para cambiar el mapa actual
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

    private fun adjustMapToScreen() {
        try {
            if (width <= 0 || height <= 0) {
                Log.d("MapView", "Vista sin dimensiones válidas: width=$width, height=$height")
                return
            }

            mapState.backgroundBitmap?.let { bitmap ->
                // Primero, determinar los factores de escala para ancho y alto
                val scaleX = width.toFloat() / bitmap.width
                val scaleY = height.toFloat() / bitmap.height

                // Para asegurar que todo el mapa sea visible, usamos la menor escala
                // Esto garantiza que el mapa ocupe la mayor cantidad de espacio posible
                // sin salirse de los límites de la pantalla
                val baseScale = Math.min(scaleX, scaleY)

                // Ajustamos con un factor para estar seguros de ocupar toda la pantalla
                val finalScale = baseScale * 0.98f  // 98% para dejar un pequeño margen

                // Establecer la escala
                mapState.scaleFactor = finalScale

                // Calcular dimensiones escaladas del mapa
                val scaledWidth = bitmap.width * finalScale
                val scaledHeight = bitmap.height * finalScale

                // Centrar el mapa en la pantalla
                mapState.offsetX = (width - scaledWidth) / 2f
                mapState.offsetY = (height - scaledHeight) / 2f

                Log.d("MapView", "Mapa ajustado: scale=$finalScale, offset=(${mapState.offsetX},${mapState.offsetY}), " +
                        "screenSize=($width,$height), scaledMapSize=(${scaledWidth},${scaledHeight})")
            } ?: run {
                Log.e("MapView", "No hay bitmap para ajustar")
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
// Método para añadir a la clase MapView
// Agregar estas funciones después de las existentes en MapView.kt

    /**
     * Mapa para entidades especiales como enemigos, ítems, etc.
     * Key: ID de la entidad
     * Value: Pair<Pair<Int, Int>, String> (posición, mapa)
     */
    private val specialEntities = mutableMapOf<String, Pair<Pair<Int, Int>, String>>()

    fun updateSpecialEntity(entityId: String, position: Pair<Int, Int>, map: String) {
        // Actualizar en PlayerManager (esta es la fuente de verdad)
        playerManager.updateSpecialEntity(entityId, position, map)

        // No necesitamos mantener una copia local aquí
        // specialEntities[entityId] = Pair(position, map) <- QUITAR ESTA LÍNEA

        Log.d("MapView", "Entidad especial actualizada: $entityId en posición $position, mapa $map")
        invalidate() // Forzar un redibujado para mostrar el cambio
    }

    fun removeSpecialEntity(entityId: String) {
        specialEntities.remove(entityId)
    }


    /**
     * Método para dibujar entidades especiales
     * Este método debería ser llamado desde onDraw en PlayerManager
     */
    fun drawSpecialEntities(canvas: Canvas, cellWidth: Float, cellHeight: Float) {
        val currentMapId = playerManager.getCurrentMap()

        // Configurar pinturas para diferentes tipos de entidades
        val zombiePaint = Paint().apply {
            color = Color.rgb(0, 100, 0)  // Verde oscuro para zombies
            style = Paint.Style.FILL
        }

        val itemPaint = Paint().apply {
            color = Color.rgb(255, 215, 0)  // Dorado para ítems
            style = Paint.Style.FILL
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }

        // Dibujar cada entidad especial que esté en el mapa actual
        specialEntities.forEach { (entityId, info) ->
            val (position, entityMap) = info

            // Solo dibujar entidades que estén en el mapa actual
            if (entityMap == currentMapId) {
                val entityX = position.first * cellWidth + cellWidth / 2
                val entityY = position.second * cellHeight + cellHeight / 2

                // Determinar qué tipo de entidad es por su ID
                when {
                    entityId == "zombie" -> {
                        // Dibujar un zombie (círculo verde más grande)
                        canvas.drawCircle(entityX, entityY, cellWidth * 0.4f, zombiePaint)
                        canvas.drawText("ZOMBIE", entityX, entityY - cellHeight * 0.7f, textPaint)
                    }
                    entityId.startsWith("item_") -> {
                        // Dibujar un ítem (estrella dorada)
                        canvas.drawCircle(entityX, entityY, cellWidth * 0.3f, itemPaint)
                        canvas.drawText("ITEM", entityX, entityY - cellHeight * 0.5f, textPaint)
                    }
                    // Añadir más tipos según sea necesario
                }
            }
        }
    }

    fun debugSpecialEntities() {
        val specialEntitiesCount = playerManager.getSpecialEntitiesCount()
        Log.d("MapView", "Entidades especiales registradas: $specialEntitiesCount")
        playerManager.logSpecialEntities()
    }
}