package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.gobierno

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothWebSocketBridge
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.PlayerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie.CafeteriaZombieController
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie.FogOfWarRenderer
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity

class EdificioGobierno : AppCompatActivity(),
    BluetoothManager.BluetoothManagerCallback,
    BluetoothGameManager.ConnectionListener,
    OnlineServerManager.WebSocketListener,
    MapView.MapTransitionListener {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var mapView: MapView

    // UI Components
    private lateinit var btnNorth: Button
    private lateinit var btnSouth: Button
    private lateinit var btnEast: Button
    private lateinit var btnWest: Button
    private lateinit var btnBackToHome: Button
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var buttonA: Button
    private lateinit var btnB1: Button
    private lateinit var btnB2: Button

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

    // Controlador para el minijuego del zombie
    private lateinit var zombieController: CafeteriaZombieController

    // Estado del minijuego
    private var zombieGameActive = false
    private var playerScore = 0
    private var gameStartTime = 0L
    private val GAME_DURATION_MS = 60000L // 60 segundos
    private var selectedDifficulty = CafeteriaZombieController.DIFFICULTY_EASY
    private val difficultyNames = mapOf(
        CafeteriaZombieController.DIFFICULTY_EASY to "Fácil",
        CafeteriaZombieController.DIFFICULTY_MEDIUM to "Medio",
        CafeteriaZombieController.DIFFICULTY_HARD to "Difícil"
    )

    private lateinit var fogOfWarRenderer: FogOfWarRenderer
    private var fogOfWarEnabled = false

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
        setContentView(R.layout.activity_edificio_gobierno)

        try {
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_edificio_gobierno
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            initializeComponents(savedInstanceState)

            mapView.post {
                val normalizedMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_EDIFICIO_GOBIERNO)
                mapView.setCurrentMap(normalizedMap, R.drawable.escom_edificio_gobierno)

                mapView.playerManager.apply {
                    setCurrentMap(normalizedMap)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }

                Log.d(TAG, "Set map to: $normalizedMap")

                // Mostrar diálogo de bienvenida al minijuego
                showZombieGameIntroDialog()
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
            gameState.isConnected = intent.getBooleanExtra("IS_CONNECTED", false)
            // Posición inicial válida (zona PATH)
            gameState.playerPosition = Pair(16, 5)
        } else {
            restoreState(savedInstanceState)
        }

        initializeViews()
        initializeManagers()
        setupButtonListeners()

        mapView.apply {
            playerManager.localPlayerId = playerName
            updateLocalPlayerPosition(gameState.playerPosition)
        }

        serverConnectionManager.onlineServerManager.setListener(this)

        // Inicializar el controlador del zombie
        zombieController = CafeteriaZombieController(
            onZombiePositionChanged = { zombieId, position ->
                runOnUiThread {
                    mapView.updateSpecialEntity(
                        zombieId,
                        position,
                        MapMatrixProvider.MAP_EDIFICIO_GOBIERNO
                    )
                    mapView.invalidate()
                }
            },
            onPlayerCaught = {
                onZombieCaughtPlayer()
            }
        )

        connectToOnlineServer()

        fogOfWarRenderer = FogOfWarRenderer()
        mapView.setFogOfWarRenderer(fogOfWarRenderer)

        mapView.playerManager.setEntityVisibilityChecker(object : PlayerManager.EntityVisibilityChecker {
            override fun isEntityVisible(entityId: String, position: Pair<Int, Int>): Boolean {
                return isEntityVisibleThroughFog(entityId, position)
            }
        })
    }

    private fun showZombieGameIntroDialog() {
        val options = arrayOf("Fácil", "Medio", "Difícil")
        var selectedOption = 0

        AlertDialog.Builder(this)
            .setTitle("¡Bienvenido al Edificio de Gobierno!")
            .setMessage("¡Cuidado! Hay zombies burocráticos en el edificio. " +
                    "Sobrevive durante 60 segundos sin ser atrapado.\n\n" +
                    "Usa el botón 'B1' para iniciar el juego y 'A' para interactuar.")
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
                selectedDifficulty = when(which) {
                    1 -> CafeteriaZombieController.DIFFICULTY_MEDIUM
                    2 -> CafeteriaZombieController.DIFFICULTY_HARD
                    else -> CafeteriaZombieController.DIFFICULTY_EASY
                }
            }
            .setPositiveButton("¡Entendido!") { dialog, _ ->
                dialog.dismiss()
                val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
                Toast.makeText(this, "Dificultad seleccionada: $difficultyName", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun startZombieGame() {
        zombieGameActive = true
        playerScore = 0
        gameStartTime = System.currentTimeMillis()

        runOnUiThread {
            val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
            tvBluetoothStatus.text = "MINIJUEGO ACTIVO - Dificultad: $difficultyName"
            btnB1.text = "STOP"
        }

        zombieController.startGame(selectedDifficulty)
        startGameUpdateTimer()

        // Enviar al servidor con el mapa correcto
        sendZombieGameUpdate("start", difficulty = selectedDifficulty)

        if (fogOfWarEnabled) {
            mapView.setFogOfWarEnabled(true)
            runOnUiThread {
                Toast.makeText(this, "¡Cuidado con la niebla! Tu visión es limitada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopZombieGame() {
        mapView.setFogOfWarEnabled(false)

        if (zombieGameActive) {
            zombieController.stopGame()
            zombieGameActive = false

            stopGameUpdateTimer()

            runOnUiThread {
                tvBluetoothStatus.text = "Conectado al servidor online - EDIFICIO GOBIERNO"
                btnB1.text = "B1"
                clearZombieEntities()
                mapView.invalidate()
            }

            sendZombieGameUpdate("stop")
        }
    }

    private fun clearZombieEntities() {
        for (i in 0 until 10) {
            mapView.removeSpecialEntity("zombie_$i")
        }
        mapView.removeSpecialEntity("zombie")
    }

    private fun onZombieCaughtPlayer() {
        if (zombieGameActive) {
            completeZombieGame(false)
        }
    }

    private fun completeZombieGame(survived: Boolean) {
        zombieGameActive = false
        zombieController.stopGame()
        stopGameUpdateTimer()

        val timeElapsed = System.currentTimeMillis() - gameStartTime
        val secondsSurvived = timeElapsed / 1000

        runOnUiThread {
            val message = if (survived) {
                "¡VICTORIA! Has sobrevivido los 60 segundos. Puntuación: $playerScore"
            } else {
                "¡GAME OVER! Un zombie te atrapó. Sobreviviste $secondsSurvived segundos. Puntuación: $playerScore"
            }

            AlertDialog.Builder(this)
                .setTitle("Fin del Minijuego")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    tvBluetoothStatus.text = "Conectado al servidor online - EDIFICIO GOBIERNO"
                    btnB1.text = "B1"
                    clearZombieEntities()
                    mapView.invalidate()
                }
                .setCancelable(false)
                .show()
        }

        sendZombieGameUpdate("complete", survived, secondsSurvived.toInt(), playerScore)
    }

    private fun sendZombieGameUpdate(action: String, survived: Boolean = false, time: Int = 0, score: Int = 0, difficulty: Int = CafeteriaZombieController.DIFFICULTY_EASY) {
        try {
            val message = JSONObject().apply {
                put("type", "zombie_game_update")
                put("action", action)
                put("player", playerName)
                put("map", MapMatrixProvider.MAP_EDIFICIO_GOBIERNO)

                if (action == "start") {
                    put("difficulty", difficulty)
                }

                if (action == "complete") {
                    put("survived", survived)
                    put("time", time)
                    put("score", score)
                }
            }

            serverConnectionManager.onlineServerManager.queueMessage(message.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando actualización del juego: ${e.message}")
        }
    }

    private fun updateGameStatus() {
        if (!zombieGameActive) {
            updateBluetoothStatus("EDIFICIO GOBIERNO - Conectado al servidor")
            return
        }

        val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
        val elapsedTime = (System.currentTimeMillis() - gameStartTime) / 1000
        val remainingTime = GAME_DURATION_MS / 1000 - elapsedTime

        val statusText = "ZOMBIES 🧟‍♂️ | Dif: $difficultyName | Tiempo: ${remainingTime}s | Puntos: $playerScore"
        updateBluetoothStatus(statusText)
    }

    private val gameUpdateHandler = Handler(Looper.getMainLooper())
    private var gameUpdateRunnable: Runnable? = null

    private fun startGameUpdateTimer() {
        gameUpdateRunnable?.let { gameUpdateHandler.removeCallbacks(it) }

        gameUpdateRunnable = object : Runnable {
            override fun run() {
                if (zombieGameActive) {
                    updateGameStatus()

                    val elapsedTime = System.currentTimeMillis() - gameStartTime
                    if (elapsedTime >= GAME_DURATION_MS) {
                        completeZombieGame(true)
                        return
                    }

                    gameUpdateHandler.postDelayed(this, 1000)
                }
            }
        }

        gameUpdateHandler.post(gameUpdateRunnable!!)
    }

    private fun stopGameUpdateTimer() {
        gameUpdateRunnable?.let { gameUpdateHandler.removeCallbacks(it) }
        gameUpdateRunnable = null
    }

    private fun showDifficultySelectionDialog() {
        if (zombieGameActive) {
            Toast.makeText(this, "No puedes cambiar la dificultad durante el juego", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("Fácil", "Medio", "Difícil")
        var selectedOption = selectedDifficulty - 1

        AlertDialog.Builder(this)
            .setTitle("Selecciona la Dificultad")
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
                selectedDifficulty = when(which) {
                    1 -> CafeteriaZombieController.DIFFICULTY_MEDIUM
                    2 -> CafeteriaZombieController.DIFFICULTY_HARD
                    else -> CafeteriaZombieController.DIFFICULTY_EASY
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
                Toast.makeText(this, "Dificultad seleccionada: $difficultyName", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun isEntityVisibleThroughFog(entityId: String, position: Pair<Int, Int>): Boolean {
        if (!fogOfWarEnabled || !zombieGameActive) return true
        if (entityId == playerName) return true

        val visionRadius = getPlayerVisionRadius()
        return fogOfWarRenderer.isEntityVisible(
            position,
            gameState.playerPosition,
            visionRadius
        )
    }

    private fun getPlayerVisionRadius(): Int {
        return when (selectedDifficulty) {
            CafeteriaZombieController.DIFFICULTY_HARD -> 18
            CafeteriaZombieController.DIFFICULTY_MEDIUM -> 12
            else -> 8
        }
    }

    private fun connectToOnlineServer() {
        updateBluetoothStatus("Conectando al servidor online...")

        serverConnectionManager.connectToServer { success ->
            runOnUiThread {
                gameState.isConnected = success

                if (success) {
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        gameState.playerPosition,
                        MapMatrixProvider.MAP_EDIFICIO_GOBIERNO
                    )
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    updateBluetoothStatus("Conectado al servidor online - EDIFICIO GOBIERNO")
                } else {
                    updateBluetoothStatus("Error al conectar al servidor online")
                }
            }
        }
    }

    private fun initializeViews() {
        btnNorth = findViewById(R.id.button_north)
        btnSouth = findViewById(R.id.button_south)
        btnEast = findViewById(R.id.button_east)
        btnWest = findViewById(R.id.button_west)
        btnBackToHome = findViewById(R.id.button_back_to_home)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        buttonA = findViewById(R.id.button_a)
        btnB1 = findViewById(R.id.button_small_1)
        btnB2 = findViewById(R.id.button_small_2)

        tvBluetoothStatus.text = "Edificio Gobierno - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@EdificioGobierno)
        }

        bluetoothBridge = BluetoothWebSocketBridge.getInstance()

        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@EdificioGobierno)
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
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.MAP_MAIN -> {
                returnToMainActivity()
            }
            else -> {
                Log.d(TAG, "Mapa destino no reconocido: $targetMap")
            }
        }
    }

    private var canChangeMap = false
    private var targetMapId: String? = null

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        targetMapId = mapView.getMapTransitionPoint(position.first, position.second)
        canChangeMap = targetMapId != null

        if (canChangeMap) {
            runOnUiThread {
                when (targetMapId) {
                    MapMatrixProvider.MAP_MAIN -> {
                        Toast.makeText(this, "Presiona A para volver al mapa principal", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

        btnBackToHome.setOnClickListener {
            returnToMainActivity()
        }

        btnB1.setOnClickListener {
            if (zombieGameActive) {
                stopZombieGame()
            } else {
                startZombieGame()
            }
        }

        buttonA.setOnClickListener {
            if (canChangeMap && targetMapId != null) {
                mapView.initiateMapTransition(targetMapId!!)
            } else if (zombieGameActive) {
                Toast.makeText(this, "No hay items aquí", Toast.LENGTH_SHORT).show()
            } else {
                showDifficultySelectionDialog()
            }
        }

        btnB2.setOnLongClickListener {
            fogOfWarEnabled = !fogOfWarEnabled
            val message = if (fogOfWarEnabled) "Niebla de guerra activada" else "Niebla de guerra desactivada"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            mapView.invalidate()
            true
        }
    }

    private fun returnToMainActivity() {
        if (zombieGameActive) {
            stopZombieGame()
        }

        val previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
            ?: MapMatrixProvider.MAIN_TO_EDIFICIO_GOBIERNO

        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", previousPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                gameState.playerPosition = position
                mapView.updateLocalPlayerPosition(position, forceCenter = true)

                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.MAP_EDIFICIO_GOBIERNO)
                }

                if (zombieGameActive && this::zombieController.isInitialized) {
                    zombieController.updatePlayerPosition(playerName, position)
                }

                checkPositionForMapChange(position)
            } catch (e: Exception) {
                Log.e(TAG, "Error en updatePlayerPosition: ${e.message}")
            }
        }

        if (fogOfWarEnabled && zombieGameActive) {
            mapView.invalidate()
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
        gameState.remotePlayerName = device.name
        updateBluetoothStatus("Conectado a ${device.name}")
    }

    override fun onBluetoothConnectionFailed(error: String) {
        updateBluetoothStatus("Error: $error")
        showToast(error)
    }

    override fun onConnectionComplete() {
        updateBluetoothStatus("Conexión establecida completamente.")
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
                                val normalizedMap = MapMatrixProvider.normalizeMapName(map)

                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to GameState.PlayerInfo(position, normalizedMap))

                                val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_EDIFICIO_GOBIERNO)
                                if (normalizedMap == currentMap) {
                                    mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                                }
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
                            val map = jsonObject.optString("map", jsonObject.optString("currentmap", MapMatrixProvider.MAP_MAIN))
                            val normalizedMap = MapMatrixProvider.normalizeMapName(map)

                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to GameState.PlayerInfo(position, normalizedMap))

                            val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_EDIFICIO_GOBIERNO)
                            if (normalizedMap == currentMap) {
                                mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                            }
                        }
                    }
                    "zombie_position" -> {
                        val zombieId = jsonObject.optString("id", "zombie")
                        val x = jsonObject.getInt("x")
                        val y = jsonObject.getInt("y")
                        val zombiePosition = Pair(x, y)

                        Log.d(TAG, "Recibida posición de zombie $zombieId en ($x, $y)")

                        if (this::zombieController.isInitialized) {
                            zombieController.setZombiePosition(zombieId, zombiePosition)
                        }

                        mapView.updateSpecialEntity(zombieId, zombiePosition, MapMatrixProvider.MAP_EDIFICIO_GOBIERNO)
                        mapView.invalidate()
                    }
                    "zombie_game_command" -> {
                        when (jsonObject.optString("command")) {
                            "start" -> {
                                if (!zombieGameActive) {
                                    val gameDifficulty = jsonObject.optInt("difficulty", 1)
                                    selectedDifficulty = gameDifficulty
                                    startZombieGame()
                                }
                            }
                            "stop" -> {
                                if (zombieGameActive) {
                                    stopZombieGame()
                                }
                            }
                            "caught" -> {
                                val caughtPlayer = jsonObject.optString("player")
                                if (caughtPlayer == playerName && zombieGameActive) {
                                    completeZombieGame(false)
                                }
                            }
                        }
                    }
                    "join" -> {
                        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                        serverConnectionManager.sendUpdateMessage(
                            playerName,
                            gameState.playerPosition,
                            MapMatrixProvider.MAP_EDIFICIO_GOBIERNO
                        )
                    }
                    "disconnect" -> {
                        val disconnectedId = jsonObject.getString("id")
                        if (disconnectedId != playerName) {
                            gameState.remotePlayerPositions = gameState.remotePlayerPositions - disconnectedId
                            mapView.removeRemotePlayer(disconnectedId)
                        }
                    }
                }
                mapView.invalidate()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}")
            }
        }
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
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
            putBoolean("ZOMBIE_GAME_ACTIVE", zombieGameActive)
            putInt("PLAYER_SCORE", playerScore)
            putLong("GAME_START_TIME", gameStartTime)
        }
    }

    override fun onResume() {
        super.onResume()
        bluetoothManager.reconnect()
        movementManager.setPosition(gameState.playerPosition)

        if (gameState.isConnected) {
            connectToOnlineServer()
        }

        if (gameState.isConnected) {
            serverConnectionManager.sendUpdateMessage(
                playerName,
                gameState.playerPosition,
                MapMatrixProvider.MAP_EDIFICIO_GOBIERNO
            )
        }

        updateRemotePlayersOnMap()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()

        if (zombieGameActive) {
            stopZombieGame()
        }
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

    private fun updateBluetoothStatus(status: String) {
        runOnUiThread {
            tvBluetoothStatus.text = status
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int>
                ?: MapMatrixProvider.getInitialPositionForMap(MapMatrixProvider.MAP_EDIFICIO_GOBIERNO)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions = (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                    as? HashMap<String, GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        zombieGameActive = savedInstanceState.getBoolean("ZOMBIE_GAME_ACTIVE", false)
        playerScore = savedInstanceState.getInt("PLAYER_SCORE", 0)
        gameStartTime = savedInstanceState.getLong("GAME_START_TIME", 0L)

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

        if (zombieGameActive) {
            zombieController.startGame(selectedDifficulty)
        }
    }

    companion object {
        private const val TAG = "EdificioGobierno"
    }
}