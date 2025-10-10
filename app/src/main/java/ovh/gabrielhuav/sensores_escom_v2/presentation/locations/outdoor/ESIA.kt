package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
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
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.components.UIManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView

class ESIA : AppCompatActivity(),
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
        var playerPosition: Pair<Int, Int> = Pair(25, 35), // Posición inicial cerca de la entrada
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

        Log.d(TAG, "=== ESIA onCreate INICIADO ===")

        try {
            Log.d(TAG, "1. Estableciendo layout de ESIA")
            setContentView(R.layout.activity_esia) // ← AHORA USA EL LAYOUT CORRECTO
            Log.d(TAG, "✓ Layout de ESIA establecido correctamente")

            Log.d(TAG, "2. Creando MapView con imagen de ESIA")
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.esia_croquis // ← AHORA USA LA IMAGEN DE ESIA
            )
            Log.d(TAG, "✓ MapView con imagen de ESIA creado correctamente")

            Log.d(TAG, "3. Agregando MapView al contenedor")
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)
            Log.d(TAG, "✓ MapView agregado al contenedor")

            Log.d(TAG, "4. Iniciando initializeComponents")
            initializeComponents(savedInstanceState)
            Log.d(TAG, "✓ initializeComponents completado")

            Log.d(TAG, "5. Configurando mapView.post para ESIA")
            mapView.post {
                try {
                    Log.d(TAG, "6. Dentro de mapView.post - configurando mapa ESIA")

                    val normalizedMap = MapMatrixProvider.Companion.normalizeMapName(MapMatrixProvider.Companion.MAP_ESIA)
                    Log.d(TAG, "✓ Mapa normalizado: $normalizedMap")

                    mapView.setCurrentMap(normalizedMap, R.drawable.esia_croquis) // ← IMAGEN DE ESIA
                    Log.d(TAG, "✓ Mapa ESIA establecido con imagen correcta")

                    // Configurar el playerManager
                    mapView.playerManager.apply {
                        Log.d(TAG, "7. Configurando playerManager para ESIA")
                        setCurrentMap(normalizedMap)
                        localPlayerId = playerName
                        updateLocalPlayerPosition(gameState.playerPosition)
                        Log.d(TAG, "✓ PlayerManager de ESIA configurado")
                    }

                    Log.d(TAG, "✓ ESIA mapa configurado completamente: $normalizedMap")
                } catch (e: Exception) {
                    Log.e(TAG, "ERROR en mapView.post de ESIA: ${e.message}", e)
                }
            }

            Log.d(TAG, "=== ESIA onCreate COMPLETADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR CRÍTICO EN ONCREATE DE ESIA ===")
            Log.e(TAG, "Error: ${e.message}", e)
            Toast.makeText(this, "Error en ESIA: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        Log.d(TAG, "initializeComponents ESIA: INICIO")

        try {
            // Obtener datos desde Intent
            Log.d(TAG, "ESIA: Obteniendo PLAYER_NAME del Intent")
            playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
                Log.e(TAG, "ESIA: PLAYER_NAME no encontrado en Intent")
                Toast.makeText(this, "Nombre de jugador no encontrado.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            Log.d(TAG, "ESIA: ✓ PLAYER_NAME obtenido: $playerName")

            if (savedInstanceState == null) {
                Log.d(TAG, "ESIA: Inicializando desde Intent")
                gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
                gameState.playerPosition =
                    (intent.getParcelableExtra("INITIAL_POSITION") ?: Pair(25, 35)) as Pair<Int, Int>
                Log.d(TAG, "ESIA: ✓ Estado inicial - isServer: ${gameState.isServer}, position: ${gameState.playerPosition}")
            } else {
                Log.d(TAG, "ESIA: Restaurando estado guardado")
                restoreState(savedInstanceState)
            }

            Log.d(TAG, "ESIA: Llamando initializeViews()")
            initializeViews()
            Log.d(TAG, "ESIA: ✓ initializeViews completado")

            Log.d(TAG, "ESIA: Llamando initializeManagers()")
            initializeManagers()
            Log.d(TAG, "ESIA: ✓ initializeManagers completado")

            Log.d(TAG, "ESIA: Llamando setupInitialConfiguration()")
            setupInitialConfiguration()
            Log.d(TAG, "ESIA: ✓ setupInitialConfiguration completado")

            Log.d(TAG, "ESIA: Configurando mapView")
            mapView.apply {
                playerManager.localPlayerId = playerName
                updateLocalPlayerPosition(gameState.playerPosition)
            }
            Log.d(TAG, "ESIA: ✓ MapView configurado")

            Log.d(TAG, "ESIA: Configurando WebSocket listener")
            serverConnectionManager.onlineServerManager.setListener(this)
            Log.d(TAG, "ESIA: ✓ WebSocket listener configurado")

            Log.d(TAG, "ESIA initializeComponents: COMPLETADO EXITOSAMENTE")

        } catch (e: Exception) {
            Log.e(TAG, "ERROR en ESIA initializeComponents: ${e.message}", e)
            throw e
        }
    }

    private fun initializeViews() {
        Log.d(TAG, "ESIA initializeViews: INICIO")
        try {
            uiManager = UIManager(findViewById(R.id.main_layout), mapView).apply {
                Log.d(TAG, "ESIA: Llamando UIManager.initializeViews()")
                initializeViews()
            }
            Log.d(TAG, "ESIA initializeViews: ✓ COMPLETADO")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en ESIA initializeViews: ${e.message}", e)
            throw e
        }
    }

    private fun initializeManagers() {
        Log.d(TAG, "ESIA initializeManagers: INICIO")
        try {
            bluetoothManager = BluetoothManager.Companion.getInstance(this, uiManager.tvBluetoothStatus).apply {
                setCallback(this@ESIA)
            }

            bluetoothBridge = BluetoothWebSocketBridge.Companion.getInstance()

            val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
                setListener(this@ESIA)
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

            Log.d(TAG, "ESIA initializeManagers: COMPLETADO")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en ESIA initializeManagers: ${e.message}", e)
            throw e
        }
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        Log.d(TAG, "ESIA onMapTransitionRequested: targetMap=$targetMap")
        when (targetMap) {
            MapMatrixProvider.Companion.MAP_ZACATENCO -> {
                Log.d(TAG, "ESIA: Transición a Zacatenco")
                returnToZacatencoActivity()
            }
            else -> {
                Log.d(TAG, "ESIA: Mapa destino no reconocido: $targetMap")
            }
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int>
                ?: Pair(25, 35)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions = (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                    as? HashMap<String, GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        if (gameState.isConnected && ::serverConnectionManager.isInitialized) {
            serverConnectionManager.connectToServer { success ->
                if (success) {
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)
                    updateRemotePlayersOnMap()
                }
            }
        }

        val bluetoothState = savedInstanceState.getInt("BLUETOOTH_STATE")
        val connectedDevice = savedInstanceState.getParcelable<BluetoothDevice>("CONNECTED_DEVICE")

        if (bluetoothState == BluetoothManager.ConnectionState.CONNECTED.ordinal &&
            connectedDevice != null && ::bluetoothManager.isInitialized) {
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
                uiManager.updateBluetoothStatus("Bluetooth desactivado. Las funciones Bluetooth no estarán disponibles.")
            }
        }
    }

    private var canChangeMap = false
    private var targetDestination: String? = null

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        when {
            // Puerta de salida/entrada abajo a la derecha (cerca de biblioteca)
            position.first == 25 && position.second == 35 -> {
                canChangeMap = true
                targetDestination = "zacatenco"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para regresar a Zacatenco", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            else -> {
                canChangeMap = false
                targetDestination = null
            }
        }
    }

    private fun setupButtonListeners() {
        uiManager.apply {
            btnStartServer.setOnClickListener {
                if (gameState.isConnected) bluetoothManager.startServer()
                else showToast("Debe conectarse al servidor online primero.")
            }

            btnConnectDevice.setOnClickListener {
                Log.d(TAG, "ESIA: btnConnectDevice click - Regresando a Zacatenco")
                returnToZacatencoActivity()
            }

            btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
            btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
            btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
            btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

            buttonA.setOnClickListener {
                if (canChangeMap) {
                    when (targetDestination) {
                        "zacatenco" -> {
                            Log.d(TAG, "ESIA: buttonA click - Regresando a Zacatenco")
                            returnToZacatencoActivity()
                        }
                        else -> showToast("No hay interacción disponible en esta posición")
                    }
                } else {
                    showToast("No hay interacción disponible en esta posición")
                }
            }

            // AGREGAR ESTOS LISTENERS PARA EVITAR ERRORES
            try {
                findViewById<Button>(R.id.button_back_to_home)?.setOnClickListener {
                    Log.d(TAG, "ESIA: button_back_to_home click - Regresando a Zacatenco")
                    returnToZacatencoActivity()
                }

                findViewById<Button>(R.id.button_small_1)?.setOnClickListener {
                    showToast("Botón B1 - ESIA")
                }

                findViewById<Button>(R.id.button_small_2)?.setOnClickListener {
                    showToast("Botón B2 - ESIA")
                }

                findViewById<Button>(R.id.button_serverOnline)?.setOnClickListener {
                    showToast("Server Online - ESIA")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Algunos botones adicionales no encontrados: ${e.message}")
            }
        }
    }

    private fun returnToZacatencoActivity() {
        Log.d(TAG, "ESIA: returnToZacatencoActivity INICIO")


        // Zacatenco usará la posición guardada automáticamente
        val intent = Intent(this, Zacatenco::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            // ✅ NO PONER INITIAL_POSITION para que use la posición guardada
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }

        Log.d(TAG, "ESIA: Iniciando actividad Zacatenco (usará posición guardada)")
        startActivity(intent)
        finish()
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                gameState.playerPosition = position

                if (::mapView.isInitialized) {
                    mapView.updateLocalPlayerPosition(position, forceCenter = true)
                }

                if (gameState.isConnected && ::serverConnectionManager.isInitialized) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, "esia")
                }

                checkPositionForMapChange(position)
            } catch (e: Exception) {
                Log.e(TAG, "Error en ESIA updatePlayerPosition: ${e.message}")
            }
        }
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) {
            movementManager.handleMovement(event, deltaX, deltaY)
        }
    }

    private fun updateRemotePlayersOnMap() {
        runOnUiThread {
            if (::mapView.isInitialized) {
                for ((id, playerInfo) in gameState.remotePlayerPositions) {
                    if (id != playerName) {
                        mapView.updateRemotePlayerPosition(id, playerInfo.position, playerInfo.map)
                    }
                }
            }
        }
    }

    // Implementaciones de callbacks
    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gameState.remotePlayerName = device.name
        if (::uiManager.isInitialized) {
            uiManager.updateBluetoothStatus("Conectado a ${device.name}")
        }
    }

    override fun onBluetoothConnectionFailed(error: String) {
        if (::uiManager.isInitialized) {
            uiManager.updateBluetoothStatus("Error: $error")
        }
        showToast(error)
    }

    override fun onConnectionComplete() {
        if (::uiManager.isInitialized) {
            uiManager.updateBluetoothStatus("Conexión establecida completamente.")
        }
    }

    override fun onConnectionFailed(message: String) {
        onBluetoothConnectionFailed(message)
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gameState.remotePlayerName = device.name
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(message)
                when (jsonObject.getString("type")) {
                    "positions" -> {
                        val players = jsonObject.getJSONObject("players")
                        players.keys().forEach { playerId ->
                            if (playerId != playerName && ::mapView.isInitialized) {
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
                        if (playerId != playerName && ::mapView.isInitialized) {
                            val position = Pair(
                                jsonObject.getInt("x"),
                                jsonObject.getInt("y")
                            )
                            val map = jsonObject.getString("map")
                            mapView.updateRemotePlayerPosition(playerId, position, map)
                        }
                    }
                }
                if (::mapView.isInitialized) {
                    mapView.invalidate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}")
            }
        }
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            val deviceName = if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@runOnUiThread
            } else {
                device.name ?: "Unknown"
            }
            if (::mapView.isInitialized) {
                val currentMap = mapView.playerManager.getCurrentMap()
                mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), currentMap)
                mapView.invalidate()
            }
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

            if (::bluetoothManager.isInitialized) {
                putInt("BLUETOOTH_STATE", bluetoothManager.getConnectionState().ordinal)
                bluetoothManager.getConnectedDevice()?.let { device ->
                    putParcelable("CONNECTED_DEVICE", device)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::bluetoothManager.isInitialized) {
            bluetoothManager.reconnect()
        }
        if (::movementManager.isInitialized) {
            movementManager.setPosition(gameState.playerPosition)
        }
        updateRemotePlayersOnMap()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothManager.isInitialized) {
            bluetoothManager.cleanup()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::movementManager.isInitialized) {
            movementManager.stopMovement()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        try {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (::movementManager.isInitialized) {
                        movementManager.setPosition(gameState.playerPosition)
                    }
                    if (::mapView.isInitialized) {
                        mapView.forceRecenterOnPlayer()
                    }
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
        private const val TAG = "ESIAActivity"
    }
}