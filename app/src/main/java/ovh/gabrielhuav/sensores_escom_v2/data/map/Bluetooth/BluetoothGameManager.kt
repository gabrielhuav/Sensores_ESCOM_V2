package ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class BluetoothGameManager private constructor(private val context: Context) {
    private var serverSocket: BluetoothServerSocket? = null
    private val clientSockets = mutableListOf<BluetoothSocket>()
    private val inputStreams = mutableListOf<InputStream>()
    private val outputStreams = mutableListOf<OutputStream>()

    private val connectedDevices = CopyOnWriteArrayList<BluetoothDevice>()
    private var connectionListener: ConnectionListener? = null
    private val handler = Handler(Looper.getMainLooper())

    private var playerName: String = ""
    private lateinit var onlineServerManager: OnlineServerManager
    private var localPlayerPosition: Pair<Int, Int> = Pair(0, 0)

    private var isConnectionActive = true
    private var reconnectionAttempts = 0
    private val MAX_RECONNECTION_ATTEMPTS = 5

    private var isServer = false

    // Elimina el init block anterior y usa esta función
    fun initialize(playerName: String, listener: OnlineServerManager.WebSocketListener? = null) {
        this.playerName = playerName
        onlineServerManager = OnlineServerManager.getInstance(context)
    }

    interface ConnectionListener {
        fun onDeviceConnected(device: BluetoothDevice)
        fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int)
        fun onConnectionComplete()
        fun onConnectionFailed(message: String)
    }

    fun setConnectionListener(listener: ConnectionListener) {
        connectionListener = listener
    }

    fun startServer() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN) || !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG, "Faltan permisos de Bluetooth para iniciar el servidor.")
            handler.post { connectionListener?.onConnectionFailed("Faltan permisos para iniciar el servidor.") }
            return
        }

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth no es compatible con este dispositivo.")
            handler.post { connectionListener?.onConnectionFailed("Bluetooth no es compatible con este dispositivo.") }
            return
        }

        Thread {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
                Log.d(TAG, "Esperando conexión de cliente...")
                while (true) {
                    try {
                        val socket = serverSocket!!.accept()
                        val remoteDevice = socket.remoteDevice
                        Log.d(TAG, "Cliente conectado, configurando streams...")
                        setupStreams(socket)

                        if (outputStreams.isEmpty()) {
                            Log.e(TAG, "Streams no configurados correctamente después de setupStreams")
                        } else {
                            Log.d(TAG, "Streams configurados correctamente, cantidad: ${outputStreams.size}")
                        }

                        connectedDevices.add(remoteDevice)
                        handler.post {
                            connectionListener?.onDeviceConnected(remoteDevice)
                            connectionListener?.onConnectionComplete()
                        }

                        receiveData(socket) { data ->
                            processReceivedData(data, remoteDevice, localPlayerPosition)
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Error aceptando conexión: ${e.message}")
                        break
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error en servidor socket: ${e.message}")
            }
        }.start()

    }

    fun connectToDevice(device: BluetoothDevice) {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG, "Faltan permisos de Bluetooth")
            return
        }

        isServer = false // Añadir esta línea

        Thread {
            try {
                Log.d(TAG, "Iniciando conexión a ${getDeviceName(device)}...")
                val socket = device.createRfcommSocketToServiceRecord(MY_UUID)

                Log.d(TAG, "Socket creado, intentando conectar...")
                socket.connect()

                Log.d(TAG, "Socket conectado, configurando streams...")
                setupStreams(socket)

                if (outputStreams.isEmpty()) {
                    Log.e(TAG, "Streams no configurados correctamente después de conectar")
                    throw IOException("Fallo en configuración de streams")
                }

                Log.d(TAG, "Streams configurados correctamente, cantidad: ${outputStreams.size}")

                connectedDevices.add(device)
                handler.post {
                    connectionListener?.onDeviceConnected(device)
                    connectionListener?.onConnectionComplete()
                }

                receiveData(socket) { data ->
                    processReceivedData(data, device, localPlayerPosition)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error conectando: ${e.message}")
                handler.post {
                    connectionListener?.onConnectionFailed("Error conectando a ${getDeviceName(device)}: ${e.message}")
                }
            }
        }.start()
    }

    private fun setupStreams(socket: BluetoothSocket) {
        try {
            Log.d(TAG, "Configurando streams para socket")
            Log.d(TAG, "Estado actual - Sockets: ${clientSockets.size}, InputStreams: ${inputStreams.size}, OutputStreams: ${outputStreams.size}")

            clientSockets.add(socket)
            inputStreams.add(socket.inputStream)
            outputStreams.add(socket.outputStream)

            Log.d(TAG, "Streams configurados - Nuevo estado - Sockets: ${clientSockets.size}, InputStreams: ${inputStreams.size}, OutputStreams: ${outputStreams.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando streams: ${e.message}")
        }
    }

    fun sendPlayerPosition(x: Int, y: Int) {
        if (!isConnectionActive) {
            Log.e(TAG, "Conexión no activa, no se pueden enviar datos")
            return
        }

        try {
            val jsonData = JSONObject().apply {
                put("type", "position")
                put("x", x)
                put("y", y)
            }

            val data = jsonData.toString()
            Log.d(TAG, "Preparando envío de posición - Estado actual:")
            Log.d(TAG, "Sockets conectados: ${clientSockets.size}")
            Log.d(TAG, "OutputStreams disponibles: ${outputStreams.size}")
            Log.d(TAG, "Dispositivos conectados: ${connectedDevices.size}")

            if (outputStreams.isEmpty()) {
                Log.e(TAG, "No hay streams de salida disponibles - Intentando reconexión")
                connectedDevices.forEach { device ->
                    reconnectToDevice(device)
                }
                return
            }


            var streamsClosed = false
            outputStreams.toList().forEachIndexed { index, outputStream ->
                try {
                    outputStream.write((data + "\n").toByteArray())
                    outputStream.flush()
                    Log.d(TAG, "Datos enviados exitosamente")
                } catch (e: IOException) {
                    Log.e(TAG, "Error enviando datos: ${e.message}")
                    streamsClosed = true
                    // Intentar reconectar con el dispositivo correspondiente
                    if (index < connectedDevices.size) {
                        reconnectToDevice(connectedDevices[index])
                    }
                }
            }

            if (streamsClosed) {
                cleanupClosedConnections()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error general enviando posición: ${e.message}")
        }
    }

    private fun cleanupClosedConnections() {
        val iterator = clientSockets.iterator()
        var index = 0
        while (iterator.hasNext()) {
            val socket = iterator.next()
            try {
                socket.outputStream.write(1) // Test si el socket está vivo
            } catch (e: IOException) {
                iterator.remove()
                if (index < inputStreams.size) inputStreams.removeAt(index)
                if (index < outputStreams.size) outputStreams.removeAt(index)
                if (index < connectedDevices.size) connectedDevices.removeAt(index)
            }
            index++
        }
    }

    private fun reconnectToDevice(device: BluetoothDevice) {
        if (reconnectionAttempts >= MAX_RECONNECTION_ATTEMPTS) {
            Log.e(TAG, "Máximo número de intentos de reconexión alcanzado")
            handler.post { connectionListener?.onConnectionFailed("No se pudo restablecer la conexión") }
            return
        }

        Log.d(TAG, "Intentando reconexión ${reconnectionAttempts + 1}/$MAX_RECONNECTION_ATTEMPTS")

        Thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                socket.connect()

                // Limpiar streams antiguos
                clearOldConnections()

                // Configurar nuevos streams
                setupStreams(socket)

                reconnectionAttempts = 0
                Log.d(TAG, "Reconexión exitosa")

                // Reiniciar la recepción de datos
                receiveData(socket) { data ->
                    processReceivedData(data, device, localPlayerPosition)
                }

            } catch (e: IOException) {
                Log.e(TAG, "Error en reconexión: ${e.message}")
                reconnectionAttempts++
                handler.postDelayed({ reconnectToDevice(device) }, 5000) // Esperar 5 segundos antes de reintentar
            }
        }.start()
    }

    private fun clearOldConnections() {
        clientSockets.forEach { it.close() }
        clientSockets.clear()
        inputStreams.clear()
        outputStreams.clear()
    }


    private fun receiveData(socket: BluetoothSocket, callback: (String) -> Unit) {
        Thread {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))

            while (isConnectionActive) {
                try {
                    val receivedData = reader.readLine()
                    if (receivedData == null) {
                        Log.e(TAG, "Conexión cerrada por el otro extremo")
                        break
                    }

                    Log.d(TAG, "Datos recibidos: $receivedData")
                    handler.post { callback(receivedData) }

                } catch (e: IOException) {
                    Log.e(TAG, "Error leyendo datos: ${e.message}")
                    val device = socket.remoteDevice
                    if (isConnectionActive) {
                        reconnectToDevice(device)
                    }
                    break
                }
            }
        }.start()
    }

    fun stopConnection() {
        isConnectionActive = false
        clearOldConnections()
    }

    fun resumeConnection() {
        isConnectionActive = true
        reconnectionAttempts = 0
        // Reconectar con dispositivos guardados
        connectedDevices.forEach { device ->
            reconnectToDevice(device)
        }
    }


    private fun startHeartbeat() {
        Thread {
            while (true) {
                try {
                    Thread.sleep(5000) // Cada 5 segundos
                    val heartbeat = JSONObject().apply {
                        put("type", "heartbeat")
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendData(heartbeat.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Error en heartbeat: ${e.message}")
                }
            }
        }.start()
    }

    private fun sendData(data: String) {
        try {
            outputStreams.forEach { outputStream ->
                try {
                    outputStream.write((data + "\n").toByteArray())
                    outputStream.flush()
                } catch (e: IOException) {
                    Log.e(TAG, "Error enviando datos: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en sendData: ${e.message}")
        }
    }

    private val remotePlayerPositions = mutableMapOf<String, Pair<Int, Int>>() // Posiciones remotas

    private fun processReceivedData(data: String, device: BluetoothDevice, localPosition: Pair<Int, Int>) {
        try {
            val jsonData = JSONObject(data)
            val x = jsonData.getInt("x")
            val y = jsonData.getInt("y")

            Log.d(TAG, "Datos recibidos del dispositivo ${device.name}: x=$x, y=$y")

            handler.post {
                // Guardar posición remota
                remotePlayerPositions[device.name ?: "Unknown"] = Pair(x, y)

                // Notificar al listener
                connectionListener?.onPositionReceived(device, x, y)

                // Enviar ambas posiciones
                try {
                    sendBothPositions(localPosition, Pair(x, y))
                } catch (e: Exception) {
                    Log.e(TAG, "Error enviando posiciones: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando datos JSON: ${e.message}")
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }


    private fun getDeviceName(device: BluetoothDevice?): String {
        return if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            device?.name ?: "Dispositivo Desconocido"
        } else {
            "Permiso no otorgado"
        }
    }

    private fun closeServerSocket() {
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error al cerrar el socket del servidor: ${e.message}")
        }
    }

    fun sendBothPositions(localPosition: Pair<Int, Int>, remotePosition: Pair<Int, Int>?) {
        try {
            val data = JSONObject().apply {
                put("type", "update")
                put("id", playerName.trim())
                put("map", "main")
                put("local", JSONObject().apply {
                    put("x", localPosition.first)
                    put("y", localPosition.second)
                })
                remotePosition?.let {
                    put("remote", JSONObject().apply {
                        put("x", it.first)
                        put("y", it.second)
                    })
                }
            }

            Log.d(TAG, "Sending message: $data")
            onlineServerManager.queueMessage(data.toString())
        } catch (e: JSONException) {
            Log.e(TAG, "Error building JSON: ${e.message}")
        }
    }

    fun sendAllPositions(localPosition: Pair<Int, Int>) {
        try {
            val data = JSONObject().apply {
                put("type", "update")
                put("id", playerName.trim())
                put("map", "main")
                put("local", JSONObject().apply {
                    put("x", localPosition.first)
                    put("y", localPosition.second)
                })

                // Agregar todas las posiciones remotas
                val remotesArray = JSONArray()
                remotePlayerPositions.forEach { (id, position) ->
                    val remoteData = JSONObject().apply {
                        put("id", id)
                        put("x", position.first)
                        put("y", position.second)
                    }
                    remotesArray.put(remoteData)
                }
                put("remotes", remotesArray)
            }

            Log.d(TAG, "Sending message with all positions: $data")
            onlineServerManager.queueMessage(data.toString())
        } catch (e: JSONException) {
            Log.e(TAG, "Error building JSON: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BluetoothGameManager"
        private const val NAME = "BluetoothGame"
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        @Volatile
        private var INSTANCE: BluetoothGameManager? = null

        fun getInstance(context: Context): BluetoothGameManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothGameManager(context).also { INSTANCE = it }
            }
        }
    }

}
