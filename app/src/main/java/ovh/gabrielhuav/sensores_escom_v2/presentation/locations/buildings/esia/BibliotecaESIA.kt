package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.esia

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.components.UIManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.ESIA

class BibliotecaESIA : AppCompatActivity(),
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
    private var returnToESIAPosition: Pair<Int, Int> = Pair(6, 31)
    data class GameState(
        var isServer: Boolean = false,
        var isConnected: Boolean = false,
        var playerPosition: Pair<Int, Int> = Pair(38, 19), // Posición inicial en la entrada

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

        Log.d(TAG, "=== BibliotecaESIA onCreate INICIADO ===")

        try {
            Log.d(TAG, "1. Estableciendo layout de Biblioteca")
            setContentView(R.layout.activity_esia)
            Log.d(TAG, "✓ Layout de Biblioteca establecido correctamente")

            Log.d(TAG, "2. Creando MapView con imagen de Biblioteca")
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.biblioteca_esia
            )
            Log.d(TAG, "✓ MapView con imagen de Biblioteca creado correctamente")

            Log.d(TAG, "3. Agregando MapView al contenedor")
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)
            Log.d(TAG, "✓ MapView agregado al contenedor")

            Log.d(TAG, "4. Iniciando initializeComponents")
            initializeComponents(savedInstanceState)
            Log.d(TAG, "✓ initializeComponents completado")

            Log.d(TAG, "5. Configurando mapView.post para Biblioteca")
            mapView.post {
                try {
                    Log.d(TAG, "6. Dentro de mapView.post - configurando mapa Biblioteca")
                    val normalizedMap = "biblioteca_esia"
                    Log.d(TAG, "✓ Mapa normalizado: $normalizedMap")

                    mapView.setCurrentMap(normalizedMap, R.drawable.biblioteca_esia)
                    Log.d(TAG, "✓ Mapa Biblioteca establecido con imagen correcta")

                    // Configurar el playerManager
                    mapView.playerManager.apply {
                        Log.d(TAG, "7. Configurando playerManager para Biblioteca")
                        setCurrentMap(normalizedMap)
                        localPlayerId = playerName
                        updateLocalPlayerPosition(gameState.playerPosition)
                        Log.d(TAG, "✓ PlayerManager de Biblioteca configurado")
                    }

                    Log.d(TAG, "✓ Biblioteca mapa configurado completamente: $normalizedMap")
                } catch (e: Exception) {
                    Log.e(TAG, "ERROR en mapView.post de Biblioteca: ${e.message}", e)
                }
            }

            Log.d(TAG, "=== BibliotecaESIA onCreate COMPLETADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR CRÍTICO EN ONCREATE DE BIBLIOTECA ===")
            Log.e(TAG, "Error: ${e.message}", e)
            Toast.makeText(this, "Error en Biblioteca: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        Log.d(TAG, "initializeComponents Biblioteca: INICIO")

        try {
            // Obtener datos desde Intent
            Log.d(TAG, "Biblioteca: Obteniendo PLAYER_NAME del Intent")
            playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
                Log.e(TAG, "Biblioteca: PLAYER_NAME no encontrado en Intent")
                Toast.makeText(this, "Nombre de jugador no encontrado.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            val returnX = intent.getIntExtra("RETURN_X", 6)
            val returnY = intent.getIntExtra("RETURN_Y", 31)
            returnToESIAPosition = Pair(returnX, returnY)
            Log.d(TAG, "Biblioteca: ✓ PLAYER_NAME obtenido: $playerName")

            if (savedInstanceState == null) {
                Log.d(TAG, "Biblioteca: Inicializando desde Intent")
                gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
                gameState.playerPosition =
                    (intent.getParcelableExtra("INITIAL_POSITION") ?: Pair(38, 19)) as Pair<Int, Int>
                Log.d(TAG, "Biblioteca: ✓ Estado inicial - isServer: ${gameState.isServer}, position: ${gameState.playerPosition}")
            } else {
                Log.d(TAG, "Biblioteca: Restaurando estado guardado")
                restoreState(savedInstanceState)
            }

            Log.d(TAG, "Biblioteca: Llamando initializeViews()")
            initializeViews()
            Log.d(TAG, "Biblioteca: ✓ initializeViews completado")

            Log.d(TAG, "Biblioteca: Llamando initializeManagers()")
            initializeManagers()
            Log.d(TAG, "Biblioteca: ✓ initializeManagers completado")

            Log.d(TAG, "Biblioteca: Llamando setupInitialConfiguration()")
            setupInitialConfiguration()
            Log.d(TAG, "Biblioteca: ✓ setupInitialConfiguration completado")

            Log.d(TAG, "Biblioteca: Configurando mapView")
            mapView.apply {
                playerManager.localPlayerId = playerName
                updateLocalPlayerPosition(gameState.playerPosition)
            }
            Log.d(TAG, "Biblioteca: ✓ MapView configurado")

            Log.d(TAG, "Biblioteca: Configurando WebSocket listener")
            serverConnectionManager.onlineServerManager.setListener(this)
            Log.d(TAG, "Biblioteca: ✓ WebSocket listener configurado")

            Log.d(TAG, "Biblioteca initializeComponents: COMPLETADO EXITOSAMENTE")

        } catch (e: Exception) {
            Log.e(TAG, "ERROR en Biblioteca initializeComponents: ${e.message}", e)
            throw e
        }
    }

    private fun initializeViews() {
        Log.d(TAG, "Biblioteca initializeViews: INICIO")
        try {
            uiManager = UIManager(findViewById(R.id.main_layout), mapView).apply {
                Log.d(TAG, "Biblioteca: Llamando UIManager.initializeViews()")
                initializeViews()
            }
            Log.d(TAG, "Biblioteca initializeViews: ✓ COMPLETADO")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en Biblioteca initializeViews: ${e.message}", e)
            throw e
        }
    }

    private fun initializeManagers() {
        Log.d(TAG, "Biblioteca initializeManagers: INICIO")
        try {
            bluetoothManager = BluetoothManager.Companion.getInstance(this, uiManager.tvBluetoothStatus).apply {
                setCallback(this@BibliotecaESIA)
            }

            bluetoothBridge = BluetoothWebSocketBridge.Companion.getInstance()

            val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
                setListener(this@BibliotecaESIA)
            }

            serverConnectionManager = ServerConnectionManager(
                context = this,
                onlineServerManager = onlineServerManager
            )

            movementManager = MovementManager(
                mapView = mapView
            ) { position -> updatePlayerPosition(position) }

            movementManager.setPosition(gameState.playerPosition)

            mapView.playerManager.localPlayerId = playerName
            mapView.setMapTransitionListener(this)
            updatePlayerPosition(gameState.playerPosition)

            Log.d(TAG, "Biblioteca initializeManagers: COMPLETADO")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en Biblioteca initializeManagers: ${e.message}", e)
            throw e
        }
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        Log.d(TAG, "Biblioteca onMapTransitionRequested: targetMap=$targetMap")
        when (targetMap) {
            MapMatrixProvider.Companion.MAP_ESIA -> {
                Log.d(TAG, "Biblioteca: Transición a ESIA")
                returnToESIAActivity()
            }
            else -> {
                Log.d(TAG, "Biblioteca: Mapa destino no reconocido: $targetMap")
            }
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int>
                ?: Pair(38, 19)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions = (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                    as? HashMap<String, GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }
        returnToESIAPosition = savedInstanceState.getSerializable("RETURN_TO_ESIA_POSITION") as? Pair<Int, Int>
            ?: Pair(6, 31)

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
            // Entrada/salida principal de la biblioteca
            position.first == 38 && position.second == 19 -> {
                canChangeMap = true
                targetDestination = "esia"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para regresar a ESIA", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            // Zona de lectura (área con mesas circulares)
            position.first in 18..22 && position.second in 8..12 -> {
                canChangeMap = false
                runOnUiThread {
                    Toast.makeText(this, "Zona de lectura - Presiona A para estudiar", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            // Zona de estanterías (rectángulos de la izquierda)
            position.first in 2..12 && position.second in 5..20 -> {
                canChangeMap = false
                runOnUiThread {
                    Toast.makeText(this, "Estanterías - Presiona A para buscar libros", Toast.LENGTH_SHORT)
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
                Log.d(TAG, "Biblioteca: btnConnectDevice click - Regresando a ESIA")
                returnToESIAActivity()
            }
            setupMovementButtons(movementManager)

            btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
            btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
            btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
            btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

            buttonA.setOnClickListener {
                val currentPosition = gameState.playerPosition
                when {
                    canChangeMap && targetDestination == "esia" -> {
                        Log.d(TAG, "Biblioteca: buttonA click - Regresando a ESIA")
                        returnToESIAActivity()
                    }
                    // Interacciones específicas de la biblioteca
                    currentPosition.first in 18..22 && currentPosition.second in 8..12 -> {
                        showToast("Estudiando en la zona de lectura...")
                    }
                    currentPosition.first in 2..12 && currentPosition.second in 5..20 -> {
                        showToast("Buscando libros en las estanterías...")
                    }
                    else -> {
                        showToast("No hay interacción disponible en esta posición")
                    }
                }
            }

            // Botones adicionales
            try {
                findViewById<Button>(R.id.button_back_to_home)?.setOnClickListener {
                    Log.d(TAG, "Biblioteca: button_back_to_home click - Regresando a ESIA")
                    returnToESIAActivity()
                }

                findViewById<Button>(R.id.button_small_1)?.setOnClickListener {
                    showToast("Catálogo de libros")
                }

                findViewById<Button>(R.id.button_small_2)?.setOnClickListener {
                    showToast("Reservar sala de estudio")
                }

                findViewById<Button>(R.id.button_serverOnline)?.setOnClickListener {
                    showToast("Server Online - Biblioteca")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Algunos botones adicionales no encontrados: ${e.message}")
            }
        }
    }

    private fun returnToESIAActivity() {
        Log.d(TAG, "Biblioteca: returnToESIAActivity INICIO")

        val intent = Intent(this, ESIA::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("RETURN_FROM_BIBLIOTECA", true) // ✅ AGREGAR bandera
            putExtra("BIBLIOTECA_RETURN_X", returnToESIAPosition.first)
            putExtra("BIBLIOTECA_RETURN_Y", returnToESIAPosition.second)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }

        Log.d(TAG, "Biblioteca: Iniciando actividad ESIA")
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
                    serverConnectionManager.sendUpdateMessage(playerName, position, "biblioteca_esia")
                }

                checkPositionForMapChange(position)
            } catch (e: Exception) {
                Log.e(TAG, "Error en Biblioteca updatePlayerPosition: ${e.message}")
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
        private const val TAG = "BibliotecaESIAActivity"
    }
}