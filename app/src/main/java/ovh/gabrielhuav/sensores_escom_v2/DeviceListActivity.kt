package ovh.gabrielhuav.sensores_escom_v2

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

class DeviceListActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListView: ListView
    private val deviceList = mutableListOf<String>()
    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        deviceListView = findViewById(R.id.deviceListView)
        val scanButton = findViewById<Button>(R.id.btnScanDevices)
        val backToMenuButton = findViewById<Button>(R.id.btnBackToMenu)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Verificar y solicitar permisos
        checkAndRequestPermissions()

        // Mostrar dispositivos emparejados
        showPairedDevices()

        // Configuración de eventos
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
                val intent = Intent()
                intent.putExtra(EXTRA_DEVICE, device)
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        // Registrar receptor de broadcast
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(receiver, filter)
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
        const val EXTRA_DEVICE = "device"
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

}