package ovh.gabrielhuav.sensores_escom_v2.presentation.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager

class DeviceListActivity : AppCompatActivity(), BluetoothGameManager.ConnectionListener {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private val deviceList = mutableListOf<String>()
    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        // Initialize views
        deviceListView = findViewById(R.id.deviceListView)
        val scanButton = findViewById<Button>(R.id.btnScanDevices)
        val backToMenuButton = findViewById<Button>(R.id.btnBackToMenu)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get player name from intent
        val playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Nombre de jugador no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize BluetoothGameManager with context
        BluetoothGameManager.getInstance(applicationContext).apply {
            setConnectionListener(this@DeviceListActivity)
            initialize(playerName)
        }

        checkAndRequestPermissions()
        showPairedDevices()

        // Check if device is Bluetooth server or client
        val isServer = intent.getBooleanExtra("IS_SERVER", false)

        // Event setup
        scanButton.setOnClickListener {
            if (hasRequiredPermissions()) {
                startDiscovery()
            } else {
                checkAndRequestPermissions()
            }
        }

        backToMenuButton.setOnClickListener {
            navigateToMainMenu()
        }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            val deviceInfo = deviceList[position]
            val device = deviceMap[deviceInfo]
            if (device != null) {
                connectToDevice(device)
            }
        }

        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(receiver, filter)
    }
    private fun connectToDevice(device: BluetoothDevice) {
        val isServer = intent.getBooleanExtra("IS_SERVER", false)

        if (isServer) {
            BluetoothGameManager.getInstance(applicationContext).startServer() // Pasando el contexto
            launchGameplayActivity(isServer = true)
        } else {
            BluetoothGameManager.getInstance(applicationContext).connectToDevice(device) // Pasando el contexto
            BluetoothGameManager.getInstance(applicationContext).setConnectionListener(object :
                BluetoothGameManager.ConnectionListener {
                override fun onDeviceConnected(device: BluetoothDevice) {
                    runOnUiThread {
                        Toast.makeText(
                            this@DeviceListActivity,
                            "Conexión exitosa con ${device.name ?: "Dispositivo desconocido"}",
                            Toast.LENGTH_SHORT
                        ).show()
                        launchGameplayActivity(isServer = false, selectedDevice = device)
                    }
                }

                override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
                    // Manejado en GameplayActivity
                }

                override fun onConnectionComplete() {
                    // Conexión completa, manejada en `onDeviceConnected`
                }

                override fun onConnectionFailed(message: String) {
                    runOnUiThread {
                        Toast.makeText(
                            this@DeviceListActivity,
                            "Error al conectar: $message",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        }
    }

    // Add required interface implementations
    override fun onDeviceConnected(device: BluetoothDevice) {
        runOnUiThread {
            Toast.makeText(this, "Conectado a ${device.name ?: "Dispositivo desconocido"}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        // Not needed in DeviceListActivity
    }

    override fun onConnectionComplete() {
        runOnUiThread {
            Toast.makeText(this, "Conexión establecida", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailed(message: String) {
        runOnUiThread {
            Toast.makeText(this, "Error de conexión: $message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGameplayActivity(isServer: Boolean, selectedDevice: BluetoothDevice? = null) {
        val playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"
        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            if (!isServer) {
                putExtra("SELECTED_DEVICE", selectedDevice)
            }
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToMainMenu() {
        finish()
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        }
    }

    private fun showPairedDevices() {
        if (!hasRequiredPermissions()) return

        val pairedDevices = bluetoothAdapter.bondedDevices
        deviceList.clear()
        pairedDevices?.forEach { device ->
            val deviceInfo = getDeviceDisplayName(device)
            if (!deviceList.contains(deviceInfo)) {
                deviceList.add(deviceInfo)
                deviceMap[deviceInfo] = device
            }
        }
        updateDeviceList()
    }

    private fun startDiscovery() {
        if (!hasRequiredPermissions()) return

        deviceList.clear()
        showPairedDevices()

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter.startDiscovery()
        Toast.makeText(this, "Buscando dispositivos...", Toast.LENGTH_SHORT).show()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceInfo = getDeviceDisplayName(it)
                        if (!deviceList.contains(deviceInfo)) {
                            deviceList.add(deviceInfo)
                            deviceMap[deviceInfo] = it
                            updateDeviceList()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Toast.makeText(context, "Búsqueda finalizada.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getDeviceDisplayName(device: BluetoothDevice): String {
        val deviceName = if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            device.name ?: "Dispositivo Desconocido"
        } else {
            "Permiso no otorgado"
        }
        return "$deviceName\n${device.address}"
    }

    private fun updateDeviceList() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
        deviceListView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        if (hasRequiredPermissions() && bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        unregisterReceiver(receiver)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
