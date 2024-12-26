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
import ovh.gabrielhuav.sensores_escom_v2.data.map.BluetoothWebSocketBridge
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
    // Declaración del BluetoothWebSocketBridge
    private lateinit var bluetoothBridge: BluetoothWebSocketBridge

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

        val buttonA = findViewById<Button>(R.id.button_a)
        mapContainer = findViewById(R.id.map_container)
        mapView = MapView(this)
        mapContainer.addView(mapView)

        setupButtonListeners()
        setupInteractionButton(buttonA)
        checkBluetoothSupport()

        // Verificar si el Intent tiene un nombre de jugador
        playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Nombre de jugador no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Verificar si el Intent tiene una posición de retorno
        val returnedPosition = intent.getSerializableExtra("PLAYER_POSITION") as? Pair<Int, Int>
        localPlayerPosition = returnedPosition ?: Pair(1, 1)

        println("GameplayActivity1 - playerName: $playerName")
        println("GameplayActivity1 - localPlayerPosition: $localPlayerPosition")

        // Dibujar posición inicial del jugador local
        mapView.updateLocalPlayerPosition(localPlayerPosition)

        if (returnedPosition == null) {
            Toast.makeText(this, "Bienvenido de nuevo, $playerName", Toast.LENGTH_SHORT).show()
        }

        // Configurar BluetoothGameManager
        BluetoothGameManager.appContext = applicationContext
        BluetoothGameManager.getInstance().setConnectionListener(this)

        // Configurar OnlineServerManager
        onlineServerManager = OnlineServerManager(this)
        connectToOnlineServer()

        // Configurar BluetoothWebSocketBridge
        bluetoothBridge = BluetoothWebSocketBridge.getInstance()
        bluetoothBridge.initialize(onlineServerManager, playerName)
        bluetoothBridge.setPositionUpdateListener(object : BluetoothWebSocketBridge.PositionUpdateListener {
            override fun onPositionUpdated(playerId: String, position: Pair<Int, Int>?) {
                if (position != null) {
                    mapView.updateRemotePlayerPosition(playerId, position)
                } else {
                    mapView.removeRemotePlayer(playerId)
                }
            }
        })
    }


    private fun setupInteractionButton(buttonA: Button) {
        buttonA.setOnClickListener {
            val (x, y) = localPlayerPosition
            if (x == 15 && y == 10) {
                println("Button A pressed - Player is at (15, 10), entering building")

                // Notificar al servidor y cambiar de actividad
                onlineServerManager.sendUpdateMessage(playerName, x, y, "building")

                val intent = Intent(this, BuildingActivity::class.java).apply {
                    putExtra("PLAYER_NAME", playerName) // Pasar el nombre del jugador
                    putExtra("PLAYER_POSITION", Pair(0, 0)) // Establecer posición inicial en el edificio
                }

                startActivity(intent)
                finish()
            } else {
                println("Button A pressed - Player not at (15, 10), interaction not allowed")
                Toast.makeText(this, "No puedes interactuar aquí", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupButtonListeners() {
        btnStartServer.setOnClickListener { startBluetoothServer() } // Iniciar servidor Bluetooth
        btnConnectDevice.setOnClickListener {
            Toast.makeText(this, "Ya conectado al servidor online.", Toast.LENGTH_SHORT).show()
        }
        btnOnlineServer.setOnClickListener { connectToOnlineServer() }
        setupMovementButtons()
    }

    private fun startBluetoothServer() {
        if (!isConnected) {
            Toast.makeText(this, "Debe estar conectado al servidor online para iniciar el servidor Bluetooth.", Toast.LENGTH_SHORT).show()
            return
        }

        BluetoothGameManager.getInstance().startServer()
        updateBluetoothStatus("Servidor Bluetooth iniciado, esperando conexiones.")
        Toast.makeText(this, "Servidor Bluetooth iniciado, esperando conexiones.", Toast.LENGTH_SHORT).show()
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

            // Actualizar a través del puente
            bluetoothBridge.updatePosition(playerName, localPlayerPosition)
        }
    }


    private fun connectToOnlineServer() {
        val serverUrl = "ws://192.168.1.17:3000"
        onlineServerManager.connectToServer(serverUrl)
        Toast.makeText(this, "Conectando al servidor online...", Toast.LENGTH_SHORT).show()

        // Enviar mensaje de unión con el nombre del jugador
        onlineServerManager.sendJoinMessage(playerName)

        // Marcar como conectado al servidor online al completar la conexión
        onlineServerManager.setOnConnectionCompleteListener {
            isConnected = true
            runOnUiThread {
                Toast.makeText(this, "Conexión al servidor online establecida.", Toast.LENGTH_SHORT).show()
            }
        }

        onlineServerManager.setOnConnectionFailedListener {
            isConnected = false
            runOnUiThread {
                Toast.makeText(this, "Error al conectar al servidor online.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onMessageReceived(message: String) {
        bluetoothBridge.handleWebSocketMessage(message)
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

    override fun onDeviceConnected(device: BluetoothDevice) {
        isConnected = true
        bluetoothBridge.addBluetoothClient(device, device.name ?: "Unknown")
        updateBluetoothStatus("Connected to ${device.name ?: "Unknown"}")
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        val remotePosition = Pair(x, y)
        Log.d("GameplayActivity", "Local position: $localPlayerPosition, Remote position: $remotePosition")
        BluetoothGameManager.getInstance().sendBothPositions(localPlayerPosition, remotePosition)
        onlineServerManager.sendBothPositions(playerName, localPlayerPosition.first, localPlayerPosition.second, x, y, "main")
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

    private fun updateBluetoothStatus(status: String) {
        tvBluetoothStatus.text = status
    }

    companion object {
        const val TAG = "GameplayActivity"
        const val REQUEST_BLUETOOTH_PERMISSIONS = 101
    }
}
