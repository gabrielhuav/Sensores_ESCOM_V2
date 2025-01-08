package ovh.gabrielhuav.sensores_escom_v2.data.map

import android.bluetooth.BluetoothDevice
import android.util.Log
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import java.util.concurrent.ConcurrentHashMap

class BluetoothWebSocketBridge private constructor() {
    private var onlineServerManager: OnlineServerManager? = null
    private var localPlayerId: String = ""
    private val connectedClients = ConcurrentHashMap<String, BluetoothDevice>()
    private val playerPositions = ConcurrentHashMap<String, Pair<Int, Int>>()
    private var hasInternetConnection = true

    // Listener para actualizar la UI cuando hay cambios en las posiciones
    interface PositionUpdateListener {
        fun onPositionUpdated(playerId: String, position: Pair<Int, Int>?)
    }

    private var positionUpdateListener: PositionUpdateListener? = null

    fun setPositionUpdateListener(listener: PositionUpdateListener) {
        positionUpdateListener = listener
    }

    fun initialize(serverManager: OnlineServerManager, playerId: String) {
        onlineServerManager = serverManager
        localPlayerId = playerId

        // Verificar si ya hay una posición para el jugador
        if (!playerPositions.containsKey(localPlayerId)) {
            updatePosition(localPlayerId, Pair(1, 1)) // Solo establecer la posición inicial si no existe
        } else {
            Log.d(TAG, "La posición inicial ya está establecida para $localPlayerId: ${playerPositions[localPlayerId]}")
        }
    }


    fun synchronizeAllPositions() {
        playerPositions.forEach { (playerId, position) ->
            onlineServerManager?.sendBothPositions(
                playerId,
                position.first,
                position.second,
                null, null, // No hay posiciones remotas específicas en este caso
                "main"
            )
        }
    }

    fun updateRemoteAndLocalPositions(localPosition: Pair<Int, Int>, remotePosition: Pair<Int, Int>?) {
        // Actualiza las posiciones locales
        updatePosition(localPlayerId, localPosition)

        // Si hay una posición remota, actualiza también
        remotePosition?.let {
            updatePosition("remote_$localPlayerId", it)
        }

        // Envía al servidor Node.js
        onlineServerManager?.sendBothPositions(
            localPlayerId, localPosition.first, localPosition.second,
            remotePosition?.first, remotePosition?.second,
            "main"
        )
    }

    fun setInternetConnectionStatus(hasConnection: Boolean) {
        hasInternetConnection = hasConnection
        if (hasConnection) {
            // Sincronizar todas las posiciones almacenadas cuando se recupera la conexión
            synchronizePositions()
        }
    }

    fun addBluetoothClient(device: BluetoothDevice, deviceName: String) {
        connectedClients[deviceName] = device
    }

    fun removeBluetoothClient(deviceName: String) {
        connectedClients.remove(deviceName)
        playerPositions.remove(deviceName)
    }

    fun updatePosition(playerId: String, position: Pair<Int, Int>) {
        playerPositions[playerId] = position

        // Notificar al listener de la UI
        positionUpdateListener?.onPositionUpdated(playerId, position)

        // Si hay conexión a internet, enviar al servidor WebSocket
        if (hasInternetConnection) {
            onlineServerManager?.sendUpdateMessage(
                playerId,
                position.first,
                position.second,
                "main"
            )
        }

        // Si es un cliente Bluetooth, propagar la actualización localmente
        if (connectedClients.containsKey(playerId)) {
            broadcastPositionUpdate(playerId, position)
        }
    }

    private fun synchronizePositions() {
        playerPositions.forEach { (playerId, position) ->
            onlineServerManager?.sendUpdateMessage(
                playerId,
                position.first,
                position.second,
                "main"
            )
        }
    }

    private fun broadcastPositionUpdate(playerId: String, position: Pair<Int, Int>) {
        val positionData = JSONObject().apply {
            put("type", "position_update")
            put("id", playerId)
            put("x", position.first)
            put("y", position.second)
        }

        // Propagar a todos los clientes Bluetooth conectados
        connectedClients.forEach { (_, device) ->
            // Aquí iría la lógica para enviar a través de Bluetooth
            // Usando el BluetoothGameManager existente
        }
    }

    fun handleWebSocketMessage(message: String) {
        try {
            val jsonObject = JSONObject(message)
            when (jsonObject.getString("type")) {
                "positions" -> {
                    val players = jsonObject.getJSONObject("players")
                    players.keys().forEach { playerId ->
                        if (playerId != localPlayerId && !connectedClients.containsKey(playerId)) {
                            val position = players.getJSONObject(playerId)
                            val x = position.getInt("x")
                            val y = position.getInt("y")
                            updatePosition(playerId, Pair(x, y))
                        }
                    }
                }
                "disconnect" -> {
                    val disconnectedId = jsonObject.getString("id")
                    playerPositions.remove(disconnectedId)
                    positionUpdateListener?.onPositionUpdated(disconnectedId, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing WebSocket message: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BluetoothWebSocketBridge"

        @Volatile
        private var instance: BluetoothWebSocketBridge? = null

        fun getInstance(): BluetoothWebSocketBridge {
            return instance ?: synchronized(this) {
                instance ?: BluetoothWebSocketBridge().also { instance = it }
            }
        }
    }
}