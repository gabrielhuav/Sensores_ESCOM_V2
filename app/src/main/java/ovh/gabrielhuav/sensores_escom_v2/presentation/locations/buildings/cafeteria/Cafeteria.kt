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
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie.CafeteriaZombieController
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.zombie.FogOfWarRenderer

class Cafeteria : AppCompatActivity(),
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
    private lateinit var buttonA: Button  // Botón A para interactuar con el minijuego
    private lateinit var btnB1: Button    // Botón auxiliar 1
    private lateinit var btnB2: Button    // Botón auxiliar 2 (BCK)

    private lateinit var playerName: String
    private lateinit var bluetoothBridge: BluetoothWebSocketBridge

    // Reutilizamos la misma estructura de GameState que BuildingNumber2
    private var gameState = BuildingNumber2.GameState()

    // Controlador para el minijuego del zombie
    private lateinit var cafeteriaZombieController: CafeteriaZombieController

    // Estado del minijuego
    private var zombieGameActive = false
    private var playerScore = 0
    private var gameStartTime = 0L
    private val GAME_DURATION_MS = 60000L // 60 segundos de duración del juego
    private var selectedDifficulty = CafeteriaZombieController.Companion.DIFFICULTY_EASY
    private val difficultyNames = mapOf(
        CafeteriaZombieController.Companion.DIFFICULTY_EASY to "Fácil",
        CafeteriaZombieController.Companion.DIFFICULTY_MEDIUM to "Medio",
        CafeteriaZombieController.Companion.DIFFICULTY_HARD to "Difícil"
    )

    private lateinit var fogOfWarRenderer: FogOfWarRenderer
    private var fogOfWarEnabled = false // Flag para activar/desactivar la niebla
    private val otherPlayersInZombieGame = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cafeteria)

        try {
            // Inicializar el mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_cafeteria // Usa la imagen de la cafe de la ESCOM
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            // Inicializar componentes
            initializeComponents(savedInstanceState)

            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa para la cafe de la ESCOM
                mapView.setCurrentMap(MapMatrixProvider.Companion.MAP_CAFETERIA, R.drawable.escom_cafeteria)

                // Configurar el playerManager
                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.Companion.MAP_CAFETERIA)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }

                Log.d(TAG, "Set map to: " + MapMatrixProvider.Companion.MAP_CAFETERIA)

                // Importante: Enviar un update inmediato para que otros jugadores sepan dónde estamos
                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, MapMatrixProvider.Companion.MAP_CAFETERIA)
                }

                // Mostrar diálogo de bienvenida al minijuego
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
            // Si no hay juego activo, usa el selector normal
            showDifficultySelectionDialog()
            return
        }

        // Si hay un juego activo, muestra opciones pero avisa que reiniciará el juego
        val options = arrayOf("Fácil", "Medio", "Difícil")
        var selectedOption = selectedDifficulty - 1 // Índice basado en 0

        AlertDialog.Builder(this)
            .setTitle("Cambiar Dificultad")
            .setMessage("Cambiar la dificultad reiniciará el juego actual. ¿Deseas continuar?")
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
            }
            .setPositiveButton("Cambiar") { dialog, _ ->
                // Actualizar dificultad
                selectedDifficulty = when(selectedOption) {
                    1 -> CafeteriaZombieController.Companion.DIFFICULTY_MEDIUM
                    2 -> CafeteriaZombieController.Companion.DIFFICULTY_HARD
                    else -> CafeteriaZombieController.Companion.DIFFICULTY_EASY
                }

                // Reiniciar el juego con la nueva dificultad
                stopZombieGame()
                startZombieGame()

                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        // Obtener datos desde Intent o restaurar el estado guardado
        playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Nombre de jugador no encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (savedInstanceState == null) {
            // Inicializar desde el Intent
            gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
            gameState.isConnected = intent.getBooleanExtra("IS_CONNECTED", false) // Preservar estado de conexión
            gameState.playerPosition = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
                ?: Pair(20, 20)
        } else {
            restoreState(savedInstanceState)
        }

        // Inicializar vistas y gestores de lógica
        initializeViews()
        initializeManagers()
        setupButtonListeners()

        // Inicializar el jugador
        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(gameState.playerPosition)

        // Inicializar el controlador del zombie
        cafeteriaZombieController = CafeteriaZombieController(
            onZombiePositionChanged = { zombieId, position ->
                // Cuando el zombie cambia de posición, lo dibujamos en el mapa
                runOnUiThread {
                    mapView.updateSpecialEntity(
                        zombieId,
                        position,
                        MapMatrixProvider.Companion.MAP_CAFETERIA
                    )
                    mapView.invalidate()
                }
            },
            onPlayerCaught = {
                // Cuando el zombie atrapa al jugador
                onZombieCaughtPlayer()
            }
        )

        // Asegurarnos de que nos reconectamos al servidor online
        // Este es un paso importante para mantener la conexión
        connectToOnlineServer()

        // Inicializar el renderizador de niebla de guerra
        fogOfWarRenderer = FogOfWarRenderer()

        // Proporcionar el renderizador al MapView
        mapView.setFogOfWarRenderer(fogOfWarRenderer)

        // Configurar el checker de visibilidad para el PlayerManager
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
            .setMessage("¡Cuidado! Hay zombies hambrientos en la cafetería. " +
                    "Sobrevive durante 60 segundos sin ser atrapado.\n\n" +
                    "Usa el botón 'B1' para iniciar el juego y 'A' para recoger comida " +
                    "que ralentizará a los zombies.")
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
                selectedDifficulty = when(which) {
                    1 -> CafeteriaZombieController.Companion.DIFFICULTY_MEDIUM
                    2 -> CafeteriaZombieController.Companion.DIFFICULTY_HARD
                    else -> CafeteriaZombieController.Companion.DIFFICULTY_EASY
                }
            }
            .setPositiveButton("¡Entendido!") { dialog, _ ->
                dialog.dismiss()

                // Mostrar la dificultad seleccionada
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

        // Actualizar UI
        runOnUiThread {
            val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
            tvBluetoothStatus.text = "MINIJUEGO ACTIVO - Dificultad: $difficultyName"
            btnB1.text = "STOP"
        }

        // Iniciar el controlador del zombie con la dificultad seleccionada
        cafeteriaZombieController.startGame(selectedDifficulty)

        // Iniciar timer de actualización
        startGameUpdateTimer()

        // Informar al servidor que el juego ha iniciado
        sendZombieGameUpdate("start", difficulty = selectedDifficulty)

        // Activar la niebla de guerra cuando inicia el juego
        if (fogOfWarEnabled) {
            mapView.setFogOfWarEnabled(true)
            // Actualizar la información en la UI
            runOnUiThread {
                Toast.makeText(this, "¡Cuidado con la niebla! Tu visión es limitada.", Toast.LENGTH_SHORT).show()
            }
        }

        // Programar fin del juego (si no es atrapado) - Ya manejado por el timer
    }

    private fun stopZombieGame() {
        // Desactivar la niebla de guerra cuando termina el juego
        mapView.setFogOfWarEnabled(false)

        if (zombieGameActive) {
            cafeteriaZombieController.stopGame()
            zombieGameActive = false

            // Detener timer
            stopGameUpdateTimer()

            // Actualizar UI
            runOnUiThread {
                tvBluetoothStatus.text = "Conectado al servidor online - CAFE ESCOM"
                btnB1.text = "B1"

                // Limpiar los zombies del mapa
                clearZombieEntities()
                mapView.invalidate()
            }

            // Informar al servidor
            sendZombieGameUpdate("stop")
        }
    }

    private fun clearZombieEntities() {
        // Buscar y eliminar todos los zombies (zombie_0, zombie_1, etc.)
        for (i in 0 until 10) { // Máximo razonable de zombies
            mapView.removeSpecialEntity("zombie_$i")
        }
        // También eliminar el zombie original si existe
        mapView.removeSpecialEntity("zombie")
    }

    private fun onZombieCaughtPlayer() {
        // El zombie atrapó al jugador
        if (zombieGameActive) {
            completeZombieGame(false)
        }
    }

    private fun completeZombieGame(survived: Boolean) {
        zombieGameActive = false
        cafeteriaZombieController.stopGame()

        // Detener timer
        stopGameUpdateTimer()

        val timeElapsed = System.currentTimeMillis() - gameStartTime
        val secondsSurvived = timeElapsed / 1000

        // Mostrar resultado
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
                    // Actualizar UI
                    tvBluetoothStatus.text = "Conectado al servidor online - CAFE ESCOM"
                    btnB1.text = "B1"

                    // Limpiar el zombie del mapa
                    clearZombieEntities()
                    mapView.invalidate()
                }
                .setCancelable(false)
                .show()
        }

        // Informar al servidor
        sendZombieGameUpdate("complete", survived, secondsSurvived.toInt(), playerScore)
    }

    private fun sendZombieGameUpdate(action: String, survived: Boolean = false, time: Int = 0, score: Int = 0, difficulty: Int = CafeteriaZombieController.Companion.DIFFICULTY_EASY) {
        try {
            val message = JSONObject().apply {
                put("type", "zombie_game_update")
                put("action", action)
                put("player", playerName)
                put("map", MapMatrixProvider.Companion.MAP_CAFETERIA)

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

    // 7. Añadir método para actualizar las posiciones de los jugadores remotos en el zombieController
    private fun updateRemotePlayersForZombies() {
        // Esta función debe ser llamada cuando se detecten cambios en las posiciones de jugadores remotos
        if (zombieGameActive && this::cafeteriaZombieController.isInitialized) {
            // Actualizar la posición del jugador local
            cafeteriaZombieController.updatePlayerPosition(playerName, gameState.playerPosition)

            // Actualizar las posiciones de jugadores remotos
            gameState.remotePlayerPositions.forEach { (playerId, playerInfo) ->
                // Solo considerar jugadores en el mismo mapa (la cafetería)
                if (MapMatrixProvider.Companion.normalizeMapName(playerInfo.map) == MapMatrixProvider.Companion.MAP_CAFETERIA) {
                    cafeteriaZombieController.updatePlayerPosition(playerId, playerInfo.position)
                }
            }
        }
    }

    private fun checkForFoodItem(position: Pair<Int, Int>) {
        // Verificar si hay comida en la posición actual
        if (zombieGameActive) {
            // Coordenadas donde hay comida (zonas de comida en la cafetería)
            val foodSpots = listOf(
                Pair(12, 8), // Tacos
                Pair(12, 32), // Más tacos
                Pair(28, 8), // Burrito
                Pair(28, 32), // Chile
                Pair(20, 8) // Guacamole
            )

            if (foodSpots.any { it.first == position.first && it.second == position.second }) {
                // Encontró comida
                playerScore += 10

                // Mostrar mensaje
                Toast.makeText(this, "¡Recogiste comida! +10 puntos", Toast.LENGTH_SHORT).show()

                // Ralentizar a los zombies
                cafeteriaZombieController.slowDownZombies(3000) // 3 segundos más lentos

                // Informar al servidor
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
        var selectedOption = selectedDifficulty - 1 // Índice basado en 0

        AlertDialog.Builder(this)
            .setTitle("Selecciona la Dificultad")
            .setSingleChoiceItems(options, selectedOption) { _, which ->
                selectedOption = which
                selectedDifficulty = when(which) {
                    1 -> CafeteriaZombieController.Companion.DIFFICULTY_MEDIUM
                    2 -> CafeteriaZombieController.Companion.DIFFICULTY_HARD
                    else -> CafeteriaZombieController.Companion.DIFFICULTY_EASY
                }
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()

                // Mostrar la dificultad seleccionada
                val difficultyName = difficultyNames[selectedDifficulty] ?: "Fácil"
                Toast.makeText(this, "Dificultad seleccionada: $difficultyName", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun connectToOnlineServer() {
        // Mostrar estado de conexión
        updateBluetoothStatus("Conectando al servidor online...")

        serverConnectionManager.connectToServer { success ->
            runOnUiThread {
                gameState.isConnected = success

                if (success) {
                    // Enviar mensaje de unión y posición actual
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)

                    // Enviar inmediatamente la posición actual con el mapa correcto
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        gameState.playerPosition,
                        MapMatrixProvider.Companion.MAP_CAFETERIA
                    )

                    // Solicitar actualizaciones de posición
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()

                    updateBluetoothStatus("Conectado al servidor online - CAFE ESCOM")
                } else {
                    updateBluetoothStatus("Error al conectar al servidor online")
                }
            }
        }
    }

    private fun initializeViews() {
        // Obtener referencias a los botones de movimiento
        btnNorth = findViewById(R.id.button_north)
        btnSouth = findViewById(R.id.button_south)
        btnEast = findViewById(R.id.button_east)
        btnWest = findViewById(R.id.button_west)
        btnBackToHome = findViewById(R.id.button_back_to_home)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        buttonA = findViewById(R.id.button_a)
        btnB1 = findViewById(R.id.button_small_1)
        btnB2 = findViewById(R.id.button_small_2)

        // Cambiar el título para indicar dónde estamos
        tvBluetoothStatus.text = "Cafeteria - Conectando..."
    }


    /**
     * Determina si una entidad está visible a través de la niebla de guerra
     */
    private fun isEntityVisibleThroughFog(entityId: String, position: Pair<Int, Int>): Boolean {
        // Si la niebla no está activada o el juego no está activo, todas las entidades son visibles
        if (!fogOfWarEnabled || !zombieGameActive) return true

        // El jugador local siempre es visible
        if (entityId == playerName) return true

        // Comprobar si la entidad está dentro del rango de visión del jugador
        val visionRadius = getPlayerVisionRadius()
        return fogOfWarRenderer.isEntityVisible(
            position,
            gameState.playerPosition,
            visionRadius
        )
    }

    /**
     * Obtiene el radio de visión del jugador basado en la dificultad del juego
     */
    private fun getPlayerVisionRadius(): Int {
        // Usar el mismo rango de detección que los zombies en dificultad difícil
        return when (selectedDifficulty) {
            CafeteriaZombieController.Companion.DIFFICULTY_HARD -> 18
            CafeteriaZombieController.Companion.DIFFICULTY_MEDIUM -> 12
            else -> 8
        }
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.Companion.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@Cafeteria)
        }

        bluetoothBridge = BluetoothWebSocketBridge.Companion.getInstance()

        val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
            setListener(this@Cafeteria)
        }

        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        // Configurar el MovementManager
        movementManager = MovementManager(
            mapView = mapView
        ) { position -> updatePlayerPosition(position) }

        // Configurar listener de transición
        mapView.setMapTransitionListener(this)
    }

    private fun setupButtonListeners() {
        // Configurar los botones de movimiento
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

        // Botón para volver al edificio 2
        btnBackToHome.setOnClickListener {
            returnToBuilding2()
        }

//        // Botón BCK para volver
//        btnB2.setOnClickListener {
//            returnToBuilding2()
//        }

        // Botón B1 para iniciar/detener el minijuego
        btnB1.setOnClickListener {
            if (zombieGameActive) {
                stopZombieGame()
            } else {
                startZombieGame()
            }
        }

        //Agregar un botón de depuración separado o un onLongClickListener:
        btnB1.setOnLongClickListener {
            debugForceZombie()
            true // Solo en setOnLongClickListener se debe retornar true
        }

        buttonA.setOnClickListener {
            if (zombieGameActive) {
                // Si el juego está activo, buscar comida
                val currentPosition = gameState.playerPosition
                checkForFoodItem(currentPosition)
            } else {
                // Si el juego no está activo, mostrar diálogo de dificultad
                showDifficultySelectionDialog()
            }
        }

        buttonA.setOnLongClickListener {
            // Si se mantiene A presionado mientras el juego está activo, mostrar opciones de dificultad
            if (zombieGameActive) {
                changeDifficultyDuringGame()
                return@setOnLongClickListener true
            }
            return@setOnLongClickListener false
        }

        btnB2.setOnLongClickListener {
            fogOfWarEnabled = !fogOfWarEnabled
            val message = if (fogOfWarEnabled) "Niebla de guerra activada" else "Niebla de guerra desactivada"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            mapView.invalidate() // Forzar redibujado del mapa
            true // Consumir el evento
        }

    }

    private fun returnToBuilding2() {
        // Si hay un juego activo, detenerlo primero
        if (zombieGameActive) {
            stopZombieGame()
        }

        // Obtener la posición previa
        val previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
            ?: Pair(15, 16) // Por defecto, volver al pasillo principal

        // Crear intent para volver al Edificio 2
        val intent = Intent(this, BuildingNumber2::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected) // Pasar el estado de conexión
            putExtra("INITIAL_POSITION", previousPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // Limpiar datos
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

            // Actualizar posición del jugador y forzar centrado
            mapView.updateLocalPlayerPosition(position)
            mapView.forceRecenterOnPlayer() // Forzar explícitamente el centrado aquí

            // Enviar actualización a otros jugadores con el mapa específico
            if (gameState.isConnected) {
                // Enviar la posición con el nombre del mapa correcto
                serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.Companion.MAP_CAFETERIA)
            }

            // Si el minijuego está activo, actualizar la posición del jugador para el zombie
            if (zombieGameActive && this::cafeteriaZombieController.isInitialized) {
                cafeteriaZombieController.updatePlayerPosition(playerName, position)
            }
        }
        if (fogOfWarEnabled && zombieGameActive) {
            mapView.invalidate()
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

        // Restaurar estado del minijuego
        zombieGameActive = savedInstanceState.getBoolean("ZOMBIE_GAME_ACTIVE", false)
        playerScore = savedInstanceState.getInt("PLAYER_SCORE", 0)
        gameStartTime = savedInstanceState.getLong("GAME_START_TIME", 0L)

        // Reconectar si es necesario
        if (gameState.isConnected) {
            connectToOnlineServer()
        }

        // Restaurar el minijuego si estaba activo
        if (zombieGameActive) {
            cafeteriaZombieController.startGame()
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

        // Mostrar tiempo restante, puntuación y dificultad
        val statusText = "ZOMBIES 🧟‍♂️ | Dif: $difficultyName | Tiempo: ${remainingTime}s | Puntos: $playerScore"
        updateBluetoothStatus(statusText)
    }

    private val gameUpdateHandler = Handler(Looper.getMainLooper())
    private var gameUpdateRunnable: Runnable? = null

    private fun startGameUpdateTimer() {
        // Detener timer existente si hay
        gameUpdateRunnable?.let { gameUpdateHandler.removeCallbacks(it) }

        gameUpdateRunnable = object : Runnable {
            override fun run() {
                if (zombieGameActive) {
                    updateGameStatus()

                    // Verificar si el tiempo se ha agotado
                    val elapsedTime = System.currentTimeMillis() - gameStartTime
                    if (elapsedTime >= GAME_DURATION_MS) {
                        completeZombieGame(true)
                        return
                    }

                    gameUpdateHandler.postDelayed(this, 1000) // Actualizar cada segundo
                }
            }
        }

        // Iniciar timer
        gameUpdateHandler.post(gameUpdateRunnable!!)
    }

    private fun stopGameUpdateTimer() {
        gameUpdateRunnable?.let { gameUpdateHandler.removeCallbacks(it) }
        gameUpdateRunnable = null
    }

    // Implementación MapTransitionListener
    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.Companion.MAP_BUILDING2 -> {
                returnToBuilding2()
            }
            else -> {
                Log.d(TAG, "Mapa destino no reconocido: $targetMap")
            }
        }
    }

    // Callbacks de Bluetooth
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
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.Companion.MAP_CAFETERIA)
            mapView.invalidate()
        }
    }

    private fun debugForceZombie() {
        // Crear un zombie en el centro del mapa para mayor visibilidad
        val debugPosition = Pair(20, 20)

        // Actualizar la entidad en el PlayerManager directamente
        mapView.playerManager.updateSpecialEntity("zombie", debugPosition, MapMatrixProvider.Companion.MAP_CAFETERIA)

        // Forzar el redibujado
        mapView.invalidate()

        // Mostrar mensaje informativo
        Toast.makeText(this, "DEBUG: Zombie forzado en (20, 20)", Toast.LENGTH_LONG).show()

        // Verificar cuántas entidades especiales hay registradas
        val count = mapView.playerManager.getSpecialEntitiesCount()

        // Loggear información detallada
        Log.d(TAG, "DEBUG: Entidades especiales registradas: $count")
        mapView.playerManager.logSpecialEntities()

        // Si hay un ZombieController inicializado, también actualizar ahí
        if (this::cafeteriaZombieController.isInitialized) {
            // Cambiar esta línea:
            // zombieController.setZombiePosition(debugPosition)
            // Por esta:
            cafeteriaZombieController.setZombiePosition("zombie", debugPosition)
        }
    }

    private fun forceZombieGameWithFixedZombie() {
        // 1. Detener cualquier juego activo
        if (zombieGameActive) {
            cafeteriaZombieController.stopGame()
        }

        // 2. Establecer el estado del juego
        zombieGameActive = true
        playerScore = 0
        gameStartTime = System.currentTimeMillis()

        // 3. Actualizar UI
        runOnUiThread {
            tvBluetoothStatus.text = "MINIJUEGO ACTIVO (FORZADO) - Zombie en posición fija"
            btnB1.text = "STOP"
        }

        // 4. Inicializar el zombie en una posición fija pero visible
        val zombiePosition = Pair(20, 20)

        // 5. Establecer la posición del zombie en el controlador
        // Cambiar esta línea:
        // zombieController.setZombiePosition(zombiePosition)
        // Por esta:
        cafeteriaZombieController.setZombiePosition("zombie", zombiePosition)

        // 6. Actualizar la entidad especial en el mapa (esto debe llamar a updateSpecialEntity del PlayerManager)
        mapView.playerManager.updateSpecialEntity("zombie", zombiePosition, MapMatrixProvider.Companion.MAP_CAFETERIA)

        // 7. Forzar un redibujado inmediato
        mapView.invalidate()

        // 8. Mostrar información de depuración
        val debugMessage = "Juego zombie forzado con zombie en $zombiePosition"
        Toast.makeText(this, debugMessage, Toast.LENGTH_LONG).show()
        Log.d(TAG, debugMessage)

        // 9. Programar fin del juego (si no es atrapado) - igual que en startZombieGame
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
                                val position = Pair(
                                    playerData.getInt("x"),
                                    playerData.getInt("y")
                                )

                                // Obtener el mapa y normalizarlo
                                val mapStr = playerData.optString("map", playerData.optString("currentMap", "main"))
                                val normalizedMap = MapMatrixProvider.Companion.normalizeMapName(mapStr)

                                // Guardar en el estado del juego
                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to BuildingNumber2.GameState.PlayerInfo(position, normalizedMap))

                                // Obtener el mapa actual normalizado para comparar
                                val currentMap = MapMatrixProvider.Companion.normalizeMapName(
                                    MapMatrixProvider.Companion.MAP_CAFETERIA)

                                // Solo mostrar jugadores en el mismo mapa
                                if (normalizedMap == currentMap) {
                                    mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                                    Log.d(TAG, "Updated remote player $playerId in map $normalizedMap (original: $mapStr)")
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

                            // Obtener y normalizar el mapa
                            val mapStr = jsonObject.optString("map", jsonObject.optString("currentmap", "main"))
                            val normalizedMap = MapMatrixProvider.Companion.normalizeMapName(mapStr)

                            // Guardar en el estado
                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to BuildingNumber2.GameState.PlayerInfo(position, normalizedMap))

                            // Obtener el mapa actual normalizado para comparar
                            val currentMap = MapMatrixProvider.Companion.normalizeMapName(
                                MapMatrixProvider.Companion.MAP_CAFETERIA)

                            // IMPORTANTE: Loggear para depuración
                            Log.d(TAG, "Jugador remoto $playerId en mapa '$normalizedMap', mapa actual es '$currentMap'")

                            // Solo mostrar jugadores en el mismo mapa
                            if (normalizedMap == currentMap) {
                                mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                                Log.d(TAG, "Updated remote player $playerId in map $normalizedMap")
                            }
                        }
                    }
                    "zombie_position" -> {
                        // Manejo de la posición del zombie
                        val zombieId = jsonObject.optString("id", "zombie")
                        val x = jsonObject.getInt("x")
                        val y = jsonObject.getInt("y")
                        val zombiePosition = Pair(x, y)

                        Log.d(TAG, "¡Recibida posición de zombie $zombieId en ($x, $y)!")

                        // Actualizar la posición del zombie en el controlador
                        if (this::cafeteriaZombieController.isInitialized) {
                            cafeteriaZombieController.setZombiePosition(zombieId, zombiePosition)
                        }

                        // IMPORTANTE: Actualizar la entidad especial en el mapa
                        mapView.updateSpecialEntity(zombieId, zombiePosition, MapMatrixProvider.Companion.MAP_CAFETERIA)
                        mapView.invalidate() // Forzar redibujado
                    }

                    "zombie_game_command" -> {
                        // Procesar comandos del juego del zombie
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
                            "zombie_slowed" -> {
                                // Mostrar efecto visual de ralentización
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
                                // Notificar que los zombies vuelven a velocidad normal
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
                        // Un jugador se unió, solicitar actualización de posiciones
                        serverConnectionManager.onlineServerManager.requestPositionsUpdate()

                        // Enviar nuestra posición actual para que el nuevo jugador nos vea
                        serverConnectionManager.sendUpdateMessage(
                            playerName,
                            gameState.playerPosition,
                            MapMatrixProvider.Companion.MAP_CAFETERIA
                        )
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
    private fun normalizeMapId(mapId: String): String {
        // Casos específicos conocidos
        return when {
            mapId.contains("cafeteria") -> MapMatrixProvider.Companion.MAP_CAFETERIA
            mapId.contains("salon2009") -> MapMatrixProvider.Companion.MAP_SALON2009
            mapId.contains("salon2010") -> MapMatrixProvider.Companion.MAP_SALON2010
            mapId.contains("building2") -> MapMatrixProvider.Companion.MAP_BUILDING2
            mapId.contains("main") -> MapMatrixProvider.Companion.MAP_MAIN
            else -> mapId
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

            // Guardar estado del minijuego
            putBoolean("ZOMBIE_GAME_ACTIVE", zombieGameActive)
            putInt("PLAYER_SCORE", playerScore)
            putLong("GAME_START_TIME", gameStartTime)
        }
    }

    override fun onResume() {
        super.onResume()
        movementManager.setPosition(gameState.playerPosition)

        // Simplemente intentar reconectar si estamos en estado conectado
        if (gameState.isConnected) {
            connectToOnlineServer()
        }

        // Reenviar nuestra posición para asegurar que todos nos vean
        if (gameState.isConnected) {
            serverConnectionManager.sendUpdateMessage(
                playerName,
                gameState.playerPosition,
                MapMatrixProvider.Companion.MAP_CAFETERIA
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()

        // Asegurarse de detener el minijuego al salir
        if (zombieGameActive) {
            stopZombieGame()
        }
    }

    override fun onPause() {
        super.onPause()
        movementManager.stopMovement()
    }

    companion object {
        private const val TAG = "CafeteriaESCOM"
    }
}