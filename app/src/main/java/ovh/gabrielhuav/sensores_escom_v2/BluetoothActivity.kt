package ovh.gabrielhuav.sensores_escom_v2

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class BluetoothActivity : AppCompatActivity(), BluetoothGameManager.ConnectionListener {

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private lateinit var btnStartServer: Button
    private lateinit var btnConnectDevice: Button
    private lateinit var btnBackToMenu: Button
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var mapContainer: FrameLayout
    private lateinit var mapView: MapView

    private var isConnected = false
    private var isServer = false
    private var isAttemptingConnection = false
    private val connectionTimeout = 60000L // 1 minuto
    private val handler = Handler(Looper.getMainLooper())

    // Launcher para manejar la solicitud de habilitar Bluetooth
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth habilitado.", Toast.LENGTH_SHORT).show()
                checkPermissions()
            } else {
                Toast.makeText(this, "Bluetooth no fue habilitado.", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher para manejar la selección de dispositivo
    private val selectDeviceLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val device: BluetoothDevice? = result.data?.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE)
                device?.let {
                    connectToDevice(it)
                }
            } else {
                updateBluetoothStatus("Selección de dispositivo cancelada.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        // Inicializar componentes UI
        btnStartServer = findViewById(R.id.btnStartServer)
        btnConnectDevice = findViewById(R.id.btnConnectToDevice)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        mapContainer = findViewById(R.id.mapContainer)

        // Crear y agregar MapView al contenedor
        mapView = MapView(this)
        mapContainer.addView(mapView)

        // Configuración inicial
        setupButtonListeners()
        checkBluetoothSupport()

        // Establecer contexto para BluetoothGameManager
        BluetoothGameManager.appContext = applicationContext
        BluetoothGameManager.getInstance().setConnectionListener(this)
    }

    private fun setupButtonListeners() {
        btnStartServer.setOnClickListener {
            startServer()
        }

        btnConnectDevice.setOnClickListener {
            // Usar el nuevo launcher
            val intent = Intent(this, DeviceListActivity::class.java)
            selectDeviceLauncher.launch(intent)
        }

        btnBackToMenu.setOnClickListener {
            navigateToMainMenu()
        }
    }

    private fun navigateToMainMenu() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun checkBluetoothSupport() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
            finish()
        } else if (!bluetoothAdapter!!.isEnabled) {
            requestEnableBluetooth()
        } else {
            checkPermissions()
        }
    }

    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ), REQUEST_BLUETOOTH_PERMISSIONS
                )
                return
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
                )
                return
            }
        }
    }

    private fun startServer() {
        if (!checkPermissionsGranted()) {
            checkPermissions()
            Toast.makeText(this, "Faltan permisos para iniciar el servidor", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            updateBluetoothStatus("Iniciando servidor Bluetooth...")
            BluetoothGameManager.getInstance().startServer()
            isServer = true
            updateBluetoothStatus("Servidor iniciado. Esperando conexión...")
            Toast.makeText(this, "Servidor iniciado", Toast.LENGTH_SHORT).show()

            handler.postDelayed({
                if (!isConnected) {
                    updateBluetoothStatus("Conexión fallida. Tiempo agotado.")
                }
            }, connectionTimeout)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar el servidor", e)
            updateBluetoothStatus("Error al iniciar el servidor: ${e.message}")
            Toast.makeText(this, "Error al iniciar el servidor: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (isAttemptingConnection) return

        isAttemptingConnection = true
        val deviceName = getDeviceName(device)

        updateBluetoothStatus("Intentando conectar a $deviceName...")
        Toast.makeText(this, "Conectando a $deviceName...", Toast.LENGTH_SHORT).show()

        BluetoothGameManager.getInstance().connectToDevice(device)

        handler.postDelayed({
            if (isAttemptingConnection && !isConnected) {
                isAttemptingConnection = false
                updateBluetoothStatus("Conexión fallida: tiempo agotado.")
                Toast.makeText(this, "No se pudo conectar al dispositivo.", Toast.LENGTH_SHORT).show()
            }
        }, connectionTimeout)
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            device.name ?: "Dispositivo Desconocido"
        } else {
            "Permiso no otorgado"
        }
    }

    private fun checkPermissionsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun updateBluetoothStatus(status: String) {
        runOnUiThread {
            tvBluetoothStatus.text = status
            Log.d(TAG, "Status: $status")
        }
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        isAttemptingConnection = false
        isConnected = true
        val deviceName = getDeviceName(device)

        updateBluetoothStatus("Conectado a: $deviceName")
        Toast.makeText(this, "Conectado a $deviceName", Toast.LENGTH_SHORT).show()
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            mapView.updateRemotePlayerPosition(Pair(x, y))
            updateBluetoothStatus("Posición recibida: ($x, $y)")
        }
    }

    override fun onConnectionComplete() {
        runOnUiThread {
            updateBluetoothStatus("Conexión establecida completamente.")
            Toast.makeText(this, "Conexión establecida", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailed(message: String) {
        isAttemptingConnection = false
        isConnected = false
        runOnUiThread {
            updateBluetoothStatus("Conexión fallida: $message")
            Toast.makeText(this, "Error al conectar: $message", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startServer()
        } else {
            Toast.makeText(this, "Permisos denegados.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "BluetoothActivity"
        const val REQUEST_CONNECT_DEVICE = 1
        const val REQUEST_BLUETOOTH_PERMISSIONS = 101
        const val REQUEST_LOCATION_PERMISSION = 102
    }
}