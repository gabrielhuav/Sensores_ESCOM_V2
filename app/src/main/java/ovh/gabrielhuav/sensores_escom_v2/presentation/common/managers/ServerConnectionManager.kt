package ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// ServerConnectionManager.kt
class ServerConnectionManager(
    private val context: Context,
    val onlineServerManager: OnlineServerManager
) {
    private val serverUrl = "ws://10.100.72.87:3000"
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

    /**
     * Registra la asistencia del estudiante en el servidor
     * @param phoneID ID del dispositivo del estudiante
     * @param fullName Nombre completo del estudiante
     * @param group Grupo al que pertenece el estudiante
     * @param callback Callback que devuelve true si la asistencia se registró correctamente, false en caso contrario
     */
    fun registerAttendance(
        phoneID: String,
        fullName: String,
        group: String,
        callback: (Boolean, String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://172.24.115.49:3000/attendance")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                // Crear el JSON con los datos de asistencia
                val jsonData = JSONObject().apply {
                    put("phoneID", phoneID)
                    put("fullName", fullName)
                    put("group", group)
                }

                // Enviar los datos
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(jsonData.toString())
                outputStream.flush()
                outputStream.close()

                // Leer la respuesta
                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_CREATED || 
                                   responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                }

                connection.disconnect()

                // Procesar la respuesta
                withContext(Dispatchers.Main) {
                    if (responseCode == HttpURLConnection.HTTP_CREATED || 
                        responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d(TAG, "Asistencia registrada correctamente: $response")
                        callback(true, "Asistencia registrada correctamente")
                    } else if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                        Log.w(TAG, "Ya se registró la asistencia hoy: $response")
                        callback(false, "Ya registraste tu asistencia hoy")
                    } else {
                        Log.e(TAG, "Error al registrar asistencia: $responseCode - $response")
                        callback(false, "Error al registrar asistencia")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión al registrar asistencia: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Error de conexión: ${e.message}")
                }
            }
        }
    }


    companion object {
        private const val TAG = "ServerConnectionManager"
    }
}