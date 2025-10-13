package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.cafeteria

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
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.PlayerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie.FogOfWarRenderer
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie.ZombieGameManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity


class Cafeteria : AppCompatActivity(),
    BluetoothManager.BluetoothManagerCallback,
    BluetoothGameManager.ConnectionListener,
    OnlineServerManager.WebSocketListener,
    MapView.MapTransitionListener {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var mapView: MapView

    // UI
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

    // Reutilizamos GameState de BuildingNumber2
    private var gameState = BuildingNumber2.GameState()

    // === Minijuego estado/UI ===
    private var zombieGameActive = false
    private var playerScore = 0
    private var gameStartTime = 0L
    private val GAME_DURATION_MS = 60000L // 60s
    private var selectedDifficulty = ZombieGameManager.DIFFICULTY_EASY
    private val difficultyNames = mapOf(
        ZombieGameManager.DIFFICULTY_EASY to "Fácil",
        ZombieGameManager.DIFFICULTY_MEDIUM to "Medio",
        ZombieGameManager.DIFFICULTY_HARD to "Difícil"
    )

    // Control de transición de mapa vía botón A
    private var canChangeMap = false
    private var targetMapId: String? = null
    private var interactivePosition: Pair<Int, Int>? = null

    private lateinit var fogOfWarRenderer: FogOfWarRenderer
    private var fogOfWarEnabled = false
    private val otherPlayersInZombieGame = mutableSetOf<String>()

    // === Listener global de zombies (para dibujar solo los de este mapa) ===
    private val zombieListener = object : ZombieGameManager.Listener {
        override fun onZombiePosition(zombieId: String, mapId: String, position: Pair<Int, Int>) {
            val normalized = MapMatrixProvider.normalizeMapName(mapId)
            if (normalized == MapMatrixProvider.MAP_CAFETERIA) {
                runOnUiThread {
                    mapView.updateSpecialEntity(zombieId, position, normalized)
                    mapView.invalidate()
                }
            } else {
                runOnUiThread { mapView.removeSpecialEntity(zombieId) }
            }
        }

        override fun onPlayerCaught(victimId: String) {
            if (victimId == playerName && zombieGameActive) {
                completeZombieGame(false)
            }
        }

        override fun onGameStopped(reason: String) {
            runOnUiThread {
                mapView.removeSpecialEntitiesByPrefix("zombie_")
                mapView.removeSpecialEntity("zombie") // por si dejaste algún id “plano”
                mapView.setFogOfWarEnabled(false)
                tvBluetoothStatus.text = "Conectado al servidor online - CAFE ESCOM"
                btnB1.text = "B1"
                mapView.invalidate()
            }
        }


    }
    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        targetMapId = mapView.getMapTransitionPoint(position.first, position.second)
        interactivePosition = if (targetMapId != null) position else null
        canChangeMap = targetMapId != null

        if (canChangeMap) {
            val msg = when (targetMapId) {
                MapMatrixProvider.MAP_MAIN -> "Presiona A para salir al mapa principal"
                else -> "Presiona A para interactuar"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cafeteria)

        try {
            // Inicializar el mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_cafeteria
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            // Inicializar componentes
            initializeComponents(savedInstanceState)

            // Listener del manager global
            ZombieGameManager.addListener(zombieListener)

            // Esperar a que el mapView esté listo
            mapView.post {
                // Setear mapa cafetería
                mapView.setCurrentMap(MapMatrixProvider.MAP_CAFETERIA, R.drawable.escom_cafeteria)

                // Player manager
                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.MAP_CAFETERIA)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }

                Log.d(TAG, "Set map to: ${MapMatrixProvider.MAP_CAFETERIA}")

                // Enviar posición inicial a server
                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        gameState.playerPosition,
                        MapMatrixProvider.MAP_CAFETERIA
                    )
                }

                // Presentación del minijuego
                showZombieGameIntroDialog()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error inicializando la actividad.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun changeDifficultyDuringGame() {
        if (!zombieGameActive) {
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
                selectedDifficulty = when (selectedOption) {
                    1 -> ZombieGameManager.DIFFICULTY_MEDIUM
                    2 -> ZombieGameManager.DIFFICULTY_HARD
                    else -> ZombieGameManager.DIFFICULTY_EASY
                }
                stopZombieGame()
                startZombieGame()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        // Datos de Intent / estado
        playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Nombre de jugador no encontrado.", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        if (savedInstanceState == null) {
            gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
            gameState.isConnected = intent.getBooleanExtra("IS_CONNECTED", false)
            gameState.playerPosition =
                intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
                    ?: Pair(18, 34)
        } else {
            restoreState(savedInstanceState)
        }

        // Vistas y managers
        initializeViews()
        initializeManagers()
        setupButtonListeners()

        // Inicializar jugador y avisar al manager global dónde está
        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(gameState.playerPosition)

        // Re-conectar a servidor online
        connectToOnlineServer()

        // Fog of war
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
            .setTitle("¡Bienvenido a la Cafetería ESCOM!")
            .setMessage(
                "¡Cuidado! Hay zombies hambrientos en la cafetería. " +
                        "Sobrevive durante 60 segundos sin ser atrapado.\n\n" +
                        "Usa 'B1' para iniciar/stop y 'A' para recoger comida que los ralentiza."
            )
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
                selectedDifficulty = when (which) {
                    1 -> ZombieGameManager.DIFFICULTY_MEDIUM
                    2 -> ZombieGameManager.DIFFICULTY_HARD
                    else -> ZombieGameManager.DIFFICULTY_EASY
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

        // UI
        runOnUiThread {
            val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
            tvBluetoothStatus.text = "MINIJUEGO ACTIVO - Dificultad: $difficultyName"
            btnB1.text = "STOP"
        }

        // Iniciar manager global
        ZombieGameManager.startGame(selectedDifficulty)

        // Timer UI
        startGameUpdateTimer()

        // Notificar server (opcional, conservamos)
        sendZombieGameUpdate("start", difficulty = selectedDifficulty)

        // Fog of war
        if (fogOfWarEnabled) {
            mapView.setFogOfWarEnabled(true)
            runOnUiThread {
                Toast.makeText(this, "¡Cuidado con la niebla! Tu visión es limitada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopZombieGame() {
        mapView.setFogOfWarEnabled(false)
        mapView.removeSpecialEntitiesByPrefix("zombie_")
        mapView.removeSpecialEntity("zombie")
        mapView.invalidate()

        if (zombieGameActive) {
            ZombieGameManager.stopGame()
            zombieGameActive = false

            stopGameUpdateTimer()

            runOnUiThread {
                tvBluetoothStatus.text = "Conectado al servidor online - CAFE ESCOM"
                btnB1.text = "B1"
                clearZombieEntities()
                mapView.invalidate()
            }

            sendZombieGameUpdate("stop")
        }
    }

    private fun clearZombieEntities() {
        for (i in 0 until 12) {
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
        ZombieGameManager.stopGame()
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
                    tvBluetoothStatus.text = "Conectado al servidor online - CAFE ESCOM"
                    btnB1.text = "B1"
                    clearZombieEntities()
                    mapView.invalidate()
                }
                .setCancelable(false)
                .show()
        }

        sendZombieGameUpdate("complete", survived, secondsSurvived.toInt(), playerScore)
    }

    private fun sendZombieGameUpdate(
        action: String,
        survived: Boolean = false,
        time: Int = 0,
        score: Int = 0,
        difficulty: Int = ZombieGameManager.DIFFICULTY_EASY
    ) {
        try {
            val message = JSONObject().apply {
                put("type", "zombie_game_update")
                put("action", action)
                put("player", playerName)
                put("map", MapMatrixProvider.MAP_CAFETERIA)
                if (action == "start") put("difficulty", difficulty)
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

    private fun checkForFoodItem(position: Pair<Int, Int>) {
        if (zombieGameActive) {
            val foodSpots = listOf(
                Pair(12, 8),  Pair(12, 32),
                Pair(28, 8),  Pair(28, 32),
                Pair(20, 8)
            )

            if (foodSpots.any { it.first == position.first && it.second == position.second }) {
                playerScore += 10
                Toast.makeText(this, "¡Recogiste comida! +10 puntos", Toast.LENGTH_SHORT).show()

                // Ralentizar zombies globales
                ZombieGameManager.slowDownZombies(3000)

                val message = JSONObject().apply {
                    put("type", "zombie_game_food")
                    put("player", playerName)
                    put("x", position.first)
                    put("y", position.second)
                    put("score", playerScore)
                }
                serverConnectionManager.onlineServerManager.queueMessage(message.toString())
            }
        }
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
                selectedDifficulty = when (which) {
                    1 -> ZombieGameManager.DIFFICULTY_MEDIUM
                    2 -> ZombieGameManager.DIFFICULTY_HARD
                    else -> ZombieGameManager.DIFFICULTY_EASY
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
                val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
                Toast.makeText(this, "Dificultad seleccionada: $difficultyName", Toast.LENGTH_SHORT).show()
            }
            .show()
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
                        MapMatrixProvider.MAP_CAFETERIA
                    )
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    updateBluetoothStatus("Conectado al servidor online - CAFE ESCOM")
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

        tvBluetoothStatus.text = "Cafeteria - Conectando..."
    }

    // Fog of war: ¿es visible algo?
    private fun isEntityVisibleThroughFog(entityId: String, position: Pair<Int, Int>): Boolean {
        if (!fogOfWarEnabled || !zombieGameActive) return true
        if (entityId == playerName) return true
        val visionRadius = getPlayerVisionRadius()
        return fogOfWarRenderer.isEntityVisible(position, gameState.playerPosition, visionRadius)
    }

    private fun getPlayerVisionRadius(): Int {
        return when (selectedDifficulty) {
            ZombieGameManager.DIFFICULTY_HARD -> 18
            ZombieGameManager.DIFFICULTY_MEDIUM -> 12
            else -> 8
        }
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@Cafeteria)
        }
        bluetoothBridge = BluetoothWebSocketBridge.getInstance()

        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@Cafeteria)
        }
        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        movementManager = MovementManager(mapView = mapView) { position -> updatePlayerPosition(position) }

        mapView.setMapTransitionListener(this)
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

        btnBackToHome.setOnClickListener { returnToBuilding2() }

        btnB1.setOnClickListener {
            if (zombieGameActive) stopZombieGame() else startZombieGame()
        }
        btnB1.setOnLongClickListener {
            debugForceZombie()
            true
        }

        buttonA.setOnClickListener {
            // 1) ¿Estamos en un punto interactivo (puerta)?
            if (canChangeMap && targetMapId != null) {
                mapView.initiateMapTransition(targetMapId!!)
                return@setOnClickListener
            }

            // 2) Lógica existente
            if (zombieGameActive) {
                checkForFoodItem(gameState.playerPosition)
            } else {
                showDifficultySelectionDialog()
            }
        }


        buttonA.setOnLongClickListener {
            if (zombieGameActive) {
                changeDifficultyDuringGame()
                true
            } else false
        }

        btnB2.setOnClickListener {
            returnToMainActivity()
        }

        btnB2.setOnLongClickListener {
            fogOfWarEnabled = !fogOfWarEnabled
            mapView.setFogOfWarEnabled(fogOfWarEnabled)   // <── FALTABA ESTA LÍNEA
            Toast.makeText(this,
                if (fogOfWarEnabled) "Niebla de guerra activada" else "Niebla de guerra desactivada",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }


    private fun returnToMainActivity() {


        mapView.setFogOfWarEnabled(false)
        mapView.removeSpecialEntitiesByPrefix("zombie_")
        mapView.removeSpecialEntity("zombie")
        zombieGameActive = false

        // ► Indica al manager que ahora la horda debe perseguir en el main.
        ZombieGameManager.transferHordeToMap(
            MapMatrixProvider.MAP_MAIN,
            MapMatrixProvider.CAFETERIA_TO_MAIN_POSITION   // o Pair(33, 34) si prefieres
        )

        val previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
            ?: Pair(33, 34)

        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected)
            putExtra("INITIAL_POSITION", previousPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }





    private fun returnToBuilding2() {
        if (zombieGameActive) stopZombieGame()

        val previousPosition =
            intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int> ?: Pair(15, 16)

        val intent = Intent(this, BuildingNumber2::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected)
            putExtra("INITIAL_POSITION", previousPosition)
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
                    MapMatrixProvider.MAP_CAFETERIA
                )
            }

            // MUY IMPORTANTE: avisar al manager global dónde está el jugador
            ZombieGameManager.updatePlayer(
                playerName,
                MapMatrixProvider.MAP_CAFETERIA,
                position
            )
        }
        if (fogOfWarEnabled && zombieGameActive) {
            mapView.invalidate()
        }
        checkPositionForMapChange(position)
        tvBluetoothStatus.text = "CAFETERÍA (${position.first}, ${position.second})"
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int>
                ?: Pair(20, 20)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions =
                (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                        as? HashMap<String, BuildingNumber2.GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        zombieGameActive = savedInstanceState.getBoolean("ZOMBIE_GAME_ACTIVE", false)
        playerScore = savedInstanceState.getInt("PLAYER_SCORE", 0)
        gameStartTime = savedInstanceState.getLong("GAME_START_TIME", 0L)

        if (gameState.isConnected) {
            connectToOnlineServer()
        }

        if (zombieGameActive) {
            ZombieGameManager.startGame(selectedDifficulty)
        }
    }

    private fun updateGameStatus() {
        if (!zombieGameActive) {
            updateBluetoothStatus("CAFETERÍA ESCOM - Conectado al servidor")
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

    // MapTransitionListener
    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.MAP_MAIN -> {
                returnToMainActivity()
            }
            MapMatrixProvider.MAP_BUILDING2 -> {
                returnToBuilding2()
            }
            else -> Log.d(TAG, "Mapa destino no reconocido: $targetMap")
        }
    }

    // ===== Bluetooth callbacks =====
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
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.MAP_CAFETERIA)
            mapView.invalidate()
        }
    }

    private fun debugForceZombie() {
        val debugPosition = Pair(20, 20)
        mapView.playerManager.updateSpecialEntity("zombie", debugPosition, MapMatrixProvider.MAP_CAFETERIA)
        mapView.invalidate()
        Toast.makeText(this, "DEBUG: Zombie forzado en (20, 20)", Toast.LENGTH_LONG).show()

        val count = mapView.playerManager.getSpecialEntitiesCount()
        Log.d(TAG, "DEBUG: Entidades especiales registradas: $count")
        mapView.playerManager.logSpecialEntities()
    }

    private fun forceZombieGameWithFixedZombie() {
        if (zombieGameActive) ZombieGameManager.stopGame()

        zombieGameActive = true
        playerScore = 0
        gameStartTime = System.currentTimeMillis()

        runOnUiThread {
            tvBluetoothStatus.text = "MINIJUEGO ACTIVO (FORZADO) - Zombie en posición fija"
            btnB1.text = "STOP"
        }

        val zombiePosition = Pair(20, 20)
        mapView.playerManager.updateSpecialEntity("zombie", zombiePosition, MapMatrixProvider.MAP_CAFETERIA)
        mapView.invalidate()

        val debugMessage = "Juego zombie forzado con zombie en $zombiePosition"
        Toast.makeText(this, debugMessage, Toast.LENGTH_LONG).show()
        Log.d(TAG, debugMessage)

        movementManager.scheduleDelayedAction(GAME_DURATION_MS) {
            if (zombieGameActive) {
                completeZombieGame(true)
            }
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
                                val position = Pair(playerData.getInt("x"), playerData.getInt("y"))
                                val mapStr = playerData.optString("map", playerData.optString("currentMap", "main"))
                                val normalizedMap = MapMatrixProvider.normalizeMapName(mapStr)

                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to BuildingNumber2.GameState.PlayerInfo(position, normalizedMap))

                                val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_CAFETERIA)
                                if (normalizedMap == currentMap) {
                                    mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                                }

                                // Avisar al manager global (clave para persecución)
                                ZombieGameManager.updatePlayer(playerId, normalizedMap, position)
                            }
                        }
                    }
                    "update" -> {
                        val playerId = jsonObject.getString("id")
                        if (playerId != playerName) {
                            val position = Pair(jsonObject.getInt("x"), jsonObject.getInt("y"))
                            val mapStr = jsonObject.optString("map", jsonObject.optString("currentmap", "main"))
                            val normalizedMap = MapMatrixProvider.normalizeMapName(mapStr)

                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to BuildingNumber2.GameState.PlayerInfo(position, normalizedMap))

                            val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_CAFETERIA)
                            Log.d(TAG, "Jugador remoto $playerId en mapa '$normalizedMap', mapa actual '$currentMap'")

                            if (normalizedMap == currentMap) {
                                mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                            }

                            // Avisar al manager global
                            ZombieGameManager.updatePlayer(playerId, normalizedMap, position)
                        }
                    }
                    "zombie_position" -> {
                        // Compatibilidad si llega por server; solo reflejamos visualmente en cafetería
                        val zombieId = jsonObject.optString("id", "zombie")
                        val x = jsonObject.getInt("x")
                        val y = jsonObject.getInt("y")
                        val zombiePosition = Pair(x, y)
                        Log.d(TAG, "Posición zombie recibida $zombieId en ($x,$y)")
                        mapView.updateSpecialEntity(zombieId, zombiePosition, MapMatrixProvider.MAP_CAFETERIA)
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
                                if (zombieGameActive) stopZombieGame()
                            }
                            "caught" -> {
                                val caughtPlayer = jsonObject.optString("player")
                                if (caughtPlayer == playerName && zombieGameActive) {
                                    completeZombieGame(false)
                                }
                            }
                            "zombie_slowed" -> {
                                if (zombieGameActive) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this,
                                            "¡${jsonObject.optString("player")} recogió comida! Zombies ralentizados",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            "zombie_speed_normal" -> {
                                if (zombieGameActive) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this,
                                            "¡Los zombies recuperan su velocidad!",
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
                            MapMatrixProvider.MAP_CAFETERIA
                        )
                    }
                    "disconnect" -> {
                        val disconnectedId = jsonObject.getString("id")
                        if (disconnectedId != playerName) {
                            gameState.remotePlayerPositions = gameState.remotePlayerPositions - disconnectedId
                            mapView.removeRemotePlayer(disconnectedId)
                            // Quitar del manager global
                            ZombieGameManager.removePlayer(disconnectedId)
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

    private fun normalizeMapId(mapId: String): String {
        return when {
            mapId.contains("cafeteria") -> MapMatrixProvider.MAP_CAFETERIA
            mapId.contains("salon2009") -> MapMatrixProvider.MAP_SALON2009
            mapId.contains("salon2010") -> MapMatrixProvider.MAP_SALON2010
            mapId.contains("building2") -> MapMatrixProvider.MAP_BUILDING2
            mapId.contains("main") -> MapMatrixProvider.MAP_MAIN
            else -> mapId
        }
    }

    private fun updateBluetoothStatus(status: String) {
        runOnUiThread { tvBluetoothStatus.text = status }
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
            putBoolean("ZOMBIE_GAME_ACTIVE", zombieGameActive)
            putInt("PLAYER_SCORE", playerScore)
            putLong("GAME_START_TIME", gameStartTime)
        }
    }

    override fun onResume() {
        super.onResume()
        movementManager.setPosition(gameState.playerPosition)

        if (gameState.isConnected) {
            connectToOnlineServer()
            serverConnectionManager.sendUpdateMessage(
                playerName,
                gameState.playerPosition,
                MapMatrixProvider.MAP_CAFETERIA
            )
        }

        // Re-afirmar posición al manager global al volver a primer plano
        ZombieGameManager.updatePlayer(
            playerName,
            MapMatrixProvider.MAP_CAFETERIA,
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

    companion object {
        private const val TAG = "CafeteriaESCOM"
    }
}
