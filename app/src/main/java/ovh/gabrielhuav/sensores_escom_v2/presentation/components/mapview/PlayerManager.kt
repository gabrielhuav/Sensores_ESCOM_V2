package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import org.json.JSONObject

class PlayerManager {
    private var localPlayerPosition: Pair<Int, Int>? = null
    private val remotePlayerPositions = mutableMapOf<String, Pair<Int, Int>>()
    var localPlayerId: String = "player_local"
    private var isBluetoothServer = false
    private var bluetoothPlayerPosition: Pair<Int, Int>? = null

    // Paints para los diferentes tipos de jugadores
    private val paintLocalPlayer = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
    private val paintRemotePlayer = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val paintBluetoothPlayer = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    // Paint para el texto de los nombres
    private val paintText = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        // Agregar sombra para mejor legibilidad
        setShadowLayer(3f, 0f, 0f, Color.WHITE)
    }

    fun updateLocalPlayerPosition(position: Pair<Int, Int>?) {
        localPlayerPosition = position
    }

    fun updateRemotePlayerPositions(positions: Map<String, Pair<Int, Int>>) {
        remotePlayerPositions.clear()
        positions.forEach { (id, position) ->
            if (id != localPlayerId) {
                remotePlayerPositions[id] = position
            }
        }
    }

    fun removeRemotePlayer(playerId: String) {
        remotePlayerPositions.remove(playerId)
    }

    fun updateRemotePlayerPosition(playerId: String, position: Pair<Int, Int>?) {
        if (playerId != localPlayerId) {
            if (position != null) {
                remotePlayerPositions[playerId] = position
            } else {
                remotePlayerPositions.remove(playerId)
            }
        }
    }

    fun setBluetoothServerMode(isServer: Boolean) {
        isBluetoothServer = isServer
        paintBluetoothPlayer.color = if (isServer) Color.YELLOW else Color.GREEN
    }

    fun updateBluetoothPlayerPosition(position: Pair<Int, Int>?) {
        bluetoothPlayerPosition = position
    }

    fun drawPlayers(canvas: Canvas, mapState: MapState) {
        val cellWidth = mapState.backgroundBitmap?.width?.div(mapState.mapMatrix[0].size.toFloat()) ?: return
        val cellHeight = mapState.backgroundBitmap?.height?.div(mapState.mapMatrix.size.toFloat()) ?: return

        // Dibujar jugador local
        localPlayerPosition?.let {
            val playerX = it.first * cellWidth + cellWidth / 2
            val playerY = it.second * cellHeight + cellHeight / 2
            // Dibujar círculo del jugador
            canvas.drawCircle(playerX, playerY, cellWidth / 4f, paintLocalPlayer)
            // Dibujar nombre del jugador
            canvas.drawText(
                "Tú ($localPlayerId)",
                playerX,
                playerY - cellHeight / 2,
                paintText
            )
        }

        // Dibujar jugadores remotos
        for ((id, position) in remotePlayerPositions) {
            if (id != localPlayerId) {
                val remotePlayerX = position.first * cellWidth + cellWidth / 2
                val remotePlayerY = position.second * cellHeight + cellHeight / 2
                // Dibujar círculo del jugador remoto
                canvas.drawCircle(remotePlayerX, remotePlayerY, cellWidth / 4f, paintRemotePlayer)
                // Dibujar nombre del jugador remoto
                canvas.drawText(
                    id,
                    remotePlayerX,
                    remotePlayerY - cellHeight / 2,
                    paintText
                )
            }
        }

        // Dibujar jugador Bluetooth
        bluetoothPlayerPosition?.let {
            val bluetoothX = it.first * cellWidth + cellWidth / 2
            val bluetoothY = it.second * cellHeight + cellHeight / 2
            // Dibujar círculo del jugador Bluetooth
            canvas.drawCircle(bluetoothX, bluetoothY, cellWidth / 4f, paintBluetoothPlayer)
            // Dibujar texto indicando que es jugador Bluetooth
            canvas.drawText(
                if (isBluetoothServer) "BT Server" else "BT Client",
                bluetoothX,
                bluetoothY - cellHeight / 2,
                paintText
            )
        }
    }

    fun handleWebSocketMessage(message: String) {
        try {
            val jsonObject = JSONObject(message)
            if (jsonObject.getString("type") == "positions") {
                val players = jsonObject.getJSONObject("players")
                val positions = mutableMapOf<String, Pair<Int, Int>>()
                players.keys().forEach { playerId ->
                    val position = players.getJSONObject(playerId)
                    val x = position.getInt("x")
                    val y = position.getInt("y")
                    positions[playerId] = Pair(x, y)
                }
                updateRemotePlayerPositions(positions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLocalPlayerPosition(): Pair<Int, Int>? {
        return localPlayerPosition
    }
}
