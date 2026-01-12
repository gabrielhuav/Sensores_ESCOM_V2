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
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.esia.SalonESIA


class EdificioESIA : AppCompatActivity(),
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
    private var returnToESIAPosition: Pair<Int, Int> = Pair(0, 0) // posición en ESIA para regresar

    data class GameState(
        var isServer: Boolean = false,
        var isConnected: Boolean = false,
        var playerPosition: Pair<Int, Int> = Pair(20, 34), // Posición inicial en el edificio
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

        Log.d(TAG, "=== EdificioESIA onCreate INICIADO ===")

        try {
            Log.d(TAG, "1. Estableciendo layout de EdificioESIA")
            setContentView(R.layout.activity_esia)
            Log.d(TAG, "✓ Layout de EdificioESIA establecido correctamente")

            Log.d(TAG, "2. Creando MapView con imagen de EdificioESIA")
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.edificio_esia
            )
            Log.d(TAG, "✓ MapView con imagen de EdificioESIA creado correctamente")

            Log.d(TAG, "3. Agregando MapView al contenedor")
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)
            Log.d(TAG, "✓ MapView agregado al contenedor")

            Log.d(TAG, "4. Iniciando initializeComponents")
            initializeComponents(savedInstanceState)
            Log.d(TAG, "✓ initializeComponents completado")

            Log.d(TAG, "5. Configurando mapView.post para EdificioESIA")
            mapView.post {
                try {
                    Log.d(TAG, "6. Dentro de mapView.post - configurando mapa EdificioESIA")
                    val normalizedMap = MapMatrixProvider.MAP_EDIFICIO_ESIA
                    Log.d(TAG, "✓ Mapa normalizado: $normalizedMap")

                    mapView.setCurrentMap(normalizedMap, R.drawable.edificio_esia)
                    Log.d(TAG, "✓ Mapa EdificioESIA establecido con imagen correcta")

                    // Configurar el playerManager
                    mapView.playerManager.apply {
                        Log.d(TAG, "7. Configurando playerManager para EdificioESIA")
                        setCurrentMap(normalizedMap)
                        localPlayerId = playerName
                        updateLocalPlayerPosition(gameState.playerPosition)
                        Log.d(TAG, "✓ PlayerManager de EdificioESIA configurado")
                    }

                    // ✅ CONFIGURAR EL MOVEMENT MANAGER TAMBIÉN
                    if (::movementManager.isInitialized) {
                        movementManager.setCurrentMap(normalizedMap)
                        Log.d(TAG, "✓ MovementManager configurado con mapa: $normalizedMap")
                    }

                    Log.d(TAG, "✓ EdificioESIA mapa configurado completamente: $normalizedMap")
                } catch (e: Exception) {
                    Log.e(TAG, "ERROR en mapView.post de EdificioESIA: ${e.message}", e)
                }
            }

            Log.d(TAG, "=== EdificioESIA onCreate COMPLETADO EXITOSAMENTE ===")

        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR CRÍTICO EN ONCREATE DE EDIFICIO ESIA ===")
            Log.e(TAG, "Error: ${e.message}", e)
            Toast.makeText(this, "Error en EdificioESIA: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        Log.d(TAG, "initializeComponents EdificioESIA: INICIO")

        try {
            // 1. Obtener nombre del jugador
            playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
                Toast.makeText(this, "Nombre de jugador no encontrado.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // 2. Configurar estado inicial o restaurar
            if (savedInstanceState == null) {
                // Es una actividad nueva
                gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)

                // VERIFICAR SI VENIMOS DEL SALÓN
                val returnFromSalon = intent.getBooleanExtra("RETURN_FROM_SALON", false)

                if (returnFromSalon) {
                    // CASO A: Regresamos del salón -> Usar coordenadas guardadas
                    val salonReturnX = intent.getIntExtra("SALON_RETURN_X", 20)
                    val salonReturnY = intent.getIntExtra("SALON_RETURN_Y", 34)
                    gameState.playerPosition = Pair(salonReturnX, salonReturnY)
                    Log.d(TAG, "EdificioESIA: Regreso detectado. Posición restaurada: ${gameState.playerPosition}")
                } else {
                    // CASO B: Inicio normal (login) -> Usar entrada principal
                    gameState.playerPosition = Pair(20, 34)
                    Log.d(TAG, "EdificioESIA: Inicio normal. Posición default: (20, 34)")
                }

            } else {
                // Restaurando tras rotación, etc.
                Log.d(TAG, "EdificioESIA: Restaurando estado guardado")
                restoreState(savedInstanceState)
            }

            // 3. Configurar posición de retorno a ESIA (Outdoor)
            val returnX = intent.getIntExtra("RETURN_X", 0)
            val returnY = intent.getIntExtra("RETURN_Y", 0)
            returnToESIAPosition = Pair(returnX, returnY)

            // --- Resto de la inicialización (Views, Managers, etc.) ---
            initializeViews()
            initializeManagers()
            setupInitialConfiguration()

            // Asegurar que el mapa use la posición que acabamos de definir
            mapView.apply {
                playerManager.localPlayerId = playerName
                updateLocalPlayerPosition(gameState.playerPosition)
            }
            Log.d(TAG, "EdificioESIA: ✓ MapView configurado")

            Log.d(TAG, "EdificioESIA: Configurando WebSocket listener")
            serverConnectionManager.onlineServerManager.setListener(this)
            Log.d(TAG, "EdificioESIA: ✓ WebSocket listener configurado")

            Log.d(TAG, "EdificioESIA initializeComponents: COMPLETADO EXITOSAMENTE")

        } catch (e: Exception) {
            Log.e(TAG, "ERROR en EdificioESIA initializeComponents: ${e.message}", e)
            throw e
        }
    }

    private fun initializeViews() {
        Log.d(TAG, "EdificioESIA initializeViews: INICIO")
        try {
            uiManager = UIManager(findViewById(R.id.main_layout), mapView).apply {
                Log.d(TAG, "EdificioESIA: Llamando UIManager.initializeViews()")
                initializeViews()
            }
            Log.d(TAG, "EdificioESIA initializeViews: ✓ COMPLETADO")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en EdificioESIA initializeViews: ${e.message}", e)
            throw e
        }
    }

    private fun initializeManagers() {
        Log.d(TAG, "EdificioESIA initializeManagers: INICIO")
        try {
            bluetoothManager = BluetoothManager.Companion.getInstance(this, uiManager.tvBluetoothStatus).apply {
                setCallback(this@EdificioESIA)
            }

            bluetoothBridge = BluetoothWebSocketBridge.Companion.getInstance()

            val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
                setListener(this@EdificioESIA)
            }

            serverConnectionManager = ServerConnectionManager(
                context = this,
                onlineServerManager = onlineServerManager
            )

            // ✅ SIN PARÁMETRO mapName, IGUAL QUE EN BIBLIOTECA
            movementManager = MovementManager(
                mapView = mapView
            ) { position -> updatePlayerPosition(position) }

            movementManager.setPosition(gameState.playerPosition)

            mapView.playerManager.localPlayerId = playerName
            mapView.setMapTransitionListener(this)
            updatePlayerPosition(gameState.playerPosition)

            Log.d(TAG, "EdificioESIA initializeManagers: COMPLETADO")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR en EdificioESIA initializeManagers: ${e.message}", e)
            throw e
        }
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        Log.d(TAG, "EdificioESIA onMapTransitionRequested: targetMap=$targetMap")
        when (targetMap) {
            MapMatrixProvider.Companion.MAP_ESIA -> {
                Log.d(TAG, "EdificioESIA: Transición a ESIA")
                returnToESIAActivity()
            }
            else -> {
                Log.d(TAG, "EdificioESIA: Mapa destino no reconocido: $targetMap")
            }
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int>
                ?: Pair(20, 34)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions = (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                    as? HashMap<String, GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }
        returnToESIAPosition = savedInstanceState.getSerializable("RETURN_TO_ESIA_POSITION") as? Pair<Int, Int>
            ?: Pair(0, 0)

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
            // Entrada/salida principal del edificio (ajusta según tu matriz)
            position.first == 20 && position.second == 34 -> {
                canChangeMap = true
                targetDestination = "esia"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para regresar a ESIA", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            // Aulas del edificio (ajusta según la distribución de tu edificio)
            // ✅ AGREGAR: Entradas a los salones (fila 28)
            position.first == 4 && position.second == 28 -> {
                canChangeMap = true
                targetDestination = "salon"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al salón", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            position.first == 9 && position.second == 28 -> {
                canChangeMap = true
                targetDestination = "salon"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al salón", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            position.first == 14 && position.second == 28 -> {
                canChangeMap = true
                targetDestination = "salon"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al salón", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            position.first == 18 && position.second == 28 -> {
                canChangeMap = true
                targetDestination = "salon"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al salón", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            position.first == 23 && position.second == 28 -> {
                canChangeMap = true
                targetDestination = "salon"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al salón", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            position.first == 28 && position.second == 28 -> {
                canChangeMap = true
                targetDestination = "salon"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al salón", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            position.first == 33 && position.second == 28 -> {
                canChangeMap = true
                targetDestination = "salon"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al salón", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            position.first == 37 && position.second == 28 -> {
                canChangeMap = true
                targetDestination = "salon"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al salón", Toast.LENGTH_SHORT)
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
                Log.d(TAG, "EdificioESIA: btnConnectDevice click - Regresando a ESIA")
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
                        Log.d(TAG, "EdificioESIA: buttonA click - Regresando a ESIA")
                        returnToESIAActivity()
                    }
                    // Interacciones específicas del edificio
                    canChangeMap && targetDestination == "salon" -> {
                        Log.d(TAG, "EdificioESIA: buttonA click - Entrando al salón")
                        enterSalon()
                    }
                    else -> {
                        showToast("No hay interacción disponible en esta posición")
                    }
                }
            }

            // Botones adicionales
            try {
                findViewById<Button>(R.id.button_back_to_home)?.setOnClickListener {
                    Log.d(TAG, "EdificioESIA: button_back_to_home click - Regresando a ESIA")
                    returnToESIAActivity()
                }

                findViewById<Button>(R.id.button_small_1)?.setOnClickListener {
                    showToast("Horario de clases")
                }

                findViewById<Button>(R.id.button_small_2)?.setOnClickListener {
                    showToast("Lista de profesores")
                }

                findViewById<Button>(R.id.button_serverOnline)?.setOnClickListener {
                    showToast("Server Online - EdificioESIA")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Algunos botones adicionales no encontrados: ${e.message}")
            }
        }
    }

    private fun returnToESIAActivity() {
        Log.d(TAG, "EdificioESIA: returnToESIAActivity INICIO")

        val intent = Intent(this, ESIA::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("RETURN_FROM_EDIFICIO", true) // ✅ AGREGAR bandera
            putExtra("EDIF_RETURN_X", returnToESIAPosition.first)
            putExtra("EDIF_RETURN_Y", returnToESIAPosition.second)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }

        Log.d(TAG, "EdificioESIA: Iniciando actividad ESIA")
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
                    serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.MAP_EDIFICIO_ESIA)
                }

                checkPositionForMapChange(position)
            } catch (e: Exception) {
                Log.e(TAG, "Error en EdificioESIA updatePlayerPosition: ${e.message}")
            }
        }
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) {
            movementManager.handleMovement(event, deltaX, deltaY)
        }
    }
    private fun enterSalon() {
        val intent = Intent(this, SalonESIA::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected)
            putExtra("INITIAL_POSITION", Pair(20, 20)) // Posición inicial en el salón
            putExtra("RETURN_X", gameState.playerPosition.first) // Posición actual para regresar
            putExtra("RETURN_Y", gameState.playerPosition.second)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }
        startActivity(intent)
        finish()
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
            putSerializable("RETURN_TO_ESIA_POSITION", returnToESIAPosition)

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
            // ✅ CONFIGURAR EL MAPA CORRECTO
            movementManager.setCurrentMap(MapMatrixProvider.MAP_EDIFICIO_ESIA)
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
                        // ✅ CONFIGURAR EL MAPA CORRECTO
                        movementManager.setCurrentMap(MapMatrixProvider.MAP_EDIFICIO_ESIA)
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
        private const val TAG = "EdificioESIAActivity"
    }
}

private fun MovementManager.setCurrentMap(
    mapEdificioEsia: String
) {
}