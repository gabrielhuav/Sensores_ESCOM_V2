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
    private lateinit var btnNorth: Button
    private lateinit var btnSouth: Button
    private lateinit var btnEast: Button
    private lateinit var btnWest: Button
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var tvPlayerPosition: TextView
    private lateinit var mapContainer: FrameLayout
    private lateinit var mapView: MapView

    private var isConnected = false
    private var isAttemptingConnection = false
    private val connectionTimeout = 60000L // 1 minuto
    private val handler = Handler(Looper.getMainLooper())

    // Posiciones iniciales de los jugadores
    private var localPlayerPosition = Pair(5, 5)
    private var remotePlayerPosition: Pair<Int, Int>? = null

    // Launcher para habilitar Bluetooth
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth habilitado.", Toast.LENGTH_SHORT).show()
                checkPermissions()
            } else {
                Toast.makeText(this, "Bluetooth no fue habilitado.", Toast.LENGTH_SHORT).show()
            }
        }

    // Launcher para seleccionar dispositivo
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

        // Inicializar componentes
        btnStartServer = findViewById(R.id.btnStartServer)
        btnConnectDevice = findViewById(R.id.btnConnectToDevice)
        btnBackToMenu = findViewById(R.id.btnBackToMenu)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        tvPlayerPosition = findViewById(R.id.tvPlayerPosition)
        mapContainer = findViewById(R.id.mapContainer)
        btnNorth = findViewById(R.id.btnNorth)
        btnSouth = findViewById(R.id.btnSouth)
        btnEast = findViewById(R.id.btnEast)
        btnWest = findViewById(R.id.btnWest)

        mapView = MapView(this)
        mapContainer.addView(mapView)

        setupButtonListeners()
        checkBluetoothSupport()

        // Configurar BluetoothGameManager
        BluetoothGameManager.appContext = applicationContext
        BluetoothGameManager.getInstance().setConnectionListener(this)

        // Dibujar posición inicial del jugador local
        mapView.updateLocalPlayerPosition(localPlayerPosition)
    }

    private fun setupButtonListeners() {
        btnStartServer.setOnClickListener { startServer() }
        btnConnectDevice.setOnClickListener {
            val intent = Intent(this, DeviceListActivity::class.java)
            selectDeviceLauncher.launch(intent)
        }
        btnBackToMenu.setOnClickListener { navigateToMainMenu() }
        setupMovementButtons()
    }

    private fun setupMovementButtons() {
        btnNorth.setOnClickListener { movePlayer(0, -1) }
        btnSouth.setOnClickListener { movePlayer(0, 1) }
        btnEast.setOnClickListener { movePlayer(1, 0) }
        btnWest.setOnClickListener { movePlayer(-1, 0) }
    }

    private fun movePlayer(deltaX: Int, deltaY: Int) {
        val newX = (localPlayerPosition.first + deltaX).coerceIn(0, 9)
        val newY = (localPlayerPosition.second + deltaY).coerceIn(0, 9)

        localPlayerPosition = Pair(newX, newY)
        mapView.updateLocalPlayerPosition(localPlayerPosition)
        tvPlayerPosition.text = "Posición: (${localPlayerPosition.first}, ${localPlayerPosition.second})"

        // Enviar posición al jugador remoto
        if (isConnected) {
            BluetoothGameManager.getInstance().sendPlayerPosition(newX, newY)
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
            if (!hasRequiredPermissions()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSIONS
                )
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    private fun startServer() {
        if (!hasRequiredPermissions()) {
            checkPermissions()
            return
        }
        BluetoothGameManager.getInstance().startServer()
        updateBluetoothStatus("Servidor iniciado. Esperando conexión...")
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (!hasRequiredPermissions()) {
            checkPermissions()
            return
        }
        BluetoothGameManager.getInstance().connectToDevice(device)
        updateBluetoothStatus("Intentando conectar a ${device.name ?: "Desconocido"}...")
    }

    private fun updateBluetoothStatus(status: String) {
        tvBluetoothStatus.text = status
        Log.d(TAG, status)
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        isConnected = true
        updateBluetoothStatus("Conectado a ${device.name ?: "Desconocido"}")
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        remotePlayerPosition = Pair(x, y)
        mapView.updateRemotePlayerPosition(remotePlayerPosition)
    }

    override fun onConnectionComplete() {
        runOnUiThread {
            updateBluetoothStatus("Conexión establecida completamente.")
            mapView.updateLocalPlayerPosition(localPlayerPosition)
            mapView.updateRemotePlayerPosition(Pair(5, 5)) // Posición inicial del otro jugador
        }
    }

    override fun onConnectionFailed(message: String) {
        isConnected = false
        updateBluetoothStatus("Conexión fallida: $message")
    }

    companion object {
        const val TAG = "BluetoothActivity"
        const val REQUEST_BLUETOOTH_PERMISSIONS = 101
    }
}
