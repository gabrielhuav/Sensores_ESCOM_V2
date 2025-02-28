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

    private fun drawPlayer(canvas: Canvas, position: Pair<Int, Int>, playerId: String, paint: Paint, cellWidth: Float, cellHeight: Float) {
        val playerX = position.first * cellWidth + cellWidth / 2
        val playerY = position.second * cellHeight + cellHeight / 2
        canvas.drawCircle(playerX, playerY, cellWidth / 3f, paint)
        canvas.drawText(
            playerId,
            playerX,
            playerY - cellHeight / 2,
            paintText
        )
    }

    fun drawPlayers(canvas: Canvas, mapState: MapState) {
        val cellWidth = mapState.backgroundBitmap?.width?.div(MapMatrixProvider.MAP_WIDTH.toFloat()) ?: return
        val cellHeight = mapState.backgroundBitmap?.height?.div(MapMatrixProvider.MAP_HEIGHT.toFloat()) ?: return

        Log.d("PlayerManager", "Drawing players in map: $currentMap")
        Log.d("PlayerManager", "Total remote players: ${remotePlayerPositions.size}")

        // Solo dibujar los jugadores que están en el mapa actual
        val playersToDraw = remotePlayerPositions.entries
            .filter { it.value.map == currentMap }

        Log.d("PlayerManager", "Players in current map: ${playersToDraw.size}")

        playersToDraw.forEach { (id, info) ->
            val paint = if (id == localPlayerId) paintLocalPlayer else paintRemotePlayer
            val label = if (id == localPlayerId) "Tú" else id
            drawPlayer(canvas, info.position, label, paint, cellWidth, cellHeight)
            Log.d("PlayerManager", "Drew player $id at position ${info.position}")
        }
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
}