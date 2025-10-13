package ovh.gabrielhuav.sensores_escom_v2.presentation.common.base

import android.bluetooth.BluetoothDevice
import android.content.Intent
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
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothWebSocketBridge
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.components.UIManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingEdificioIA
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber2
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.PalapasISC
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.building4.BuildingNumber4
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.cafeteria.Cafeteria
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.EstacionamientoEscom
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.Zacatenco
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.building3.SalonPacman
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.buildingIA.PalapasIA
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.gobierno.EdificioGobierno
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie.ZombieGameManager

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

    // === Listener global de zombies: dibuja SOLO los que estén en el mapa main ===
    private val zombieListener = object : ZombieGameManager.Listener {
        override fun onZombiePosition(zombieId: String, mapId: String, position: Pair<Int, Int>) {
            val normalized = MapMatrixProvider.normalizeMapName(mapId)
            if (normalized == MapMatrixProvider.MAP_MAIN) {
                runOnUiThread {
                    mapView.updateSpecialEntity(zombieId, position, normalized)
                    mapView.invalidate()
                }
            } else {
                // si el zombie pertenece a otro mapa, no lo mostramos en MAIN
                runOnUiThread { mapView.removeSpecialEntity(zombieId) }
            }
        }

        override fun onPlayerCaught(victimId: String) {
            if (victimId == playerName) {
                // Mostrar feedback y limpiar. El manager ya llama stopGame().
                runOnUiThread {
                    Toast.makeText(this@GameplayActivity, "¡Te atraparon!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onGameStopped(reason: String) {
            runOnUiThread {
                mapView.removeSpecialEntitiesByPrefix("zombie_")
                mapView.removeSpecialEntity("zombie")
                mapView.invalidate()
            }
        }
    }
    private fun clearZombiesInView() {
        mapView.removeSpecialEntitiesByPrefix("zombie_")
        mapView.removeSpecialEntity("zombie")
        mapView.invalidate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay)

        try {
            initializeComponents(savedInstanceState)

            // Configura el playerManager para el mapa main
            mapView.playerManager.apply {
                setCurrentMap(MapMatrixProvider.MAP_MAIN)
                localPlayerId = playerName
                updateLocalPlayerPosition(gameState.playerPosition)
            }

            // Escuchar al manager global de zombis
            ZombieGameManager.addListener(zombieListener)
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
            playerManager.localPlayerId = playerName
            updateLocalPlayerPosition(gameState.playerPosition)
        }

        // WebSocket listener
        serverConnectionManager.onlineServerManager.setListener(this)

        // Reportar presencia inicial al manager de zombis (por si ya hay juego corriendo)
        ZombieGameManager.updatePlayer(
            playerName,
            MapMatrixProvider.MAP_MAIN,
            gameState.playerPosition
        )
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

        mapView.playerManager.localPlayerId = playerName

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

        if (gameState.isServer) {
            bluetoothManager.checkBluetoothSupport(enableBluetoothLauncher, false)
        }
    }

    private fun setupRole() {
        if (gameState.isServer) {
            setupServerFlow()
        } else {
            val selectedDevice = intent.getParcelableExtra<BluetoothDevice>("SELECTED_DEVICE")
            if (selectedDevice != null) {
                bluetoothManager.connectToDevice(selectedDevice)
                mapView.setBluetoothServerMode(false)
            } else {
                setupServerFlow()
            }
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

            buttonA.setOnClickListener {
                if (canChangeMap) {
                    when (targetDestination) {
                        "edificio2" -> startBuilding2Activity()
                        "escom_building4_floor_2" -> startBuilding4Activity()
                        "cafeteria" -> startCafeteriaActivity()
                        "salon1212" -> startSalonPacmanActivity()
                        "Estacionamiento" -> startEstacionamientoEscomActivity()
                        "zacatenco" -> startZacatencoActivity()
                        "Edificioiabajo" -> startEdificioIABajoActivity()
                        "palapas_ia" -> startPalapasIAActivity()
                        "palapas_isc" -> startPalapasISCActivity()
                        "edificio_gobierno" -> startEdificioGobiernoActivity()
                        else -> showToast("No hay interacción disponible en esta posición")
                    }
                } else {
                    showToast("No hay interacción disponible en esta posición")
                }
            }
        }
    }

    // Destinos/Activities
    private fun startEdificioGobiernoActivity() {
        val intent = Intent(this, EdificioGobierno::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(5, 20))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun startPalapasIAActivity() {
        val intent = Intent(this, PalapasIA::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(15, 37))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun startPalapasISCActivity() {
        val initialPos = MapMatrixProvider.getInitialPositionForMap(MapMatrixProvider.MAP_PALAPAS_ISC)
        val intent = Intent(this, PalapasISC::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", initialPos)
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun startZacatencoActivity() {
        val intent = Intent(this, Zacatenco::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(10, 12))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun startEdificioIABajoActivity() {
        val intent = Intent(this, BuildingEdificioIA::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(2, 36))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun startCafeteriaActivity() {
        val intent = Intent(this, Cafeteria::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(1, 1))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun startBuilding2Activity() {
        val intent = Intent(this, BuildingNumber2::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(1, 1))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun startBuilding4Activity() {
        val intent = Intent(this, BuildingNumber4::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(1, 1))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun startEstacionamientoEscomActivity() {
        val intent = Intent(this, EstacionamientoEscom::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(4, 25))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private var canChangeMap = false
    private var targetDestination: String? = null

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        when {
            position.first == 15 && position.second == 10 -> {
                canChangeMap = true; targetDestination = "edificio2"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar al edificio 2", Toast.LENGTH_SHORT).show() }
            }
            position.first == 11 && position.second == 4 -> {
                canChangeMap = true; targetDestination = "zacatenco"
                runOnUiThread { Toast.makeText(this, "Presiona A para salir a Zacatenco", Toast.LENGTH_SHORT).show() }
            }
            position.first == 33 && position.second == 34 -> {
                canChangeMap = true; targetDestination = "cafeteria"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar a la cafetería", Toast.LENGTH_SHORT).show() }
            }
            position.first == 25 && position.second == 5 -> {
                canChangeMap = true; targetDestination = "Estacionamiento"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar al estacionamiento", Toast.LENGTH_SHORT).show() }
            }
            position.first == 27 && position.second == 28 -> {
                canChangeMap = true; targetDestination = "salon1212"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar al salón 1212", Toast.LENGTH_SHORT).show() }
            }
            position.first == 23 && position.second == 10 -> {
                canChangeMap = true; targetDestination = "escom_building4_floor_2"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar al salón 1212", Toast.LENGTH_SHORT).show() }
            }
            position.first == 31 && position.second == 21 -> {
                canChangeMap = true; targetDestination = "Edificioiabajo"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar al edificio de ia ", Toast.LENGTH_SHORT).show() }
            }
            position.first == 31 && position.second == 10 -> {
                canChangeMap = true; targetDestination = "palapas_ia"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar a Palapas de IA ", Toast.LENGTH_SHORT).show() }
            }
            position.first == 8 && position.second == 29 -> {
                canChangeMap = true; targetDestination = "palapas_isc"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar a Palapas ISC", Toast.LENGTH_SHORT).show() }
            }
            position.first == 10 && position.second == 18 -> {
                canChangeMap = true; targetDestination = "edificio_gobierno"
                runOnUiThread { Toast.makeText(this, "Presiona A para entrar al edificio de gobierno", Toast.LENGTH_SHORT).show() }
            }
            else -> { canChangeMap = false; targetDestination = null }
        }
    }

    private fun startSalonPacmanActivity() {
        val intent = Intent(this, SalonPacman::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(20, 20))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent); finish()
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                gameState.playerPosition = position
                mapView.updateLocalPlayerPosition(position, forceCenter = true)

                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, "main")
                }

                // 🔑 Reportar posición al manager global para persecución
                ZombieGameManager.updatePlayer(
                    playerName,
                    MapMatrixProvider.MAP_MAIN,
                    position
                )

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
                                val map = playerData.optString("map", MapMatrixProvider.MAP_MAIN)

                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to GameState.PlayerInfo(position, map))

                                val normalizedMap = MapMatrixProvider.normalizeMapName(map)
                                mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)

                                // 🔑 Avisar al manager de zombis
                                ZombieGameManager.updatePlayer(playerId, normalizedMap, position)
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
                            val map = if (jsonObject.has("map")) {
                                jsonObject.getString("map")
                            } else if (jsonObject.has("currentmap")) {
                                jsonObject.getString("currentmap")
                            } else {
                                MapMatrixProvider.MAP_MAIN
                            }

                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to GameState.PlayerInfo(position, map))

                            val normalizedMap = MapMatrixProvider.normalizeMapName(map)
                            mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)

                            // 🔑 Avisar al manager de zombis
                            ZombieGameManager.updatePlayer(playerId, normalizedMap, position)
                            Log.d(TAG, "Updated from update: player=$playerId, pos=$position, map=$map")
                        }
                    }
                    "join" -> {
                        val newPlayerId = jsonObject.getString("id")
                        Log.d(TAG, "Player joined: $newPlayerId")
                        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                        serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, "main")
                    }
                    "disconnect" -> {
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

    private fun handleUpdateMessage(jsonObject: JSONObject) {
        runOnUiThread {
            val playerId = jsonObject.getString("id")
            if (playerId != playerName) {
                val position = Pair(
                    jsonObject.getInt("x"),
                    jsonObject.getInt("y")
                )
                val map = jsonObject.getString("currentmap")

                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                        (playerId to GameState.PlayerInfo(position, map))

                val normalizedMap = MapMatrixProvider.normalizeMapName(map)
                mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                mapView.invalidate()

                // 🔑 Avisar al manager de zombis
                ZombieGameManager.updatePlayer(playerId, normalizedMap, position)

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

        // Reafirma presencia en el main al volver al frente
        ZombieGameManager.updatePlayer(
            playerName,
            MapMatrixProvider.MAP_MAIN,
            gameState.playerPosition
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()
        ZombieGameManager.removeListener(zombieListener)
    }

    override fun onPause() {
        super.onPause()
        movementManager.stopMovement()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        try {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    movementManager.setPosition(gameState.playerPosition)
                    mapView.forceRecenterOnPlayer()
                    updateRemotePlayersOnMap()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al actualizar después de cambio de orientación: ${e.message}")
                }
            }, 300)
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
