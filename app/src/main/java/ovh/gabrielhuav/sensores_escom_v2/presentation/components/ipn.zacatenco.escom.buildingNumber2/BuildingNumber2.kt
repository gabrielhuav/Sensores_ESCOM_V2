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

class BuildingNumber2 : AppCompatActivity(),
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

    data class GameState(
        var isServer: Boolean = false,
        var isConnected: Boolean = false,
        var playerPosition: Pair<Int, Int> = Pair(1, 1),
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
        setContentView(R.layout.activity_building)

        try {
            // Primero inicializamos el mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_edificio2
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            // Inicializar componentes
            initializeComponents(savedInstanceState)

            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa
                val normalizedMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_BUILDING2)
                mapView.setCurrentMap(normalizedMap, R.drawable.escom_edificio2)

                // Después configurar el playerManager
                mapView.playerManager.apply {
                    setCurrentMap(normalizedMap)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }

                Log.d("BuildingNumber2", "Set map to: $normalizedMap")
            }
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

        uiManager = UIManager(findViewById(R.id.main_layout), mapView).apply {
            initializeViews()
        }
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, uiManager.tvBluetoothStatus).apply {
            setCallback(this@BuildingNumber2)
        }

        bluetoothBridge = BluetoothWebSocketBridge.getInstance()

        // Configurar OnlineServerManager con el listener
        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@BuildingNumber2)
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

        // Configurar el listener de transición de mapas
        mapView.setMapTransitionListener(this)

        // Inicializar posición inicial
        updatePlayerPosition(gameState.playerPosition)
    }

    // Actualiza el método onMapTransitionRequested para manejar la transición al salón 2009
    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.MAP_MAIN -> {
                // Transición al mapa principal
                returnToMainActivity()
            }
            MapMatrixProvider.MAP_SALON2009 -> {
                // Transición al salón 2009
                startSalon2009Activity()
            }
            MapMatrixProvider.MAP_SALON2010 -> {
                // Transición al salón 2010
                startSalon2010Activity()
            }
            // Añadir más casos según sea necesario para otros mapas
            else -> {
                Log.d(TAG, "Mapa destino no reconocido: $targetMap")
            }
        }
    }

    // Método para iniciar la Activity del salón 2010
    private fun startSalon2010Activity() {
        val intent = Intent(this, Salon2010::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(20, 20)) // Posición inicial en el salón
            putExtra("PREVIOUS_POSITION", gameState.playerPosition) // Guardar la posición actual para regresar
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Limpiar datos antes de cambiar de activity
        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    // Método para iniciar la Activity del salón 2009
    private fun startSalon2009Activity() {
        val intent = Intent(this, Salon2009::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(20, 20)) // Posición inicial en el salón
            putExtra("PREVIOUS_POSITION", gameState.playerPosition) // Guardar la posición actual para regresar
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Limpiar datos antes de cambiar de activity
        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
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

        // Usamos checkBluetoothSupport con false para no forzar activación
        bluetoothManager.checkBluetoothSupport(enableBluetoothLauncher, false)
    }

    private fun setupRole() {
        if (gameState.isServer) {
            setupServerFlow()
        } else {
            setupClientFlow(false) // Pasamos false para indicar que no forzamos Bluetooth
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
                // Si Bluetooth está desactivado y no forzamos, solo informamos
                uiManager.updateBluetoothStatus("Bluetooth desactivado. Las funciones Bluetooth no estarán disponibles.")
            }
        }
    }

    private var canChangeMap = false  // Variable para controlar si se puede cambiar de mapa
    private var targetMapId: String? = null  // Añadir esta variable para almacenar el mapa destino
    private var interactivePosition: Pair<Int, Int>? = null  // Coordenadas del punto interactivo

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        // Verificar si estamos en un punto interactivo que puede ser una transición
        targetMapId = mapView.getMapTransitionPoint(position.first, position.second)
        interactivePosition = if (targetMapId != null) position else null
        canChangeMap = targetMapId != null

        if (canChangeMap) {
            runOnUiThread {
                when (targetMapId) {
                    MapMatrixProvider.MAP_MAIN -> {
                        Toast.makeText(this, "Presiona A para volver al mapa principal", Toast.LENGTH_SHORT).show()
                    }
                    MapMatrixProvider.MAP_SALON2009 -> {
                        Toast.makeText(this, "Presiona A para entrar al salón 2009", Toast.LENGTH_SHORT).show()
                    }
                    MapMatrixProvider.MAP_SALON2010 -> {
                        Toast.makeText(this, "Presiona A para entrar al salón 2010", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, "Presiona A para interactuar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupButtonListeners() {
        uiManager.apply {
            btnStartServer.setOnClickListener {
                if (gameState.isConnected) bluetoothManager.startServer()
                else showToast("Debe conectarse al servidor online primero.")
            }

            // Añadir el listener para el botón de regreso
            btnConnectDevice.setOnClickListener {
                returnToMainActivity()
            }

            btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
            btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
            btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
            btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

            // Modificar el botón A para manejar las transiciones de mapa
            buttonA.setOnClickListener {
                if (canChangeMap && targetMapId != null) {
                    // En lugar de hacer la lógica aquí directamente, usa el método en MapView
                    mapView.initiateMapTransition(targetMapId!!)
                } else {
                    showToast("No hay interacción disponible en esta posición")
                }
            }
        }
    }

    private fun returnToMainActivity() {
        // Obtener la posición previa del intent
        val previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
            ?: Pair(15, 10) // Posición por defecto si no hay previa

        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", previousPosition) // Usar la posición previa
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Limpiar datos antes de cambiar de activity
        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    private fun startBuildingActivity() {
        val intent = Intent(this, BuildingNumber2::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(1, 1))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                gameState.playerPosition = position

                // Actualizar posición y forzar centrado
                mapView.updateLocalPlayerPosition(position, forceCenter = true)

                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, "escom_building2")
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

    // Implementar el método del WebSocketListener
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
                                val map = playerData.getString("map")
                                mapView.updateRemotePlayerPosition(playerId, position, map)
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
                            val map = jsonObject.getString("map")
                            mapView.updateRemotePlayerPosition(playerId, position, map)
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
