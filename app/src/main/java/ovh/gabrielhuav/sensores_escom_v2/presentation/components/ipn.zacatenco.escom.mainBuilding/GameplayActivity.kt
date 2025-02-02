package ovh.gabrielhuav.sensores_escom_v2.presentation.components

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.BluetoothWebSocketBridge
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview.*

class GameplayActivity : AppCompatActivity(),
    BluetoothManager.BluetoothManagerCallback,
    BluetoothGameManager.ConnectionListener,
    OnlineServerManager.WebSocketListener {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var uiManager: UIManager
    private lateinit var mapView: MapView

    private lateinit var playerName: String
    private lateinit var bluetoothBridge: BluetoothWebSocketBridge

    private var gameState = GameState()

    data class GameState(
        var isServer: Boolean = false,
        var isConnected: Boolean = false,
        var playerPosition: Pair<Int, Int> = Pair(1, 1),
        var remotePlayerPositions: Map<String, Pair<Int, Int>> = emptyMap(),
        var remotePlayerName: String? = null
    )

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth habilitado.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth no fue habilitado.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay)

        try {
            initializeComponents(savedInstanceState)
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error inicializando la actividad.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        // Obtener datos desde Intent o restaurar el estado guardado
        playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Nombre de jugador no encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (savedInstanceState == null) {
            // Inicializar el estado del juego desde el Intent
            gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
            gameState.playerPosition = intent.getParcelableExtra("INITIAL_POSITION") ?: Pair(1, 1)
        } else {
            restoreState(savedInstanceState)
        }

        // Inicializar vistas y gestores de lógica
        initializeViews()
        initializeManagers()
        setupInitialConfiguration()

        mapView.apply {
            playerManager.localPlayerId = playerName  // Establecer ID del jugador local
            updateLocalPlayerPosition(gameState.playerPosition)  // Establecer posición inicial
        }

        // Configurar el bridge para el servidor websocket
        serverConnectionManager.onlineServerManager.setListener(this)
    }

    private fun initializeViews() {
        mapView = MapView(this)
        findViewById<FrameLayout>(R.id.map_container).addView(mapView)

        uiManager = UIManager(findViewById(R.id.main_layout), mapView).apply {
            initializeViews()
        }
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, uiManager.tvBluetoothStatus).apply {
            setCallback(this@GameplayActivity)
        }

        bluetoothBridge = BluetoothWebSocketBridge.getInstance()

        // Configurar OnlineServerManager con el listener
        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@GameplayActivity)
        }

        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        movementManager = MovementManager(
            mapView = mapView
        ) { position -> updatePlayerPosition(position) }

        // Establecer el ID del jugador local
        mapView.playerManager.localPlayerId = playerName

        // Inicializar posición inicial
        updatePlayerPosition(gameState.playerPosition)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int>
                ?: Pair(1, 1)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions = (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                    as? HashMap<String, Pair<Int, Int>>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        // Restaurar conexiones si estaban activas
        if (gameState.isConnected) {
            // Reconectar al servidor online
            serverConnectionManager.connectToServer { success ->
                if (success) {
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)
                    updateRemotePlayersOnMap()
                }
            }
        }

        // Restaurar conexión Bluetooth si existía
        val bluetoothState = savedInstanceState.getInt("BLUETOOTH_STATE")
        val connectedDevice = savedInstanceState.getParcelable<BluetoothDevice>("CONNECTED_DEVICE")

        if (bluetoothState == BluetoothManager.ConnectionState.CONNECTED.ordinal && connectedDevice != null) {
            bluetoothManager.connectToDevice(connectedDevice)
        }
    }

    private fun setupInitialConfiguration() {
        setupRole()
        setupButtonListeners()
        bluetoothManager.checkBluetoothSupport(enableBluetoothLauncher)
    }

    private fun setupRole() {
        if (gameState.isServer) {
            setupServerFlow()
        } else {
            setupClientFlow()
        }
    }

    private fun setupServerFlow() {
        serverConnectionManager.connectToServer { success ->
            gameState.isConnected = success
            if (success) {
                // Enviar mensaje de unión al servidor
                serverConnectionManager.onlineServerManager.apply {
                    sendJoinMessage(playerName)
                    // Solicitar posiciones actuales
                    requestPositionsUpdate()
                }
                uiManager.updateBluetoothStatus("Conectado al servidor online. Puede iniciar servidor Bluetooth.")
                uiManager.btnStartServer.isEnabled = true
            } else {
                uiManager.updateBluetoothStatus("Error al conectar al servidor online.")
            }
        }
    }

    private fun setupClientFlow() {
        val selectedDevice = intent.getParcelableExtra<BluetoothDevice>("SELECTED_DEVICE")
        selectedDevice?.let { device ->
            bluetoothManager.connectToDevice(device)
            mapView.setBluetoothServerMode(false)
        }
    }

    private fun setupButtonListeners() {
        uiManager.apply {
            btnStartServer.setOnClickListener {
                if (gameState.isConnected) bluetoothManager.startServer()
                else showToast("Debe conectarse al servidor online primero.")
            }

            btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
            btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
            btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
            btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }
        }
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        gameState.playerPosition = position
        mapView.updateLocalPlayerPosition(position)

        if (gameState.isConnected) {
            serverConnectionManager.sendUpdateMessage(playerName, position, "main")
        }
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        movementManager.handleMovement(event, deltaX, deltaY)
    }

    private fun updateRemotePlayersOnMap() {
        for ((id, position) in gameState.remotePlayerPositions) {
            if (id != playerName) {
                mapView.updateRemotePlayerPosition(id, position)
            }
        }
    }

    // Bluetooth Callbacks
    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {
        gameState.remotePlayerName = device.name
        uiManager.updateBluetoothStatus("Conectado a ${device.name}")
    }

    override fun onBluetoothConnectionFailed(error: String) {
        uiManager.updateBluetoothStatus("Error: $error")
        showToast(error)
    }

    override fun onConnectionComplete() {
        uiManager.updateBluetoothStatus("Conexión establecida completamente.")
    }

    override fun onConnectionFailed(message: String) {
        onBluetoothConnectionFailed(message)
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        gameState.remotePlayerName = device.name
    }

    // Implementar el método del WebSocketListener
    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                Log.d(TAG, "WebSocket message received: $message")
                val jsonObject = JSONObject(message)

                when (jsonObject.getString("type")) {
                    "positions" -> handlePositionsMessage(jsonObject)
                    "update" -> handleUpdateMessage(jsonObject)
                    "join" -> handleJoinMessage(jsonObject)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing WebSocket message: ${e.message}", e)
            }
        }
    }
    private fun handlePositionsMessage(jsonObject: JSONObject) {
        val players = jsonObject.getJSONObject("players")
        val newPositions = mutableMapOf<String, Pair<Int, Int>>()

        players.keys().forEach { playerId ->
            val playerData = players.getJSONObject(playerId)
            val position = Pair(
                playerData.getInt("x"),
                playerData.getInt("y")
            )

            if (playerId != playerName) {
                newPositions[playerId] = position
            }
        }

        // Actualizar el estado del juego
        gameState.remotePlayerPositions = newPositions

        // Actualizar el mapa
        updateRemotePlayersOnMap()
        mapView.invalidate()
    }

    private fun handleUpdateMessage(jsonObject: JSONObject) {
        val playerId = jsonObject.getString("id")
        if (playerId != playerName) {
            val position = Pair(
                jsonObject.getInt("x"),
                jsonObject.getInt("y")
            )

            // Actualizar el estado del juego
            gameState.remotePlayerPositions = gameState.remotePlayerPositions + (playerId to position)

            // Actualizar el mapa
            mapView.updateRemotePlayerPosition(playerId, position)
            mapView.invalidate()

            Log.d(TAG, "Updated player $playerId position to $position")
        }
    }

    private fun handleJoinMessage(jsonObject: JSONObject) {
        val newPlayerId = jsonObject.getString("id")
        Log.d(TAG, "Player joined: $newPlayerId")
        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        // Usamos el PlayerManager para almacenar la posición del jugador remoto
        val deviceName = device.name ?: "Unknown"

        // Actualizamos la posición del jugador remoto (a través de PlayerManager)
        mapView.updateRemotePlayerPosition(deviceName, Pair(x, y))

        // Mostramos un log para depuración
        Log.d("GameplayActivity", "Recibida posición del dispositivo $deviceName: ($x, $y)")

        // Actualizamos el mapa visualmente
        mapView.invalidate()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean("IS_SERVER", gameState.isServer)
            putBoolean("IS_CONNECTED", gameState.isConnected)
            putSerializable("PLAYER_POSITION", gameState.playerPosition)
            putSerializable("REMOTE_PLAYER_POSITIONS", HashMap(gameState.remotePlayerPositions))
            putString("REMOTE_PLAYER_NAME", gameState.remotePlayerName)
            // Guardar el estado de la conexión Bluetooth
            putInt("BLUETOOTH_STATE", bluetoothManager.getConnectionState().ordinal)
            bluetoothManager.getConnectedDevice()?.let { device ->
                putParcelable("CONNECTED_DEVICE", device)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        bluetoothManager.reconnect()
        movementManager.setPosition(gameState.playerPosition)
        updateRemotePlayersOnMap()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()
    }

    override fun onPause() {
        super.onPause()
        movementManager.stopMovement()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Mantener el estado actual
        updateRemotePlayersOnMap()
        movementManager.setPosition(gameState.playerPosition)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "GameplayActivity"
    }
}
