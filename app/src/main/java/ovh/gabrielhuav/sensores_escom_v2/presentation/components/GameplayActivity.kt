package ovh.gabrielhuav.sensores_escom_v2.presentation.components

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
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager

class GameplayActivity : AppCompatActivity(), BluetoothGameManager.ConnectionListener, OnlineServerManager.WebSocketListener {

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private lateinit var btnStartServer: Button
    private lateinit var btnConnectDevice: Button
    private lateinit var btnNorth: Button
    private lateinit var btnSouth: Button
    private lateinit var btnEast: Button
    private lateinit var btnWest: Button
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var btnOnlineServer: Button
    private lateinit var playerName: String
    private lateinit var mapContainer: FrameLayout
    private lateinit var mapView: MapView

    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())

    private var localPlayerPosition = Pair(0, 0)
    private var remotePlayerPosition: Pair<Int, Int>? = null

    private lateinit var onlineServerManager: OnlineServerManager

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
        setContentView(R.layout.activity_gameplay)

        // Inicializar componentes
        btnStartServer = findViewById(R.id.button_small_1)
        btnConnectDevice = findViewById(R.id.button_small_2)
        btnNorth = findViewById(R.id.button_north)
        btnSouth = findViewById(R.id.button_south)
        btnEast = findViewById(R.id.button_east)
        btnWest = findViewById(R.id.button_west)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        btnOnlineServer = findViewById(R.id.button_serverOnline)

        mapContainer = findViewById(R.id.map_container)

        mapView = MapView(this)
        mapContainer.addView(mapView)



        setupButtonListeners()
        checkBluetoothSupport()


        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"
        Toast.makeText(this, "Bienvenido, $playerName", Toast.LENGTH_SHORT).show()

        // Configurar BluetoothGameManager
        BluetoothGameManager.appContext = applicationContext
        BluetoothGameManager.getInstance().setConnectionListener(this)

        // Configurar OnlineServerManager
        onlineServerManager = OnlineServerManager(this)

        // Dibujar posición inicial del jugador local
        mapView.updateLocalPlayerPosition(localPlayerPosition)

        connectToOnlineServer()

    }

    private fun setupButtonListeners() {
        btnStartServer.setOnClickListener { startServer() }
        btnConnectDevice.setOnClickListener {
            val intent = Intent(this, DeviceListActivity::class.java)
            selectDeviceLauncher.launch(intent)
        }
        btnOnlineServer.setOnClickListener { connectToOnlineServer() }
        setupMovementButtons()
    }

    private fun setupMovementButtons() {
        btnNorth.setOnClickListener { movePlayer(0, -1) }
        btnSouth.setOnClickListener { movePlayer(0, 1) }
        btnEast.setOnClickListener { movePlayer(1, 0) }
        btnWest.setOnClickListener { movePlayer(-1, 0) }
    }

    private fun movePlayer(deltaX: Int, deltaY: Int) {
        val newX = (localPlayerPosition.first + deltaX).coerceIn(0, 19) // Limitar X entre 0 y 19
        val newY = (localPlayerPosition.second + deltaY).coerceIn(0, 19) // Limitar Y entre 0 y 19

        localPlayerPosition = Pair(newX, newY)
        mapView.updateLocalPlayerPosition(localPlayerPosition)

        localPlayerPosition = Pair(newX, newY)
        mapView.updateLocalPlayerPosition(localPlayerPosition)

        // Enviar posición al servidor
        onlineServerManager.sendUpdateMessage(playerName, newX, newY) // Reemplaza "localPlayerId" con el ID de tu jugador local

        // Enviar posición al jugador remoto
        if (isConnected) {
            BluetoothGameManager.getInstance().sendPlayerPosition(newX, newY)
        }
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

    private fun connectToOnlineServer() {
        val serverUrl = "ws://3.16.218.70:8080"
        onlineServerManager.connectToServer(serverUrl)
        Toast.makeText(this, "Conectando al servidor online...", Toast.LENGTH_SHORT).show()

        // Enviar mensaje de unión al servidor
        onlineServerManager.sendJoinMessage(playerName) // Reemplaza "localPlayerId" con el ID de tu jugador local
    }


override fun onMessageReceived(message: String) {
    val jsonObject = JSONObject(message)
    when (jsonObject.getString("type")) {
        "positions" -> {
            val players = jsonObject.getJSONObject("players")
            val positions = mutableMapOf<String, Pair<Int, Int>>()
            players.keys().forEach { playerId ->
                val position = players.getJSONObject(playerId)
                val x = position.getInt("x")
                val y = position.getInt("y")
                positions[playerId] = Pair(x, y)
            }
            mapView.updateRemotePlayerPositions(positions)
        }
        "disconnect" -> {
            // Manejar la desconexión del jugador
            val playerId = jsonObject.getString("id")
            mapView.removeRemotePlayer(playerId)
        }
    }
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
        }
    }

    override fun onConnectionFailed(message: String) {
        isConnected = false
        updateBluetoothStatus("Conexión fallida: $message")
    }

    companion object {
        const val TAG = "GameplayActivity"
        const val REQUEST_BLUETOOTH_PERMISSIONS = 101
    }
}