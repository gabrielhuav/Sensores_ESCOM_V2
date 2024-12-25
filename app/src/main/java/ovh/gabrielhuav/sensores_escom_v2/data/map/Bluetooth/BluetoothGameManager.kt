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
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class BluetoothGameManager private constructor() {

    private var serverSocket: BluetoothServerSocket? = null
    private val clientSockets = mutableListOf<BluetoothSocket>()
    private val inputStreams = mutableListOf<InputStream>()
    private val outputStreams = mutableListOf<OutputStream>()

    private val connectedDevices = CopyOnWriteArrayList<BluetoothDevice>()
    private var connectionListener: ConnectionListener? = null
    private val handler = Handler(Looper.getMainLooper())

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
                while (true) { // Aceptar múltiples conexiones
                    val socket = serverSocket!!.accept()
                    val remoteDevice = socket.remoteDevice
                    setupStreams(socket)

                    connectedDevices.add(remoteDevice)
                    handler.post {
                        connectionListener?.onDeviceConnected(remoteDevice)
                        connectionListener?.onConnectionComplete()
                    }

                    Log.d(TAG, "Cliente conectado: ${getDeviceName(remoteDevice)}")

                    receiveData(socket) { data ->
                        processReceivedData(data, remoteDevice)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error al iniciar el servidor: ${e.message}")
                handler.post { connectionListener?.onConnectionFailed("Error al iniciar el servidor.") }
            } finally {
                closeServerSocket()
            }
        }.start()
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.e(TAG, "Faltan permisos de Bluetooth para conectarse al dispositivo.")
            handler.post { connectionListener?.onConnectionFailed("Faltan permisos para conectarse al dispositivo.") }
            return
        }

        Thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                Log.d(TAG, "Intentando conectar a ${getDeviceName(device)}...")
                socket.connect()
                setupStreams(socket)

                connectedDevices.add(device)
                handler.post {
                    connectionListener?.onDeviceConnected(device)
                    connectionListener?.onConnectionComplete()
                }

                Log.d(TAG, "Conectado al servidor: ${getDeviceName(device)}")

                receiveData(socket) { data ->
                    processReceivedData(data, device)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error al conectar al dispositivo: ${e.message}")
                handler.post { connectionListener?.onConnectionFailed("Error al conectar a ${getDeviceName(device)}") }
            }
        }.start()
    }

    private fun setupStreams(socket: BluetoothSocket) {
        clientSockets.add(socket)
        inputStreams.add(socket.inputStream)
        outputStreams.add(socket.outputStream)
    }

    fun sendPlayerPosition(x: Int, y: Int) {
        try {
            val positionData = JSONObject().apply {
                put("x", x)
                put("y", y)
            }
            val positionString = positionData.toString()
            outputStreams.forEach { it.write(positionString.toByteArray()) }
        } catch (e: IOException) {
            Log.e(TAG, "Error al enviar datos de posición: ${e.message}")
        }
    }

    private fun receiveData(socket: BluetoothSocket, callback: (String) -> Unit) {
        Thread {
            try {
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)
                while (true) {
                    val bytes = inputStream.read(buffer)
                    if (bytes == -1) break
                    val receivedData = String(buffer, 0, bytes)
                    callback(receivedData)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error al recibir datos: ${e.message}")
            }
        }.start()
    }

    private fun processReceivedData(data: String, device: BluetoothDevice) {
        try {
            val jsonData = JSONObject(data)
            val x = jsonData.getInt("x")
            val y = jsonData.getInt("y")
            handler.post {
                connectionListener?.onPositionReceived(device, x, y)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando datos JSON: ${e.message}")
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(appContext!!, permission) == PackageManager.PERMISSION_GRANTED
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

    companion object {
        private const val TAG = "BluetoothGameManager"
        private const val NAME = "BluetoothGame"
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var appContext: Context? = null

        @Volatile
        private var INSTANCE: BluetoothGameManager? = null

        fun getInstance(): BluetoothGameManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothGameManager().also { INSTANCE = it }
            }
        }

        fun initialize(context: Context) {
            appContext = context.applicationContext
        }
    }
}
