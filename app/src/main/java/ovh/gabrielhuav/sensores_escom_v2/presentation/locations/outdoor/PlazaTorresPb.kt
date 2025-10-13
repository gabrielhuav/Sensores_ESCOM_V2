package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothWebSocketBridge
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.components.UIManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView

class PlazaTorresPb : AppCompatActivity(),
    BluetoothManager.BluetoothManagerCallback,
    BluetoothGameManager.ConnectionListener,
    OnlineServerManager.WebSocketListener,
    MapView.MapTransitionListener {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var uiManager: UIManager
    private lateinit var mapView: MapView

    private lateinit var playerName: String
    private lateinit var bluetoothBridge: BluetoothWebSocketBridge

    private var gameState = GameState()

    // Variables para manejar la transición de mapas
    private var canChangeMap = false
    private var targetDestination: String? = null

    data class GameState(
        var isServer: Boolean = false,
        var isConnected: Boolean = false,
        var playerPosition: Pair<Int, Int> = Pair(18, 18),
        var remotePlayerPositions: Map<String, PlayerInfo> = emptyMap(),
        var remotePlayerName: String? = null
    ) {
        data class PlayerInfo(
            val position: Pair<Int, Int>,
            val map: String
        )
    }

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
        setContentView(R.layout.plaza_torres_pb)

        try {
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.plaza_torres_pb
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)
            initializeComponents(savedInstanceState)

            mapView.post {
                val normalizedMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_PLAZA_TORRES)
                mapView.setCurrentMap(normalizedMap, R.drawable.plaza_torres_pb)

                mapView.playerManager.apply {
                    setCurrentMap(normalizedMap)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }
                Log.d("PlazaTorresPb", "Set map to: $normalizedMap")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error inicializando la actividad.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Nombre de jugador no encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (savedInstanceState == null) {
            gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
            // CAMBIO: Usar la posición inicial recibida del Intent
            gameState.playerPosition =
                (intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>) ?: Pair(18, 18)
        } else {
            restoreState(savedInstanceState)
        }

        initializeViews()
        initializeManagers()
        setupInitialConfiguration()

        mapView.apply {
            playerManager.localPlayerId = playerName
            updateLocalPlayerPosition(gameState.playerPosition)
        }

        serverConnectionManager.onlineServerManager.setListener(this)
    }

    private fun initializeViews() {
        uiManager = UIManager(findViewById(R.id.main_layout), mapView).apply {
            initializeViews()
        }
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, uiManager.tvBluetoothStatus).apply {
            setCallback(this@PlazaTorresPb)
        }
        bluetoothBridge = BluetoothWebSocketBridge.getInstance()
        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@PlazaTorresPb)
        }
        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )
        movementManager = MovementManager(
            mapView = mapView
        ) { position -> updatePlayerPosition(position) }

        mapView.playerManager.localPlayerId = playerName
        mapView.setMapTransitionListener(this)
        updatePlayerPosition(gameState.playerPosition)
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        // Esta función podría usarse para transiciones a OTROS mapas desde Plaza Torres.
        // Por ahora, el regreso a Zacatenco se maneja directamente.
        Log.d(TAG, "Transición solicitada a: $targetMap no implementada.")
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int> ?: Pair(18, 18)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions = (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                    as? HashMap<String, GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        if (gameState.isConnected) {
            serverConnectionManager.connectToServer { success ->
                if (success) {
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)
                    updateRemotePlayersOnMap()
                }
            }
        }
        val bluetoothState = savedInstanceState.getInt("BLUETOOTH_STATE")
        val connectedDevice = savedInstanceState.getParcelable<BluetoothDevice>("CONNECTED_DEVICE")
        if (bluetoothState == BluetoothManager.ConnectionState.CONNECTED.ordinal && connectedDevice != null) {
            bluetoothManager.connectToDevice(connectedDevice)
        }
    }

    private fun setupInitialConfiguration() {
        setupRole()
        setupButtonListeners()
        bluetoothManager.checkBluetoothSupport(enableBluetoothLauncher, false)
    }

    private fun setupRole() {
        if (gameState.isServer) {
            setupServerFlow()
        } else {
            setupClientFlow(false)
        }
    }

    private fun setupServerFlow() {
        serverConnectionManager.connectToServer { success ->
            gameState.isConnected = success
            if (success) {
                serverConnectionManager.onlineServerManager.apply {
                    sendJoinMessage(playerName)
                    requestPositionsUpdate()
                }
                uiManager.updateBluetoothStatus("Conectado al servidor online.")
                uiManager.btnStartServer.isEnabled = bluetoothManager.isBluetoothEnabled()
            } else {
                uiManager.updateBluetoothStatus("Error al conectar al servidor online.")
            }
        }
    }

    private fun setupClientFlow(forceBluetooth: Boolean = true) {
        val selectedDevice = intent.getParcelableExtra<BluetoothDevice>("SELECTED_DEVICE")
        selectedDevice?.let { device ->
            if (bluetoothManager.isBluetoothEnabled() || forceBluetooth) {
                bluetoothManager.connectToDevice(device)
                mapView.setBluetoothServerMode(false)
            } else {
                uiManager.updateBluetoothStatus("Bluetooth desactivado.")
            }
        }
    }

    // CAMBIO: Lógica para detectar el punto de salida
    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        // Punto de entrada/salida: (18, 18)
        if (position.first == 18 && position.second == 18) {
            canChangeMap = true
            targetDestination = "zacatenco" // Identificador para el destino
            runOnUiThread {
                Toast.makeText(this, "Presiona A para volver a Zacatenco", Toast.LENGTH_SHORT).show()
            }
        } else {
            canChangeMap = false
            targetDestination = null
        }
    }

    private fun setupButtonListeners() {
        uiManager.apply {
            btnStartServer.setOnClickListener {
                if (gameState.isConnected) bluetoothManager.startServer()
                else showToast("Debe conectarse al servidor online primero.")
            }
            // CAMBIO: Botón secundario ahora también regresa
            btnConnectDevice.setOnClickListener {
                returnToZacatencoActivity()
            }
            btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
            btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
            btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
            btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

            // CAMBIO: Lógica del botón A para regresar a Zacatenco
            buttonA.setOnClickListener {
                if (canChangeMap && targetDestination == "zacatenco") {
                    returnToZacatencoActivity()
                } else {
                    showToast("No hay interacción disponible en esta posición")
                }
            }
        }
    }

    // CAMBIO: Función para regresar a Zacatenco
    private fun returnToZacatencoActivity() {
        // Recuperamos la posición que teníamos en Zacatenco para regresar al mismo lugar.
        val previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
            ?: Pair(13, 11) // Posición de fallback por si algo falla

        val intent = Intent(this, Zacatenco::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected)
            putExtra("INITIAL_POSITION", previousPosition) // Volvemos a la posición anterior
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                gameState.playerPosition = position
                mapView.updateLocalPlayerPosition(position, forceCenter = true)
                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.MAP_PLAZA_TORRES)
                }
                // CAMBIO: Llamamos a la función que revisa si estamos en un punto de salida
                checkPositionForMapChange(position)
            } catch (e: Exception) {
                Log.e(TAG, "Error en updatePlayerPosition: ${e.message}")
            }
        }
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        movementManager.handleMovement(event, deltaX, deltaY)
    }

    private fun updateRemotePlayersOnMap() {
        runOnUiThread {
            for ((id, playerInfo) in gameState.remotePlayerPositions) {
                if (id != playerName) {
                    mapView.updateRemotePlayerPosition(id, playerInfo.position, playerInfo.map)
                }
            }
        }
    }

    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
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

    override fun onConnectionFailed(message: String) { onBluetoothConnectionFailed(message) }
    override fun onDeviceConnected(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        gameState.remotePlayerName = device.name
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                Log.d(TAG, "Received WebSocket message: $message")
                val jsonObject = JSONObject(message)
                when (jsonObject.getString("type")) {
                    "positions" -> handlePositionsMessage(jsonObject)
                    "update" -> handleUpdateMessage(jsonObject)
                    "join" -> handleJoinMessage(jsonObject)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}")
            }
        }
    }

    private fun handlePositionsMessage(jsonObject: JSONObject) {
        val players = jsonObject.getJSONObject("players")
        val newPositions = mutableMapOf<String, GameState.PlayerInfo>()
        players.keys().forEach { playerId ->
            val playerData = players.getJSONObject(playerId)
            val position = Pair(playerData.getInt("x"), playerData.getInt("y"))
            val map = playerData.getString("map")
            if (playerId != playerName) newPositions[playerId] = GameState.PlayerInfo(position, map)
        }
        gameState.remotePlayerPositions = newPositions
        updateRemotePlayersOnMap()
        mapView.invalidate()
    }

    private fun handleUpdateMessage(jsonObject: JSONObject) {
        val playerId = jsonObject.getString("id")
        if (playerId != playerName) {
            val position = Pair(jsonObject.getInt("x"), jsonObject.getInt("y"))
            val map = jsonObject.getString("map")
            gameState.remotePlayerPositions = gameState.remotePlayerPositions + (playerId to GameState.PlayerInfo(position, map))
            mapView.updateRemotePlayerPosition(playerId, position, map)
            mapView.invalidate()
        }
    }

    private fun handleJoinMessage(jsonObject: JSONObject) {
        val newPlayerId = jsonObject.getString("id")
        Log.d(TAG, "Player joined: $newPlayerId")
        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return@runOnUiThread
            val deviceName = device.name ?: "Unknown"
            val currentMap = mapView.playerManager.getCurrentMap()
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), currentMap)
            mapView.invalidate()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean("IS_SERVER", gameState.isServer)
            putBoolean("IS_CONNECTED", gameState.isConnected)
            putSerializable("PLAYER_POSITION", gameState.playerPosition)
            putSerializable("REMOTE_PLAYER_POSITIONS", HashMap(gameState.remotePlayerPositions))
            putString("REMOTE_PLAYER_NAME", gameState.remotePlayerName)
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
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                movementManager.setPosition(gameState.playerPosition)
                mapView.forceRecenterOnPlayer()
                updateRemotePlayersOnMap()
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar después de cambio de orientación: ${e.message}")
            }
        }, 300)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "GameplayActivity" // TAG modificado para mejor depuración
    }
}