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
        // Usar el mapa recibido
        remotePlayerPositions[playerId] = PlayerInfo(position, receivedMap)
        Log.d("PlayerManager", "Updated player $playerId position: $position in map: $receivedMap")
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
        // Obtener dimensiones de una celda
        val cellWidth = mapState.backgroundBitmap?.width?.div(MapMatrixProvider.MAP_WIDTH.toFloat()) ?: return
        val cellHeight = mapState.backgroundBitmap?.height?.div(MapMatrixProvider.MAP_HEIGHT.toFloat()) ?: return

        // Log para depuración
        Log.d("PlayerManager", "Dibujando jugadores en mapa: $currentMap")
        Log.d("PlayerManager", "Total jugadores: ${remotePlayerPositions.size}, Entidades especiales: ${specialEntities.size}")

        // PASO 1: Dibujar jugadores regulares
        val playersToDraw = remotePlayerPositions.entries
            .filter { it.value.map == currentMap }

        Log.d("PlayerManager", "Jugadores en mapa actual: ${playersToDraw.size}")

        // Dibujar cada jugador
        playersToDraw.forEach { (id, info) ->
            val paint = if (id == localPlayerId) paintLocalPlayer else paintRemotePlayer
            val label = if (id == localPlayerId) "Tú" else id
            drawPlayer(canvas, info.position, label, paint, cellWidth, cellHeight)  // Descomentar esta línea
            Log.d("PlayerManager", "Dibujado jugador $id en posición ${info.position}")
        }

        // PASO 2: Dibujar entidades especiales
        if (specialEntities.isNotEmpty()) {
            Log.d("PlayerManager", "Dibujando ${specialEntities.size} entidades especiales")

            // Dibujar entidades especiales que están en el mapa actual
            specialEntities.forEach { (entityId, info) ->
                val (position, entityMap) = info

                // Solo dibujar si está en el mapa actual
                if (entityMap == currentMap) {
                    Log.d("PlayerManager", "Dibujando entidad $entityId en posición $position")

                    when {
                        entityId == "zombie" -> drawZombie(canvas, position, cellWidth, cellHeight)
                        entityId.startsWith("item_") -> drawItem(canvas, position, cellWidth, cellHeight)
                    }
                }
            }
        } else {
            Log.d("PlayerManager", "No hay entidades especiales para dibujar")
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
    // Método para actualizar el mapa actual
    fun setCurrentMap(map: String) {
        if (currentMap != map) {
            Log.d("PlayerManager", "Current map changed from $currentMap to $map")
            currentMap = map
            // Actualizar la posición del jugador local en el nuevo mapa
            localPlayerPosition?.let { pos ->
                remotePlayerPositions[localPlayerId] = PlayerInfo(pos, map)
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
                    val receivedMap = jsonObject.getString("map")

                    // Actualizar sin importar el mapa
                    remotePlayerPositions[playerId] = PlayerInfo(position, receivedMap)
                    Log.d("PlayerManager", "Updated remote player $playerId: pos=$position, map=$receivedMap")
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
                            val playerMap = playerData.getString("map")
                            remotePlayerPositions[playerId] = PlayerInfo(position, playerMap)
                            Log.d("PlayerManager", "Updated player $playerId from positions update: map=$playerMap")
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