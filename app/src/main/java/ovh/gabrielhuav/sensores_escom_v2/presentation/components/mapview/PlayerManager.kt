package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import org.json.JSONObject

class PlayerManager {
    private var localPlayerPosition: Pair<Int, Int>? = null
    private var remotePlayerPositions = mutableMapOf<String, PlayerInfo>()
    var localPlayerId: String = "player_local"
    private var currentMap = MapMatrixProvider.MAP_MAIN

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

            // Dibujar entidades especiales (zombie, etc.)
            drawSpecialEntities(canvas, cellWidth, cellHeight)
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error en drawPlayers: ${e.message}")
        }
    }

    // Método para dibujar entidades especiales
    private fun drawSpecialEntities(canvas: Canvas, cellWidth: Float, cellHeight: Float) {
        try {
            // Solo dibujar entidades que estén en el mapa actual
            val normalizedCurrentMap = MapMatrixProvider.normalizeMapName(currentMap)

            specialEntities.forEach { (entityId, info) ->
                val (position, entityMap) = info
                val normalizedEntityMap = MapMatrixProvider.normalizeMapName(entityMap)

                if (normalizedEntityMap == normalizedCurrentMap) {
                    val x = position.first * cellWidth + (cellWidth / 2)
                    val y = position.second * cellHeight + (cellHeight / 2)

                    when {
                        entityId == "zombie" -> {
                            // Dibujar zombie (más grande que un jugador normal)
                            canvas.drawCircle(x, y, cellWidth * 0.4f, zombiePaint)
                            canvas.drawText("ZOMBIE", x, y - cellHeight * 0.7f, zombieTextPaint)
                        }
                        entityId.startsWith("item_") -> {
                            // Dibujar ítem
                            canvas.drawCircle(x, y, cellWidth * 0.3f, itemPaint)
                            canvas.drawText("ITEM", x, y - cellHeight * 0.5f, itemTextPaint)
                        }
                    }

                    Log.d("PlayerManager", "Dibujada entidad $entityId en ($x, $y)")
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error en drawSpecialEntities: ${e.message}")
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