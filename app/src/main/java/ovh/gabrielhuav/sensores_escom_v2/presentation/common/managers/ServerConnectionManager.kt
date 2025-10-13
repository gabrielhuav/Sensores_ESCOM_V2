package ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider

// ServerConnectionManager.kt
class ServerConnectionManager(
    private val context: Context,
    val onlineServerManager: OnlineServerManager
) {
    private val serverUrl = "ws://192.168.1.111:3000"
    private val serverUrl = "ws://192.168.1.13:3000"
    private var isConnecting = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun connectToServer(callback: (Boolean) -> Unit) {
        if (isConnecting) return

        isConnecting = true

        try {
            onlineServerManager.apply {
                // Primero, asegurarse de que no hay una conexión existente
                disconnectFromServer()

                // Configurar listeners
                setOnConnectionCompleteListener {
                    isConnecting = false
                    Log.d(TAG, "Conexión al servidor completada")
                    sendJoinMessage(context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
                        .getString("player_name", "") ?: "")
                    // Asegurar que el callback se ejecute en el hilo principal
                    mainHandler.post {
                        callback(true)
                    }
                }

                setOnConnectionFailedListener {
                    isConnecting = false
                    Log.e(TAG, "Falló la conexión al servidor")
                    // Asegurar que el callback se ejecute en el hilo principal
                    mainHandler.post {
                        callback(false)
                    }
                }

                // Intentar conectar
                connectToServer(serverUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar al servidor: ${e.message}")
            isConnecting = false
            // Asegurar que el callback se ejecute en el hilo principal
            mainHandler.post {
                callback(false)
            }
        }
    }

    // Método para verificar la conexión
    fun isConnected(): Boolean {
        return onlineServerManager.isWebSocketConnected()
    }

    fun sendUpdateMessage(playerId: String, position: Pair<Int, Int>, map: String) {
        try {
            // Asegúrate de que 'map' tiene un valor válido
            val mapToSend = if (map.isBlank()) MapMatrixProvider.Companion.MAP_MAIN else map

            onlineServerManager.sendUpdateMessage(
                playerId = playerId,
                x = position.first,
                y = position.second,
                map = mapToSend
            )
            Log.d(TAG, "Mensaje de actualización enviado: Player=$playerId, Pos=$position, Map=$mapToSend")
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar actualización: ${e.message}")
        }
    }

    fun sendOSMPositionUpdate(playerId: String, latitude: Double, longitude: Double) {
        try {
            val message = JSONObject().apply {
                put("type", "update")
                put("id", playerId)
                put("lat", latitude)
                put("lon", longitude)
                put("x", (latitude * 1000000).toInt()) // For compatibility with grid system
                put("y", (longitude * 1000000).toInt())
                put("map", "osm")
                put("currentmap", "osm_real_world")
            }
            onlineServerManager.send(message.toString())
            Log.d(TAG, "OSM position update sent: Player=$playerId, Lat=$latitude, Lon=$longitude")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending OSM position update: ${e.message}")
        }
    }


    fun disconnect() {
        onlineServerManager.disconnectFromServer()
    }


    companion object {
        private const val TAG = "ServerConnectionManager"
    }
}