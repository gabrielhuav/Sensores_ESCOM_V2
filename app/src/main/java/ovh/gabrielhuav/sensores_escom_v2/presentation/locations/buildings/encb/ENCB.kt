package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.encb

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
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
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber2
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.rabbit.ENCBRabbitController
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.Zacatenco

class ENCB : AppCompatActivity(),
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

    private var gameState = BuildingNumber2.GameState()

    // Controlador para el minijuego de conejos
    private lateinit var encbRabbitController: ENCBRabbitController

    // Estado del minijuego
    private var rabbitGameActive = false
    private var rabbitsCollected = 0
    private var gameStartTime = 0L
    private val GAME_DURATION_MS = 60000L
    private var selectedDifficulty = ENCBRabbitController.DIFFICULTY_EASY
    private val difficultyNames = mapOf(
        ENCBRabbitController.DIFFICULTY_EASY to "Fácil",
        ENCBRabbitController.DIFFICULTY_MEDIUM to "Medio",
        ENCBRabbitController.DIFFICULTY_HARD to "Difícil"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encb)

        try {
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.encb
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            initializeComponents(savedInstanceState)

            mapView.post {
                mapView.setCurrentMap(MapMatrixProvider.MAP_ENCB, R.drawable.encb)

                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.MAP_ENCB)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }

                Log.d(TAG, "Set map to: ${MapMatrixProvider.MAP_ENCB}")

                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        gameState.playerPosition,
                        MapMatrixProvider.MAP_ENCB
                    )
                }

                showRabbitGameIntroDialog()

                // DEBUG TEMPORAL: Forzar conejos después de 2 segundos para verificar visualización
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "🔧 DEBUG AUTO: Intentando forzar conejos para verificar visualización")
                    debugForceRabbits()
                }, 2000)
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
            gameState.playerPosition = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
                ?: Pair(20, 20)
        } else {
            restoreState(savedInstanceState)
        }

        initializeViews()
        initializeManagers()
        setupButtonListeners()

        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(gameState.playerPosition)

        // CRÍTICO: Inicializar el controlador con el callback correcto
        encbRabbitController = ENCBRabbitController(
            onRabbitPositionChanged = { rabbitId, position ->
                // IMPORTANTE: Ejecutar en el hilo UI
                runOnUiThread {
                    Log.d(TAG, "🐰 Actualizando conejo $rabbitId a posición $position")

                    // Actualizar la entidad especial en el mapa
                    mapView.updateSpecialEntity(
                        rabbitId,
                        position,
                        MapMatrixProvider.MAP_ENCB
                    )

                    // Forzar redibujado inmediato
                    mapView.invalidate()
                }
            },
            onRabbitCaught = { rabbitId ->
                onPlayerCaughtRabbit(rabbitId)
            }
        )

        Log.d(TAG, "✅ RabbitController inicializado correctamente")

        connectToOnlineServer()
    }

    private fun showRabbitGameIntroDialog() {
        val options = arrayOf("Fácil", "Medio", "Difícil")
        var selectedOption = 0

        AlertDialog.Builder(this)
            .setTitle("¡Bienvenido a la ENCB!")
            .setMessage("¡Hora de atrapar conejos! 🐰\n\n" +
                    "Tu objetivo es atrapar tantos conejos como puedas en 60 segundos.\n\n" +
                    "• Usa el botón 'B1' para iniciar el juego\n" +
                    "• Usa el botón 'A' para atrapar un conejo cuando estés cerca\n" +
                    "• Los conejos huirán de ti, ¡sé rápido y estratégico!\n" +
                    "• Los conejos atrapados respawnearán en nuevas posiciones\n\n" +
                    "Selecciona la dificultad:")
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
                selectedDifficulty = when(which) {
                    1 -> ENCBRabbitController.DIFFICULTY_MEDIUM
                    2 -> ENCBRabbitController.DIFFICULTY_HARD
                    else -> ENCBRabbitController.DIFFICULTY_EASY
                }
            }
            .setPositiveButton("¡Comenzar!") { dialog, _ ->
                dialog.dismiss()
                val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
                Toast.makeText(this, "Dificultad seleccionada: $difficultyName", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun startRabbitGame() {
        rabbitGameActive = true
        rabbitsCollected = 0
        gameStartTime = System.currentTimeMillis()

        runOnUiThread {
            val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
            tvBluetoothStatus.text = "MINIJUEGO ACTIVO - Dificultad: $difficultyName"
            btnB1.text = "STOP"
        }

        encbRabbitController.startGame(selectedDifficulty)
        startGameUpdateTimer()
        sendRabbitGameUpdate("start", difficulty = selectedDifficulty)

        Log.d(TAG, "Juego de conejos iniciado")
    }

    private fun stopRabbitGame() {
        if (rabbitGameActive) {
            encbRabbitController.stopGame()
            rabbitGameActive = false

            stopGameUpdateTimer()

            runOnUiThread {
                tvBluetoothStatus.text = "Conectado al servidor online - ENCB"
                btnB1.text = "B1"

                clearRabbitEntities()
                mapView.invalidate()
            }

            sendRabbitGameUpdate("stop")
        }
    }

    private fun clearRabbitEntities() {
        for (i in 0 until 20) {
            mapView.removeSpecialEntity("rabbit_$i")
        }
    }

    private fun onPlayerCaughtRabbit(rabbitId: String) {
        if (rabbitGameActive) {
            rabbitsCollected++

            runOnUiThread {
                Toast.makeText(this, "¡Conejo atrapado! 🐰 Total: $rabbitsCollected", Toast.LENGTH_SHORT).show()
            }

            sendRabbitGameUpdate("rabbit_caught", rabbitsCaught = rabbitsCollected)
        }
    }

    private fun completeRabbitGame(timeUp: Boolean) {
        rabbitGameActive = false
        encbRabbitController.stopGame()
        stopGameUpdateTimer()

        val timeElapsed = System.currentTimeMillis() - gameStartTime
        val secondsPlayed = timeElapsed / 1000

        runOnUiThread {
            val message = if (timeUp) {
                "¡TIEMPO TERMINADO! 🎉\n\nConejos atrapados: $rabbitsCollected\nTiempo: 60 segundos"
            } else {
                "Juego detenido.\n\nConejos atrapados: $rabbitsCollected\nTiempo: $secondsPlayed segundos"
            }

            AlertDialog.Builder(this)
                .setTitle("Fin del Minijuego")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    tvBluetoothStatus.text = "Conectado al servidor online - ENCB"
                    btnB1.text = "B1"
                    clearRabbitEntities()
                    mapView.invalidate()
                }
                .setCancelable(false)
                .show()
        }

        sendRabbitGameUpdate("complete", timeUp, secondsPlayed.toInt(), rabbitsCollected)
    }

    private fun sendRabbitGameUpdate(
        action: String,
        survived: Boolean = false,
        time: Int = 0,
        score: Int = 0,
        rabbitsCaught: Int = 0,
        difficulty: Int = ENCBRabbitController.DIFFICULTY_EASY
    ) {
        try {
            val message = JSONObject().apply {
                put("type", "rabbit_game_update")
                put("action", action)
                put("player", playerName)
                put("map", MapMatrixProvider.MAP_ENCB)

                when (action) {
                    "start" -> put("difficulty", difficulty)
                    "complete" -> {
                        put("timeUp", survived)
                        put("time", time)
                        put("rabbitsCollected", rabbitsCaught)
                    }
                    "rabbit_caught" -> put("rabbitsCollected", rabbitsCaught)
                }
            }

            serverConnectionManager.onlineServerManager.queueMessage(message.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando actualización del juego: ${e.message}")
        }
    }

    private fun tryToCatchRabbit() {
        if (!rabbitGameActive) {
            Toast.makeText(this, "Inicia el juego primero (botón B1)", Toast.LENGTH_SHORT).show()
            return
        }

        val playerPos = gameState.playerPosition
        val caughtRabbit = encbRabbitController.tryToCatchRabbit(playerPos)

        if (caughtRabbit != null) {
            onPlayerCaughtRabbit(caughtRabbit)
        } else {
            Toast.makeText(this, "¡No hay conejos cerca!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeDifficultyDuringGame() {
        if (!rabbitGameActive) {
            showDifficultySelectionDialog()
            return
        }

        val options = arrayOf("Fácil", "Medio", "Difícil")
        var selectedOption = selectedDifficulty - 1

        AlertDialog.Builder(this)
            .setTitle("Cambiar Dificultad")
            .setMessage("Cambiar la dificultad reiniciará el juego actual. ¿Deseas continuar?")
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
            }
            .setPositiveButton("Cambiar") { dialog, _ ->
                selectedDifficulty = when(selectedOption) {
                    1 -> ENCBRabbitController.DIFFICULTY_MEDIUM
                    2 -> ENCBRabbitController.DIFFICULTY_HARD
                    else -> ENCBRabbitController.DIFFICULTY_EASY
                }
                stopRabbitGame()
                startRabbitGame()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDifficultySelectionDialog() {
        if (rabbitGameActive) {
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
                    1 -> ENCBRabbitController.DIFFICULTY_MEDIUM
                    2 -> ENCBRabbitController.DIFFICULTY_HARD
                    else -> ENCBRabbitController.DIFFICULTY_EASY
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
                Toast.makeText(this, "Dificultad seleccionada: $difficultyName", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateRemotePlayersForRabbits() {
        if (rabbitGameActive && this::encbRabbitController.isInitialized) {
            encbRabbitController.updatePlayerPosition(playerName, gameState.playerPosition)

            gameState.remotePlayerPositions.forEach { (playerId, playerInfo) ->
                if (MapMatrixProvider.normalizeMapName(playerInfo.map) == MapMatrixProvider.MAP_ENCB) {
                    encbRabbitController.updatePlayerPosition(playerId, playerInfo.position)
                }
            }
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
                        MapMatrixProvider.MAP_ENCB
                    )
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    updateBluetoothStatus("Conectado al servidor online - ENCB")
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

        tvBluetoothStatus.text = "ENCB - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@ENCB)
        }

        bluetoothBridge = BluetoothWebSocketBridge.getInstance()

        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@ENCB)
        }

        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        movementManager = MovementManager(
            mapView = mapView
        ) { position -> updatePlayerPosition(position) }

        mapView.setMapTransitionListener(this)
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

        btnBackToHome.setOnClickListener {
            returnToZacatencoActivity()
        }

        btnB1.setOnClickListener {
            if (rabbitGameActive) {
                stopRabbitGame()
            } else {
                startRabbitGame()
            }
        }

        btnB1.setOnLongClickListener {
            debugForceRabbits()
            true
        }

        buttonA.setOnClickListener {
            if (rabbitGameActive) {
                tryToCatchRabbit()
            } else {
                showDifficultySelectionDialog()
            }
        }

        buttonA.setOnLongClickListener {
            if (rabbitGameActive) {
                changeDifficultyDuringGame()
                return@setOnLongClickListener true
            }
            return@setOnLongClickListener false
        }
    }

    private fun returnToZacatencoActivity() {
        if (rabbitGameActive) {
            stopRabbitGame()
        }

        // Obtener la posición previa del intent
        val previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
            ?: Pair(24, 12) // Posición por defecto si no hay previa

        val intent = Intent(this, Zacatenco::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", previousPosition) // Usar la posición previa
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        movementManager.handleMovement(event, deltaX, deltaY)
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            gameState.playerPosition = position
            mapView.updateLocalPlayerPosition(position)
            mapView.forceRecenterOnPlayer()

            if (gameState.isConnected) {
                serverConnectionManager.sendUpdateMessage(
                    playerName,
                    position,
                    MapMatrixProvider.MAP_ENCB
                )
            }

            if (rabbitGameActive && this::encbRabbitController.isInitialized) {
                encbRabbitController.updatePlayerPosition(playerName, position)
            }
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int>
                ?: Pair(20, 20)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions = (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                    as? HashMap<String, BuildingNumber2.GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        rabbitGameActive = savedInstanceState.getBoolean("RABBIT_GAME_ACTIVE", false)
        rabbitsCollected = savedInstanceState.getInt("RABBITS_COLLECTED", 0)
        gameStartTime = savedInstanceState.getLong("GAME_START_TIME", 0L)

        if (gameState.isConnected) {
            connectToOnlineServer()
        }

        if (rabbitGameActive) {
            encbRabbitController.startGame()
        }
    }

    private fun updateGameStatus() {
        if (!rabbitGameActive) {
            updateBluetoothStatus("ENCB - Conectado al servidor")
            return
        }

        val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
        val elapsedTime = (System.currentTimeMillis() - gameStartTime) / 1000
        val remainingTime = GAME_DURATION_MS / 1000 - elapsedTime

        val statusText = "CONEJOS 🐰 | Dif: $difficultyName | Tiempo: ${remainingTime}s | Atrapados: $rabbitsCollected"
        updateBluetoothStatus(statusText)
    }

    private val gameUpdateHandler = Handler(Looper.getMainLooper())
    private var gameUpdateRunnable: Runnable? = null

    private fun startGameUpdateTimer() {
        gameUpdateRunnable?.let { gameUpdateHandler.removeCallbacks(it) }

        gameUpdateRunnable = object : Runnable {
            override fun run() {
                if (rabbitGameActive) {
                    updateGameStatus()

                    val elapsedTime = System.currentTimeMillis() - gameStartTime
                    if (elapsedTime >= GAME_DURATION_MS) {
                        completeRabbitGame(true)
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

    private fun debugForceRabbits() {
        Log.d(TAG, "🔧 DEBUG: Forzando conejos en el mapa")

        // Crear conejos en posiciones visibles y válidas
        val debugPositions = listOf(
            Pair(20, 20),  // Centro
            Pair(22, 20),  // Derecha
            Pair(20, 22),  // Abajo
            Pair(18, 20),  // Izquierda
            Pair(20, 18)   // Arriba
        )

        debugPositions.forEachIndexed { index, position ->
            val rabbitId = "rabbit_$index"

            // PASO 1: Actualizar en el PlayerManager
            mapView.playerManager.updateSpecialEntity(
                rabbitId,
                position,
                MapMatrixProvider.MAP_ENCB
            )

            Log.d(TAG, "🐰 DEBUG: Conejo $rabbitId forzado en $position")
        }

        // PASO 2: Forzar redibujado inmediato (doble invalidación)
        mapView.invalidate()
        mapView.postInvalidate()

        // PASO 3: Verificar que se registraron
        val count = mapView.playerManager.getSpecialEntitiesCount()
        Log.d(TAG, "📊 DEBUG: Total entidades especiales: $count")
        mapView.playerManager.logSpecialEntities()

        // PASO 4: Si hay un RabbitController, actualizar también ahí
        if (this::encbRabbitController.isInitialized) {
            debugPositions.forEachIndexed { index, position ->
                encbRabbitController.setRabbitPosition("rabbit_$index", position)
            }
            Log.d(TAG, "✅ DEBUG: RabbitController también actualizado")
        }

        // PASO 5: Mensaje informativo más detallado
        Toast.makeText(
            this,
            "DEBUG: ${debugPositions.size} conejos forzados\nEntidades: $count\nRevisa el Logcat",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.MAP_ZACATENCO -> returnToZacatencoActivity()
            else -> Log.d(TAG, "Mapa destino no reconocido: $targetMap")
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

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            val deviceName = device.name ?: "Unknown"
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.MAP_ENCB)
            mapView.invalidate()
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                Log.d(TAG, "WebSocket message received: $message")
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

                                val mapStr = playerData.optString("map", playerData.optString("currentMap", "main"))
                                val normalizedMap = MapMatrixProvider.normalizeMapName(mapStr)

                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to BuildingNumber2.GameState.PlayerInfo(position, normalizedMap))

                                val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_ENCB)

                                if (normalizedMap == currentMap) {
                                    mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                                    Log.d(TAG, "Updated remote player $playerId in map $normalizedMap")
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

                            val mapStr = jsonObject.optString("map", jsonObject.optString("currentmap", "main"))
                            val normalizedMap = MapMatrixProvider.normalizeMapName(mapStr)

                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to BuildingNumber2.GameState.PlayerInfo(position, normalizedMap))

                            val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_ENCB)

                            Log.d(TAG, "Jugador remoto $playerId en mapa '$normalizedMap', mapa actual es '$currentMap'")

                            if (normalizedMap == currentMap) {
                                mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                                Log.d(TAG, "Updated remote player $playerId in map $normalizedMap")
                            }
                        }
                    }
                    "rabbit_position" -> {
                        val rabbitId = jsonObject.optString("id", "rabbit")
                        val x = jsonObject.getInt("x")
                        val y = jsonObject.getInt("y")
                        val rabbitPosition = Pair(x, y)

                        Log.d(TAG, "🐰 ¡Recibida posición de conejo $rabbitId en ($x, $y)!")

                        // PASO 1: Actualizar en el controlador
                        if (this::encbRabbitController.isInitialized) {
                            encbRabbitController.setRabbitPosition(rabbitId, rabbitPosition)
                        }

                        // PASO 2: Actualizar la entidad especial en el mapa
                        mapView.updateSpecialEntity(rabbitId, rabbitPosition, MapMatrixProvider.MAP_ENCB)

                        // PASO 3: Forzar redibujado inmediato
                        mapView.invalidate()

                        Log.d(TAG, "✅ Conejo $rabbitId actualizado correctamente")
                    }

                    "rabbit_game_command" -> {
                        when (jsonObject.optString("command")) {
                            "start" -> {
                                if (!rabbitGameActive) {
                                    val gameDifficulty = jsonObject.optInt("difficulty", 1)
                                    selectedDifficulty = gameDifficulty
                                    startRabbitGame()
                                }
                            }
                            "stop" -> {
                                if (rabbitGameActive) {
                                    stopRabbitGame()
                                }
                            }
                            "rabbit_caught" -> {
                                val catcher = jsonObject.optString("player")
                                if (catcher != playerName && rabbitGameActive) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this,
                                            "¡$catcher atrapó un conejo!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                    "join" -> {
                        serverConnectionManager.onlineServerManager.requestPositionsUpdate()

                        serverConnectionManager.sendUpdateMessage(
                            playerName,
                            gameState.playerPosition,
                            MapMatrixProvider.MAP_ENCB
                        )
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

    private fun updateBluetoothStatus(status: String) {
        runOnUiThread {
            tvBluetoothStatus.text = status
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean("IS_SERVER", gameState.isServer)
            putBoolean("IS_CONNECTED", gameState.isConnected)
            putSerializable("PLAYER_POSITION", gameState.playerPosition)
            putSerializable("REMOTE_PLAYER_POSITIONS", HashMap(gameState.remotePlayerPositions))
            putString("REMOTE_PLAYER_NAME", gameState.remotePlayerName)

            putBoolean("RABBIT_GAME_ACTIVE", rabbitGameActive)
            putInt("RABBITS_COLLECTED", rabbitsCollected)
            putLong("GAME_START_TIME", gameStartTime)
        }
    }

    override fun onResume() {
        super.onResume()
        movementManager.setPosition(gameState.playerPosition)

        if (gameState.isConnected) {
            connectToOnlineServer()
        }

        if (gameState.isConnected) {
            serverConnectionManager.sendUpdateMessage(
                playerName,
                gameState.playerPosition,
                MapMatrixProvider.MAP_ENCB
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()

        if (rabbitGameActive) {
            stopRabbitGame()
        }
    }

    override fun onPause() {
        super.onPause()
        movementManager.stopMovement()
    }

    companion object {
        private const val TAG = "ENCB"
    }
}