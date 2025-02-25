package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.content.Context
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import android.util.Log

// ServerConnectionManager.kt
class ServerConnectionManager(
    private val context: Context,
    val onlineServerManager: OnlineServerManager
) {
    private val serverUrl = "ws://10.3.56.106:3000"
    private var isConnecting = false

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
                    callback(true)
                }

                setOnConnectionFailedListener {
                    isConnecting = false
                    Log.e(TAG, "Falló la conexión al servidor")
                    callback(false)
                }

                // Intentar conectar
                connectToServer(serverUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar al servidor: ${e.message}")
            isConnecting = false
            callback(false)
        }
    }

    fun sendUpdateMessage(playerId: String, position: Pair<Int, Int>, map: String) {
        try {
            onlineServerManager.sendUpdateMessage(
                playerId = playerId,
                x = position.first,
                y = position.second,
                map = map
            )
            Log.d(TAG, "Mensaje de actualización enviado: Player=$playerId, Pos=$position")
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar actualización: ${e.message}")
        }
    }

    fun disconnect() {
        onlineServerManager.disconnectFromServer()
    }

    companion object {
        private const val TAG = "ServerConnectionManager"
    }
}
