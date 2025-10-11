package ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.pacman.PacmanEntityRenderer
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.pacman.PacmanController

class PlayerManager {
    private var localPlayerPosition: Pair<Int, Int>? = null
    private var remotePlayerPositions = mutableMapOf<String, PlayerInfo>()
    var localPlayerId: String = "player_local"
    private var currentMap = MapMatrixProvider.MAP_MAIN
    private val pacmanRenderer = PacmanEntityRenderer()
    private var currentPacmanDirection = PacmanController.DIRECTION_RIGHT // Dirección por defecto

    data class PlayerInfo(
        val position: Pair<Int, Int>,
        val map: String
    )

    private val paintLocalPlayer = Paint().apply {
        color = Color.rgb(50, 205, 50)  // Verde lima para el jugador local
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2f
    }

    private val paintRemotePlayer = Paint().apply {
        color = Color.rgb(255, 105, 180)  // Rosa intenso para jugadores remotos
        style = Paint.Style.FILL_AND_STROKE
        strokeWidth = 2f
    }


    private val paintText = Paint().apply {
        color = Color.rgb(255, 255, 255)  // Texto blanco
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 0f, Color.BLACK)  // Sombra negra para legibilidad
    }

    // 2.1. Añadir una interfaz para el callback de verificación de visibilidad dentro de la clase:
    interface EntityVisibilityChecker {
        fun isEntityVisible(entityId: String, position: Pair<Int, Int>): Boolean
    }

    // 2.2. Añadir una propiedad para almacenar el callback:
    private var visibilityChecker: EntityVisibilityChecker? = null

    // 2.3. Añadir un método para establecer el checker:
    fun setEntityVisibilityChecker(checker: EntityVisibilityChecker) {
        visibilityChecker = checker
    }

    fun getCurrentMap(): String = currentMap

    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        Log.d("PlayerManager", "Updating local player position: $position in map: $currentMap")
        localPlayerPosition = position
        // Actualizar también en el mapa de jugadores remotos
        position?.let {
            remotePlayerPositions[localPlayerId] = PlayerInfo(it, currentMap)
        }
    }

    fun updateRemotePlayerPositions(positions: Map<String, Pair<Int, Int>>) {
        positions.forEach { (id, pos) ->
            if (id != localPlayerId) {
                // Aquí mantenemos el mapa original del jugador remoto si ya existe
                val existingPlayerInfo = remotePlayerPositions[id]
                val playerMap = existingPlayerInfo?.map ?: currentMap
                remotePlayerPositions[id] = PlayerInfo(pos, playerMap)
            }
        }
    }

    fun updateRemotePlayerPosition(playerId: String, position: Pair<Int, Int>, receivedMap: String) {
        // Normalizar el nombre del mapa
        val normalizedMap = MapMatrixProvider.normalizeMapName(receivedMap)

        // Usar el mapa normalizado
        remotePlayerPositions[playerId] = PlayerInfo(position, normalizedMap)
        Log.d("PlayerManager", "Updated player $playerId position: $position in normalized map: $normalizedMap (original: $receivedMap)")
    }

    private fun normalizeMapId(mapId: String): String {
        // Casos específicos conocidos
        return when {
            mapId.contains("cafeteria") -> MapMatrixProvider.MAP_CAFETERIA
            mapId.contains("salon2009") -> MapMatrixProvider.MAP_SALON2009
            mapId.contains("salon2010") -> MapMatrixProvider.MAP_SALON2010
            mapId.contains("building2") -> MapMatrixProvider.MAP_BUILDING2
            mapId.contains("main") -> MapMatrixProvider.MAP_MAIN
            mapId.contains("escom_building2_piso1") -> MapMatrixProvider.MAP_BUILDING2_PISO1
            else -> mapId
        }
    }


    private fun drawItem(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float) {
        // Configurar pinturas
        val itemPaint = Paint().apply {
            color = Color.rgb(255, 215, 0)  // Color dorado para ítems
            style = Paint.Style.FILL
        }

        val itemTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }

        // Calcular posición en píxeles
        val itemX = position.first * cellWidth + cellWidth / 2
        val itemY = position.second * cellHeight + cellHeight / 2

        // Dibujar el círculo del ítem
        canvas.drawCircle(itemX, itemY, cellWidth * 0.3f, itemPaint)

        // Dibujar texto "ITEM"
        canvas.drawText("ITEM", itemX, itemY - cellHeight * 0.5f, itemTextPaint)
    }
    /**
     * Dibuja un conejo en el mapa
     */
    private fun drawRabbit(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float) {
        // Configurar pinturas
        val rabbitPaint = Paint().apply {
            color = Color.rgb(255, 182, 193) // Rosa claro para conejos
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2f
        }

        val rabbitTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }

        // Calcular posición en píxeles
        val rabbitX = position.first * cellWidth + cellWidth / 2
        val rabbitY = position.second * cellHeight + cellHeight / 2

        // Dibujar el cuerpo del conejo (más pequeño que un zombie)
        canvas.drawCircle(rabbitX, rabbitY, cellWidth * 0.35f, rabbitPaint)

        // Dibujar texto "RABBIT" o emoji
        canvas.drawText("🐰", rabbitX, rabbitY - cellHeight * 0.7f, rabbitTextPaint)

        Log.d("PlayerManager", "Rabbit dibujado en posición ($rabbitX, $rabbitY)")
    }
    /**
     * Dibuja el zombie en el mapa
     */
    private fun drawZombie(canvas: Canvas, position: Pair<Int, Int>, cellWidth: Float, cellHeight: Float) {
        // Configurar pinturas
        val zombiePaint = Paint().apply {
            color = Color.rgb(50, 150, 50) // Verde zombie
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 3f
        }

        val zombieTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }

        // Calcular posición en píxeles
        val zombieX = position.first * cellWidth + cellWidth / 2
        val zombieY = position.second * cellHeight + cellHeight / 2

        // Dibujar el cuerpo del zombie (más grande que un jugador normal)
        canvas.drawCircle(zombieX, zombieY, cellWidth * 0.4f, zombiePaint)

        // Dibujar texto "ZOMBIE"
        canvas.drawText("ZOMBIE", zombieX, zombieY - cellHeight * 0.7f, zombieTextPaint)

        Log.d("PlayerManager", "Zombie dibujado en posición ($zombieX, $zombieY)")
    }

// Añadir a la clase PlayerManager.kt

    // Mapa para entidades especiales (zombies, ítems, etc.)
    private val specialEntities = mutableMapOf<String, Pair<Pair<Int, Int>, String>>()

    /**
     * Actualiza la posición de una entidad especial
     */
    fun updateSpecialEntity(entityId: String, position: Pair<Int, Int>, map: String) {
        specialEntities[entityId] = Pair(position, map)
    }

    /**
     * Elimina una entidad especial
     */
    fun removeSpecialEntity(entityId: String) {
        specialEntities.remove(entityId)
    }

    fun drawPlayers(canvas: Canvas, mapState: MapState) {
        try {
            // Dimensiones reales del bitmap y células
            // Nota: No usamos mapState.scaleFactor aquí porque canvas ya tiene la escala aplicada
            val bitmapWidth = mapState.backgroundBitmap?.width?.toFloat() ?: return
            val bitmapHeight = mapState.backgroundBitmap?.height?.toFloat() ?: return

            val cellWidth = bitmapWidth / MapMatrixProvider.MAP_WIDTH
            val cellHeight = bitmapHeight / MapMatrixProvider.MAP_HEIGHT

            // Logs para depuración
            Log.d("PlayerManager", "Dibujando jugadores en mapa: $currentMap")
            Log.d("PlayerManager", "Total jugadores: ${remotePlayerPositions.size}")

            // Filtrar jugadores que están en el mismo mapa
            val normalizedCurrentMap = MapMatrixProvider.normalizeMapName(currentMap)

            // Filtrar jugadores para mostrar
            val playersToDraw = remotePlayerPositions.entries
                .filter {
                    val normalizedPlayerMap = MapMatrixProvider.normalizeMapName(it.value.map)
                    normalizedPlayerMap == normalizedCurrentMap
                }

            Log.d("PlayerManager", "Jugadores a dibujar: ${playersToDraw.size} en mapa $normalizedCurrentMap")

            // Dibujar cada jugador
            playersToDraw.forEach { (id, info) ->
                // Comprobar si el jugador es visible a través de la niebla
                val isVisible = visibilityChecker?.isEntityVisible(id, info.position) ?: true

                // Solo dibujar si es visible o es el jugador local
                if (isVisible || id == localPlayerId) {
                    val paint = if (id == localPlayerId) paintLocalPlayer else paintRemotePlayer
                    val label = if (id == localPlayerId) "Tú" else id

                    // Posición en píxeles (centrada en la celda)
                    val x = info.position.first * cellWidth + (cellWidth / 2)
                    val y = info.position.second * cellHeight + (cellHeight / 2)

                    // Dibujar el jugador
                    canvas.drawCircle(x, y, cellWidth / 3, paint)

                    // Dibujar nombre del jugador
                    canvas.drawText(
                        label,
                        x,
                        y - cellHeight / 2,
                        paintText
                    )

                    Log.d("PlayerManager", "Dibujado jugador $id en ($x, $y)")
                }
            }
            // Dibujar entidades especiales (zombie, etc.)
            drawSpecialEntities(canvas, cellWidth, cellHeight)
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error en drawPlayers: ${e.message}")
        }
    }

    // Método para dibujar entidades especiales
    private fun drawSpecialEntities(canvas: Canvas, cellWidth: Float, cellHeight: Float) {
        try {
            // Only draw entities that are in the current map
            val normalizedCurrentMap = MapMatrixProvider.normalizeMapName(currentMap)

            // Factor de escala mucho más agresivo para las entidades de Pacman
            val scaleFactor = if (normalizedCurrentMap == MapMatrixProvider.MAP_SALON1212) 3.0f else 1.0f
            val adjustedCellWidth = cellWidth * scaleFactor
            val adjustedCellHeight = cellHeight * scaleFactor

            Log.d("PlayerManager", "Drawing special entities with super-sized cells: $adjustedCellWidth x $adjustedCellHeight")

            specialEntities.forEach { (entityId, info) ->
                val (position, entityMap) = info
                val normalizedEntityMap = MapMatrixProvider.normalizeMapName(entityMap)

                if (normalizedEntityMap == normalizedCurrentMap) {
                    // Comprobar si la entidad es visible a través de la niebla de guerra
                    val isVisible = visibilityChecker?.isEntityVisible(entityId, position) ?: true

                    // Solo dibujar si la entidad es visible
                    if (isVisible) {
                        val x = position.first * cellWidth + (cellWidth / 2)
                        val y = position.second * cellHeight + (cellHeight / 2)

                        when {
                            entityId == "pacman" -> {
                                Log.d("PlayerManager", "Drawing super-sized Pacman at ($x, $y) with direction $currentPacmanDirection")

                                try {
                                    pacmanRenderer.drawPacman(canvas, x, y, adjustedCellWidth, currentPacmanDirection)
                                } catch(e: Exception) {
                                    // Fallback como antes...
                                }
                            }
                            entityId == "pacman_direction" -> {
                                // Guardar la dirección actual de Pacman para usarla en el próximo frame
                                try {
                                    currentPacmanDirection = entityMap.toInt()
                                    Log.d("PlayerManager", "Actualizada dirección de Pacman: $currentPacmanDirection")
                                } catch(e: Exception) {
                                    Log.e("PlayerManager", "Error actualizando dirección de Pacman: ${e.message}")
                                }
                            }
                            entityId.startsWith("ghost_") -> {
                                // Fantasmas mucho más grandes
                                try {
                                    pacmanRenderer.drawGhost(canvas, x, y, adjustedCellWidth, entityId)
                                } catch(e: Exception) {
                                    // Fallback manual
                                    val ghostPaint = Paint().apply {
                                        color = when {
                                            entityId.endsWith("0") -> Color.RED
                                            entityId.endsWith("1") -> Color.rgb(255, 184, 255) // Pink
                                            entityId.endsWith("2") -> Color.CYAN
                                            else -> Color.rgb(255, 184, 82) // Orange
                                        }
                                        style = Paint.Style.FILL
                                        setShadowLayer(5f, 0f, 0f, Color.GRAY)
                                    }
                                    canvas.drawCircle(x, y, adjustedCellWidth * 0.7f, ghostPaint)

                                    // Añadir ojos para mejorar visibilidad
                                    val eyePaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
                                    val pupilPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

                                    // Ojo izquierdo
                                    canvas.drawCircle(x - adjustedCellWidth*0.2f, y - adjustedCellWidth*0.1f,
                                        adjustedCellWidth*0.15f, eyePaint)
                                    // Ojo derecho
                                    canvas.drawCircle(x + adjustedCellWidth*0.2f, y - adjustedCellWidth*0.1f,
                                        adjustedCellWidth*0.15f, eyePaint)

                                    // Pupilas
                                    canvas.drawCircle(x - adjustedCellWidth*0.2f, y - adjustedCellWidth*0.1f,
                                        adjustedCellWidth*0.08f, pupilPaint)
                                    canvas.drawCircle(x + adjustedCellWidth*0.2f, y - adjustedCellWidth*0.1f,
                                        adjustedCellWidth*0.08f, pupilPaint)
                                }
                            }
                            entityId.startsWith("food_") -> {
                                // Comida mucho más visible
                                try {
                                    pacmanRenderer.drawFood(canvas, x, y, adjustedCellWidth)
                                } catch(e: Exception) {
                                    // Fallback con comida más visible
                                    val foodPaint = Paint().apply {
                                        color = Color.rgb(255, 223, 0) // Amarillo brillante
                                        style = Paint.Style.FILL
                                        setShadowLayer(5f, 0f, 0f, Color.WHITE)
                                    }
                                    canvas.drawCircle(x, y, adjustedCellWidth * 0.3f, foodPaint)
                                }
                            }
                            // Sin cambios para otras entidades...
                            entityId == "zombie" || entityId.startsWith("zombie_") -> {
                                // Dibujo del zombie
                                canvas.drawCircle(x, y, cellWidth * 0.4f, zombiePaint)
                                canvas.drawText("ZOMBIE", x, y - cellHeight * 0.7f, zombieTextPaint)
                            }
                            entityId == "rabbit" || entityId.startsWith("rabbit_") -> {
                                // Dibujo del conejo
                                drawRabbit(canvas, position, cellWidth, cellHeight)
                                Log.d("PlayerManager", "🐰 Dibujando conejo $entityId en ($x, $y)")
                            }
                            entityId.startsWith("item_") -> {
                                canvas.drawCircle(x, y, cellWidth * 0.3f, itemPaint)
                                canvas.drawText("ITEM", x, y - cellHeight * 0.5f, itemTextPaint)
                            }
                        }

                        // Restringir el logging para no saturar
                        if (entityId == "pacman" || entityId.startsWith("ghost_")) {
                            Log.d("PlayerManager", "Dibujada entidad $entityId en ($x, $y) con tamaño ${adjustedCellWidth}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error in drawSpecialEntities: ${e.message}")
            e.printStackTrace()
        }
    }

    // Añadir pinturas para entidades especiales si no existen
    private val zombiePaint by lazy {
        Paint().apply {
            color = Color.rgb(50, 150, 50) // Verde zombie
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 3f
        }
    }

    private val zombieTextPaint by lazy {
        Paint().apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f, 0f, 0f, Color.BLACK)
        }
    }

    private val itemPaint by lazy {
        Paint().apply {
            color = Color.rgb(255, 215, 0)  // Dorado
            style = Paint.Style.FILL
        }
    }

    private val itemTextPaint by lazy {
        Paint().apply {
            color = Color.BLACK
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
    }

    private fun drawPlayer(canvas: Canvas, position: Pair<Int, Int>, playerId: String, paint: Paint, cellWidth: Float, cellHeight: Float) {
        val playerX = position.first * cellWidth + cellWidth / 2
        val playerY = position.second * cellHeight + cellHeight / 2

        // Dibujar el círculo del jugador
        canvas.drawCircle(playerX, playerY, cellWidth / 3f, paint)

        // Dibujar el nombre del jugador
        canvas.drawText(
            playerId,
            playerX,
            playerY - cellHeight / 2,
            paintText
        )

        Log.d("PlayerManager", "Jugador $playerId dibujado en posición ($playerX, $playerY)")
    }

    // Actualiza este método para mejorar la depuración
    fun setCurrentMap(map: String) {
        if (currentMap != map) {
            Log.d("PlayerManager", "⚠️ Current map changed from $currentMap to $map")
            currentMap = map
            // Actualizar la posición del jugador local en el nuevo mapa
            localPlayerPosition?.let { pos ->
                remotePlayerPositions[localPlayerId] = PlayerInfo(pos, map)
                Log.d("PlayerManager", "Actualizando posición del jugador local en nuevo mapa: $pos")
            }
        }
    }

    fun cleanup() {
        remotePlayerPositions.clear()
        localPlayerPosition = null
    }

    fun removeRemotePlayer(playerId: String) {
        remotePlayerPositions.remove(playerId)
    }

    fun getLocalPlayerPosition(): Pair<Int, Int>? = localPlayerPosition

    fun handleWebSocketMessage(message: String) {
        try {
            val jsonObject = JSONObject(message)
            when (jsonObject.getString("type")) {
                "update" -> {
                    val playerId = jsonObject.getString("id")
                    val position = Pair(
                        jsonObject.getInt("x"),
                        jsonObject.getInt("y")
                    )

                    // Obtener el mapa y normalizarlo
                    val receivedMap = if (jsonObject.has("map")) {
                        jsonObject.getString("map")
                    } else if (jsonObject.has("currentmap")) {
                        jsonObject.getString("currentmap")
                    } else {
                        MapMatrixProvider.MAP_MAIN // Valor predeterminado
                    }

                    val normalizedMap = MapMatrixProvider.normalizeMapName(receivedMap)

                    // Actualizar con el mapa normalizado
                    remotePlayerPositions[playerId] = PlayerInfo(position, normalizedMap)
                    Log.d("PlayerManager", "Updated from update: player=$playerId, pos=$position, map=$normalizedMap (original: $receivedMap)")
                }
                "positions" -> {
                    val players = jsonObject.getJSONObject("players")
                    players.keys().forEach { playerId ->
                        if (playerId != localPlayerId) {
                            val playerData = players.getJSONObject(playerId.toString())
                            val position = Pair(
                                playerData.getInt("x"),
                                playerData.getInt("y")
                            )

                            // Obtener y normalizar el mapa
                            val receivedMap = if (playerData.has("map")) {
                                playerData.getString("map")
                            } else if (playerData.has("currentmap")) {
                                playerData.getString("currentmap")
                            } else {
                                playerData.optString("currentMap", MapMatrixProvider.MAP_MAIN)
                            }

                            val normalizedMap = MapMatrixProvider.normalizeMapName(receivedMap)

                            remotePlayerPositions[playerId] = PlayerInfo(position, normalizedMap)
                            Log.d("PlayerManager", "Updated from positions: player=$playerId, map=$normalizedMap (original: $receivedMap)")
                        }
                    }
                }
                "disconnect" -> {
                    val disconnectedId = jsonObject.getString("id")
                    if (disconnectedId != localPlayerId) {
                        remotePlayerPositions.remove(disconnectedId)
                        Log.d("PlayerManager", "Removed disconnected player: $disconnectedId")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error processing WebSocket message", e)
        }
    }


    fun getSpecialEntitiesCount(): Int {
        return specialEntities.size
    }

    fun logSpecialEntities() {
        specialEntities.forEach { (id, info) ->
            Log.d("PlayerManager", "Entidad especial: $id en posición ${info.first}, mapa ${info.second}")
        }
    }
}