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
    private var currentMap = "main"

    data class PlayerInfo(
        val position: Pair<Int, Int>,
        val map: String
    )

    private val paintLocalPlayer = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val paintRemotePlayer = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val paintText = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(3f, 0f, 0f, Color.WHITE)
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
        // Usar el mapa recibido, no "main"
        remotePlayerPositions[playerId] = PlayerInfo(position, receivedMap)
        Log.d("PlayerManager", "Updated player $playerId position: $position in map: $receivedMap")
    }


    private fun drawPlayer(canvas: Canvas, position: Pair<Int, Int>, playerId: String, paint: Paint, cellWidth: Float, cellHeight: Float) {
        val playerX = position.first * cellWidth + cellWidth / 2
        val playerY = position.second * cellHeight + cellHeight / 2
        canvas.drawCircle(playerX, playerY, cellWidth / 4f, paint)
        canvas.drawText(
            playerId,
            playerX,
            playerY - cellHeight / 2,
            paintText
        )
    }


    fun drawPlayers(canvas: Canvas, mapState: MapState) {
        val cellWidth = mapState.backgroundBitmap?.width?.div(40f) ?: return
        val cellHeight = mapState.backgroundBitmap?.height?.div(40f) ?: return

        Log.d("PlayerManager", "Drawing players in map: $currentMap")

        // Solo dibujar jugadores que coincidan con el mapa actual
        remotePlayerPositions.entries
            .filter { it.value.map == currentMap }
            .forEach { (id, info) ->
                Log.d("PlayerManager", "Drawing player $id in map ${info.map}")
                val paint = if (id == localPlayerId) paintLocalPlayer else paintRemotePlayer
                val label = if (id == localPlayerId) "Tú ($id)" else id
                drawPlayer(canvas, info.position, label, paint, cellWidth, cellHeight)
            }
    }

    // Método para actualizar el mapa actual
    fun setCurrentMap(map: String) {
        currentMap = map
        Log.d("PlayerManager", "Current map set to: $map")
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
                    val receivedMap = jsonObject.getString("currentmap")

                    // Solo actualizar si coincide con el mapa actual
                    if (receivedMap == currentMap) {
                        remotePlayerPositions[playerId] = PlayerInfo(position, receivedMap)
                        Log.d("PlayerManager", "Stored player $playerId position: $position in map: $receivedMap")
                    } else {
                        Log.d("PlayerManager", "Ignored update for player $playerId - different map (received: $receivedMap, current: $currentMap)")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerManager", "Error processing WebSocket message", e)
        }
    }
}
