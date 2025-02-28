package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager

class BluetoothManager private constructor(
    private val activity: AppCompatActivity,
    private val statusTextView: TextView
) {

    interface BluetoothManagerCallback {
        fun onBluetoothDeviceConnected(device: BluetoothDevice)
        fun onBluetoothConnectionFailed(error: String)
    }

    private var callback: BluetoothManagerCallback? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private val handler = Handler(Looper.getMainLooper())

    private var isServer = false
    private var connectedDevice: BluetoothDevice? = null
    private var connectionState = ConnectionState.DISCONNECTED

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    companion object {
        private const val TAG = "BluetoothManager"
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 101

        @Volatile
        private var instance: BluetoothManager? = null

        fun getInstance(activity: AppCompatActivity, statusTextView: TextView): BluetoothManager {
            return instance ?: synchronized(this) {
                instance ?: BluetoothManager(activity, statusTextView).also { instance = it }
            }
        }
    }

    fun setCallback(callback: BluetoothManagerCallback) {
        this.callback = callback
    }

    fun checkBluetoothSupport(enableBluetoothLauncher: ActivityResultLauncher<Intent>) {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth no está disponible en este dispositivo.")
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            requestEnableBluetooth(enableBluetoothLauncher)
        } else {
            checkPermissions()
        }
    }

    private fun requestEnableBluetooth(enableBluetoothLauncher: ActivityResultLauncher<Intent>) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    fun startServer() {
        if (!hasRequiredPermissions()) {
            showToast("Se requieren permisos de Bluetooth")
            return
        }

        try {
            isServer = true
            connectionState = ConnectionState.CONNECTING
            updateStatus("Iniciando servidor Bluetooth...")

            BluetoothGameManager.getInstance(activity).apply {
                setConnectionListener(createConnectionListener())
                startServer()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting server: ${e.message}")
            handleError("Error al iniciar servidor: ${e.message}")
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (!hasRequiredPermissions()) {
            showToast("Se requieren permisos de Bluetooth")
            return
        }

        try {
            isServer = false
            connectionState = ConnectionState.CONNECTING
            connectedDevice = device

            // Usar handler para actualizar UI
            handler.post {
                updateStatus("Conectando a ${device.name}...")
            }

            BluetoothGameManager.getInstance(activity).apply {
                setConnectionListener(createConnectionListener())
                connectToDevice(device)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${e.message}")
            handleError("Error al conectar: ${e.message}")
        }
    }

    private fun createConnectionListener(): BluetoothGameManager.ConnectionListener {
        return object : BluetoothGameManager.ConnectionListener {
            override fun onDeviceConnected(device: BluetoothDevice) {
                handler.post {
                    connectionState = ConnectionState.CONNECTED
                    connectedDevice = device
                    updateStatus("Conectado a ${device.name}")
                    callback?.onBluetoothDeviceConnected(device)
                }
            }

            override fun onConnectionComplete() {
                handler.post {
                    connectionState = ConnectionState.CONNECTED
                    updateStatus("Conexión establecida completamente")
                }
            }

            override fun onConnectionFailed(message: String) {
                handler.post {
                    connectionState = ConnectionState.DISCONNECTED
                    handleError(message)
                }
            }

            override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
                // Manejado por el GameplayActivity
            }
        }
    }

    private fun checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!hasRequiredPermissions()) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateStatus(message: String) {
        // Asegurarse de que la actualización de UI ocurra en el hilo principal
        handler.post {
            statusTextView.text = message
            Log.d(TAG, message)
        }
    }

    private fun handleError(message: String) {
        handler.post {
            updateStatus("Error: $message")
            callback?.onBluetoothConnectionFailed(message)
            Log.e(TAG, message)
        }
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun getConnectionState(): ConnectionState {
        return connectionState
    }

    fun getConnectedDevice(): BluetoothDevice? {
        return connectedDevice
    }

    fun isServer(): Boolean {
        return isServer
    }

    fun cleanup() {
        try {
            BluetoothGameManager.getInstance(activity).stopConnection()
            connectionState = ConnectionState.DISCONNECTED
            connectedDevice = null
            callback = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }

    fun reconnect() {
        when {
            connectionState == ConnectionState.CONNECTED -> return // Ya conectado
            isServer -> startServer()
            connectedDevice != null -> connectToDevice(connectedDevice!!)
        }
    }
}