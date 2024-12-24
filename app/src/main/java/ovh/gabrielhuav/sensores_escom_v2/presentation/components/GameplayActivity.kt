package ovh.gabrielhuav.sensores_escom_v2.presentation.components

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager

@SuppressLint("ClickableViewAccessibility")
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

    private var localPlayerPosition = Pair(1, 1)
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
        btnNorth.setOnTouchListener { _, event ->
            handleMovement(event, 0, -1)
            true
        }
        btnSouth.setOnTouchListener { _, event ->
            handleMovement(event, 0, 1)
            true
        }
        btnEast.setOnTouchListener { _, event ->
            handleMovement(event, 1, 0)
            true
        }
        btnWest.setOnTouchListener { _, event ->
            handleMovement(event, -1, 0)
            true
        }
    }

   private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            handler.post(object : Runnable {
                override fun run() {
                    val newX = (localPlayerPosition.first + deltaX).coerceIn(0, 39)
                    val newY = (localPlayerPosition.second + deltaY).coerceIn(0, 39)
                    if (mapView.mapMatrix[newY][newX] != 1 && mapView.mapMatrix[newY][newX] != 3) {
                        movePlayer(deltaX, deltaY)
                    }
                    handler.postDelayed(this, 100)
                }
            })
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            handler.removeCallbacksAndMessages(null)
        }
    }
}

    private fun movePlayer(deltaX: Int, deltaY: Int) {
        val newX = (localPlayerPosition.first + deltaX).coerceIn(0, 39)
        val newY = (localPlayerPosition.second + deltaY).coerceIn(0, 39)

        if (mapView.mapMatrix[newY][newX] != 1 && mapView.mapMatrix[newY][newX] != 3) {
            localPlayerPosition = Pair(newX, newY)
            mapView.updateLocalPlayerPosition(localPlayerPosition)

            // Notificar al servidor
            onlineServerManager.sendUpdateMessage(playerName, newX, newY)

            // Notificar a dispositivos Bluetooth
            if (isConnected) {
                BluetoothGameManager.getInstance().sendPlayerPosition(newX, newY)
            }
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

    @SuppressLint("InlinedApi")
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
        val serverUrl = "ws://192.168.1.31:3000"
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

                    if (playerId != playerName) { // Excluir al jugador local por su nombre
                        positions[playerId] = Pair(x, y)
                    }
                }
                mapView.updateRemotePlayerPositions(positions)
            }
            "disconnect" -> {
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
        val remotePosition = Pair(x, y)

        // Verificar si se tiene el permiso BLUETOOTH_CONNECT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                // No actualizar si es el jugador local
                if (device.name != playerName) {
                    mapView.updateRemotePlayerPosition(device.name, remotePosition)
                }
            } else {
                Log.w(TAG, "No se tiene el permiso BLUETOOTH_CONNECT para acceder al nombre del dispositivo.")
            }
        } else {
            // No actualizar si es el jugador local (para versiones anteriores)
            if (device.name != playerName) {
                mapView.updateRemotePlayerPosition(device.name, remotePosition)
            }
        }
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