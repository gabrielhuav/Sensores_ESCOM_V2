package ovh.gabrielhuav.sensores_escom_v2.presentation.components

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
        var remotePlayerPositions: Map<String, PlayerInfo> = emptyMap(), // Cambiado para incluir mapa
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
        setContentView(R.layout.activity_gameplay)

        try {
            initializeComponents(savedInstanceState)

            // Después de inicializar los componentes, configura el playerManager
            mapView.playerManager.apply {
                setCurrentMap("main")
                localPlayerId = playerName
                gameState.playerPosition?.let { updateLocalPlayerPosition(it) }
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
            // Usar la posición inicial proporcionada
            gameState.playerPosition = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
                ?: Pair(1, 1)
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
                    as? HashMap<String, GameState.PlayerInfo>)?.toMap() ?: emptyMap()
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

        // Reemplazamos la comprobación forzosa de Bluetooth por una más flexible
        // Solo comprobamos si el usuario ha elegido ser servidor o conectarse explícitamente
        if (gameState.isServer) {
            // Solo verificamos Bluetooth si somos servidor
            bluetoothManager.checkBluetoothSupport(enableBluetoothLauncher, false) // Pasamos false para no forzar
        }
    }

    private fun setupRole() {
        if (gameState.isServer) {
            setupServerFlow()
        } else {
            // Comprobar si tiene un dispositivo seleccionado para conectarse
            val selectedDevice = intent.getParcelableExtra<BluetoothDevice>("SELECTED_DEVICE")
            if (selectedDevice != null) {
                // Solo en este caso iniciamos la conexión Bluetooth
                bluetoothManager.connectToDevice(selectedDevice)
                mapView.setBluetoothServerMode(false)
            } else {
                // Si no hay dispositivo seleccionado, solo conectamos al servidor online
                setupServerFlow()
            }
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
                uiManager.updateBluetoothStatus("Conectado al servidor online. Puede iniciar servidor Bluetooth si lo desea.")
                uiManager.btnStartServer.isEnabled = true
            } else {
                uiManager.updateBluetoothStatus("Error al conectar al servidor online. El juego funcionará solo en modo local.")
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

            // Configurar el botón A para verificar la posición y dirigirse al mapa correspondiente
            buttonA.setOnClickListener {
                if (canChangeMap) {
                    when (targetDestination) {
                        "edificio2" -> startBuildingActivity()
                        "cafeteria" -> startCafeteriaActivity()
                        "edificioNuevo" -> startEdificioNuevoActivity()
                        "salidaMetro" -> startSalidaMetroActivity()
                        else -> showToast("No hay interacción disponible en esta posición")
                    }
                } else {
                    showToast("No hay interacción disponible en esta posición")
                }
            }
        }
    }


    private fun startCafeteriaActivity() {
        val intent = Intent(this, Cafeteria::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(1, 1))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition) // Guarda la posición actual
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun startEdificioNuevoActivity() {
        val intent = Intent(this, EdificioNuevo::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(1, 1))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition) // Guarda la posición actual
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun startSalidaMetroActivity() {
        val intent = Intent(this, SalidaMetro::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(1, 1))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition) // Guarda la posición actual
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun startBuildingActivity() {
        val intent = Intent(this, BuildingNumber2::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(1, 1))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition) // Guarda la posición actual
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private var canChangeMap = false  // Variable para controlar si se puede cambiar de mapa
    private var targetDestination: String? = null  // Variable para almacenar el destino

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        // Comprobar múltiples ubicaciones de transición
        when {
            position.first == 15 && position.second == 10 -> {
                canChangeMap = true
                targetDestination = "edificio2"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al edificio 2", Toast.LENGTH_SHORT).show()
                }
            }
            position.first == 32 && position.second == 10 -> {
                canChangeMap = true
                targetDestination = "edificioNuevo"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar a las palapas", Toast.LENGTH_SHORT).show()
                }
            }
            position.first == 38 && position.second == 32 -> {
                canChangeMap = true
                targetDestination = "salidaMetro"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para salir de la escuela", Toast.LENGTH_SHORT).show()
                }
            }
            position.first == 33 && position.second == 34 -> {
                canChangeMap = true
                targetDestination = "cafeteria"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar a la cafetería", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                canChangeMap = false
                targetDestination = null
            }
        }
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                gameState.playerPosition = position

                // Actualizar posición y forzar centrado
                mapView.updateLocalPlayerPosition(position, forceCenter = true)

                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, "main")
                }

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

// Modificación para GameplayActivity.kt en el método onMessageReceived

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                Log.d(TAG, "Received WebSocket message: $message")
                val jsonObject = JSONObject(message)

                when (jsonObject.getString("type")) {
                    "positions" -> {
                        val players = jsonObject.getJSONObject("players")
                        players.keys().forEach { playerId ->
                            if (playerId != playerName) {
                                val playerData = players.getJSONObject(playerId.toString())
                                val position = Pair(
                                    playerData.getInt("x"),
                                    playerData.getInt("y")
                                )

                                // Obtener el mapa del jugador, con 'main' como valor predeterminado
                                val map = playerData.optString("map", MapMatrixProvider.MAP_MAIN)

                                // Actualizar el estado del juego
                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to GameState.PlayerInfo(position, map))

                                // Siempre actualizar la posición, permitiendo que PlayerManager
                                // determine si debe mostrarse o no
                                mapView.updateRemotePlayerPosition(playerId, position, map)
                                Log.d(TAG, "Updated from positions: player=$playerId, pos=$position, map=$map")
                            }
                        }
                    }
                    "update" -> {
                        val playerId = jsonObject.getString("id")
                        if (playerId != playerName) {
                            val position = Pair(
                                jsonObject.getInt("x"),
                                jsonObject.getInt("y")
                            )

                            // Obtener el mapa, primero intentando 'map', luego 'currentmap', con 'main' como valor predeterminado
                            val map = if (jsonObject.has("map")) {
                                jsonObject.getString("map")
                            } else if (jsonObject.has("currentmap")) {
                                jsonObject.getString("currentmap")
                            } else {
                                MapMatrixProvider.MAP_MAIN // Valor predeterminado
                            }

                            // Actualizar el estado del juego
                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to GameState.PlayerInfo(position, map))

                            // Siempre actualizar la posición en el mapa
                            mapView.updateRemotePlayerPosition(playerId, position, map)
                            Log.d(TAG, "Updated from update: player=$playerId, pos=$position, map=$map")
                        }
                    }
                    "join" -> {
                        // Un jugador se unió, solicitar actualización de posiciones
                        val newPlayerId = jsonObject.getString("id")
                        Log.d(TAG, "Player joined: $newPlayerId")
                        serverConnectionManager.onlineServerManager.requestPositionsUpdate()

                        // También enviamos nuestra posición actual
                        serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, "main")
                    }
                    "disconnect" -> {
                        // Manejar desconexión de jugador
                        val disconnectedId = jsonObject.getString("id")
                        if (disconnectedId != playerName) {
                            gameState.remotePlayerPositions = gameState.remotePlayerPositions - disconnectedId
                            mapView.removeRemotePlayer(disconnectedId)
                            Log.d(TAG, "Player disconnected: $disconnectedId")
                        }
                    }
                }
                mapView.invalidate()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}")
            }
        }
    }

    // Actualiza handlePositionsMessage
    private fun handlePositionsMessage(jsonObject: JSONObject) {
        runOnUiThread {
            val players = jsonObject.getJSONObject("players")
            val newPositions = mutableMapOf<String, GameState.PlayerInfo>()

            players.keys().forEach { playerId ->
                val playerData = players.getJSONObject(playerId)
                val position = Pair(
                    playerData.getInt("x"),
                    playerData.getInt("y")
                )
                val map = playerData.getString("map")

                if (playerId != playerName) {
                    newPositions[playerId] = GameState.PlayerInfo(position, map)
                }
            }

            gameState.remotePlayerPositions = newPositions
            updateRemotePlayersOnMap()
            mapView.invalidate()
        }
    }
    // Actualiza handleUpdateMessage
    private fun handleUpdateMessage(jsonObject: JSONObject) {
        runOnUiThread {
            val playerId = jsonObject.getString("id")
            if (playerId != playerName) {
                val position = Pair(
                    jsonObject.getInt("x"),
                    jsonObject.getInt("y")
                )
                val map = jsonObject.getString("currentmap")  // Cambiado de "currentmap" a "map"

                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                        (playerId to GameState.PlayerInfo(position, map))

                mapView.updateRemotePlayerPosition(playerId, position, map)
                mapView.invalidate()

                Log.d(TAG, "Updated player $playerId position to $position in map $map")
            }
        }
    }

    private fun handleJoinMessage(jsonObject: JSONObject) {
        val newPlayerId = jsonObject.getString("id")
        Log.d(TAG, "Player joined: $newPlayerId")
        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            val deviceName = device.name ?: "Unknown"
            val currentMap = mapView.playerManager.getCurrentMap()
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), currentMap)
            Log.d("GameplayActivity", "Recibida posición del dispositivo $deviceName: ($x, $y)")
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

        try {
            // Evitamos llamar directamente a las funciones que podrían causar problemas
            // En su lugar, programamos una tarea para cuando la UI esté lista
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    // Recuperar el estado actual
                    movementManager.setPosition(gameState.playerPosition)

                    // Actualizar el estado del mapa para la nueva orientación
                    mapView.forceRecenterOnPlayer()

                    // Actualizar jugadores remotos
                    updateRemotePlayersOnMap()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al actualizar después de cambio de orientación: ${e.message}")
                }
            }, 300) // Pequeño retraso para asegurar que la vista se ha actualizado
        } catch (e: Exception) {
            Log.e(TAG, "Error en onConfigurationChanged: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "GameplayActivity"
    }
}
