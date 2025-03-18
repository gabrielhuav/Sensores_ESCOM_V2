package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.buildingNumber3

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
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber4
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview.*
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.buildingNumber3.pacman.PacmanController
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.buildingNumber3.pacman.PacmanGameStatus

class SalonPacman : AppCompatActivity(),
    BluetoothManager.BluetoothManagerCallback,
    BluetoothGameManager.ConnectionListener,
    OnlineServerManager.WebSocketListener,
    MapView.MapTransitionListener {

    // UI Components
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var mapView: MapView

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

    // Reutilizamos la misma estructura de GameState que en BuildingNumber2
    private var gameState = BuildingNumber4.GameState()

    // Pacman controller - maneja la lógica del juego
    private lateinit var pacmanController: PacmanController
    private var pacmanGameActive = false
    private var gameScore = 0
    private var lives = 3

    private fun applyAggressiveZoom() {
        // Llamar a este método después de iniciar el juego Pacman
        mapView.zoomToFitGame()

        // También programar un zoom periódico para mantener el ajuste
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (pacmanGameActive) {
                    mapView.zoomToFitGame()
                    handler.postDelayed(this, 1000) // Verificar cada segundo
                }
            }
        }, 500) // Esperar medio segundo para la primera verificación
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salon_pacman)

        try {
            // Inicializar el mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_salon1212
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            // Inicializar componentes
            initializeComponents(savedInstanceState)

            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa para el salón 1212
                val normalizedMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_SALON1212)
                mapView.setCurrentMap(normalizedMap, R.drawable.escom_salon1212)

                // Configurar el playerManager
                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.MAP_SALON1212)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }

                // Importante: Enviar un update inmediato para que otros jugadores sepan dónde estamos
                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, MapMatrixProvider.MAP_SALON1212)
                }

                // Mostrar diálogo de introducción al juego Pacman
                showPacmanGameIntroDialog()
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
            // Inicializar desde el Intent
            gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
            gameState.isConnected = intent.getBooleanExtra("IS_CONNECTED", false)
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

        // Ajustar el tamaño de las celdas para el juego de Pacman
        mapView.post {
            mapView.adjustMapToScreen()
            mapView.forceRecenterOnPlayer()

            // También puedes forzar un zoom específico para Pacman
            mapView.zoomToFitGame()
        }

        // Inicializar el controlador del Pacman
        pacmanController = PacmanController(
            onGameStateChanged = { state ->
                // Manejar cambios de estado del juego
                when (state) {
                    PacmanGameStatus.WIN -> onGameCompleted(true)
                    PacmanGameStatus.LOSE -> onGameCompleted(false)
                    PacmanGameStatus.UPDATE -> updatePacmanGameUI()
                    PacmanGameStatus.PLAYING -> {
                        runOnUiThread {
                            tvBluetoothStatus.text = "Jugando Pac-Man - ¡Buena suerte!"
                        }
                    }
                    PacmanGameStatus.PAUSED -> {
                        runOnUiThread {
                            tvBluetoothStatus.text = "Juego en pausa"
                        }
                    }
                    else -> {
                        runOnUiThread {
                            tvBluetoothStatus.text = "MINIJUEGO ACTIVO - Pac-Man"
                        }
                    }
                }
            },
            onEntityPositionChanged = { entityId, position, entityType ->
                // Skip processing if position is (-1, -1) which means removal
                if (position.first == -1 && position.second == -1) {
                    runOnUiThread {
                        // Remove entity (like eaten food)
                        mapView.removeSpecialEntity(entityId)
                        mapView.invalidate()
                    }
                } else {
                    // Regular entity update
                    runOnUiThread {
                        mapView.updateSpecialEntity(entityId, position, MapMatrixProvider.MAP_SALON1212)
                        mapView.invalidate()
                    }
                }
            }
        )

        // Asegurarnos de que nos reconectamos al servidor online
        connectToOnlineServer()
    }

    private fun showPacmanGameIntroDialog() {
        AlertDialog.Builder(this)
            .setTitle("¡Bienvenido al Salón 1212!")
            .setMessage("¡Desafío Pac-Man en el Salón 1212!\n\n" +
                    "Come todos los puntos mientras evitas a los fantasmas. " +
                    "Usa los botones direccionales para mover a Pac-Man.\n\n" +
                    "Presiona 'B1' para iniciar el juego.")
            .setPositiveButton("¡Entendido!") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun startPacmanGame() {
        pacmanGameActive = true
        gameScore = 0
        lives = 3

        // Actualizar UI
        runOnUiThread {
            tvBluetoothStatus.text = "MINIJUEGO ACTIVO - Pac-Man"
            btnB1.text = "STOP"
        }

        // Iniciar el controlador del Pacman
        pacmanController.startGame()

        // Informar al servidor que el juego ha iniciado
        sendPacmanGameUpdate("start")

        // Aplicar un zoom agresivo al tablero (añadir esta línea)
        applyAggressiveZoom()

        Log.d(TAG, "Minijuego Pac-Man iniciado con zoom agresivo")
    }

    private fun stopPacmanGame() {
        if (pacmanGameActive) {
            pacmanController.stopGame()
            pacmanGameActive = false

            // Actualizar UI
            runOnUiThread {
                tvBluetoothStatus.text = "Conectado al servidor online - SALON 1212"
                btnB1.text = "B1"

                // Limpiar las entidades del mapa
                clearPacmanEntities()
                mapView.invalidate()
            }

            // Informar al servidor
            sendPacmanGameUpdate("stop")
        }
    }

    private fun onGameCompleted(won: Boolean) {
        pacmanGameActive = false
        pacmanController.stopGame()

        // Mostrar resultado
        runOnUiThread {
            val message = if (won) {
                "¡VICTORIA! Has completado el nivel de Pac-Man. Puntuación: $gameScore"
            } else {
                "¡GAME OVER! Los fantasmas te han atrapado. Puntuación: $gameScore"
            }

            AlertDialog.Builder(this)
                .setTitle("Fin del Minijuego")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    // Actualizar UI
                    tvBluetoothStatus.text = "Conectado al servidor online - SALON 1212"
                    btnB1.text = "B1"

                    // Limpiar el mapa
                    clearPacmanEntities()
                    mapView.invalidate()
                }
                .setCancelable(false)
                .show()
        }

        // Informar al servidor
        sendPacmanGameUpdate("complete", won, gameScore)
    }


    private fun updatePacmanGameUI() {
        runOnUiThread {
            // Update score and lives display
            gameScore = pacmanController.getScore()
            lives = pacmanController.getLives()

            // Make sure this text is very visible
            val statusText = "PAC-MAN | Puntos: $gameScore | Vidas: $lives"
            tvBluetoothStatus.text = statusText

            // Log to verify this method is being called
            Log.d(TAG, "Updating UI: $statusText")

            // Force redraw to ensure entities are drawn
            mapView.invalidate()
        }
    }

    private fun clearPacmanEntities() {
        // Eliminar todas las entidades del juego del mapa
        mapView.removeSpecialEntity("pacman")

        // Eliminar los fantasmas
        for (i in 0 until 4) {
            mapView.removeSpecialEntity("ghost_$i")
        }

        // También podríamos tener que eliminar los puntos
        for (i in 0 until 30) {
            for (j in 0 until 30) {
                mapView.removeSpecialEntity("food_${i}_${j}")
            }
        }
    }

    private fun sendPacmanGameUpdate(action: String, won: Boolean = false, score: Int = 0) {
        try {
            val message = JSONObject().apply {
                put("type", "pacman_game_update")
                put("action", action)
                put("player", playerName)
                put("map", MapMatrixProvider.MAP_SALON1212)

                if (action == "complete") {
                    put("won", won)
                    put("score", score)
                }
            }

            serverConnectionManager.onlineServerManager.queueMessage(message.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando actualización del juego: ${e.message}")
        }
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
                        MapMatrixProvider.MAP_SALON1212
                    )

                    // Solicitar actualizaciones de posición
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()

                    updateBluetoothStatus("Conectado al servidor online - SALON 1212")
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
        tvBluetoothStatus.text = "Salón 1212 - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@SalonPacman)
        }

        bluetoothBridge = BluetoothWebSocketBridge.getInstance()

        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@SalonPacman)
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
        btnNorth.setOnTouchListener { _, event ->
            if (pacmanGameActive && event.action == MotionEvent.ACTION_DOWN) {
                // Log to verify direction changes
                Log.d(TAG, "Setting Pacman direction to UP")
                pacmanController.setDirection(PacmanController.DIRECTION_UP)
                return@setOnTouchListener true
            } else {
                handleMovement(event, 0, -1)
                return@setOnTouchListener true
            }
        }

        btnSouth.setOnTouchListener { _, event ->
            if (pacmanGameActive && event.action == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "Setting Pacman direction to BOTTOM")
                pacmanController.setDirection(PacmanController.DIRECTION_BOTTOM)
                return@setOnTouchListener true
            } else {
                handleMovement(event, 0, 1)
                return@setOnTouchListener true
            }
        }

        btnEast.setOnTouchListener { _, event ->
            if (pacmanGameActive && event.action == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "Setting Pacman direction to RIGHT")
                pacmanController.setDirection(PacmanController.DIRECTION_RIGHT)
                return@setOnTouchListener true
            } else {
                handleMovement(event, 1, 0)
                return@setOnTouchListener true
            }
        }

        btnWest.setOnTouchListener { _, event ->
            if (pacmanGameActive && event.action == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "Setting Pacman direction to LEFT")
                pacmanController.setDirection(PacmanController.DIRECTION_LEFT)
                return@setOnTouchListener true
            } else {
                handleMovement(event, -1, 0)
                return@setOnTouchListener true
            }
        }

        // Botón para volver al edificio
        btnBackToHome.setOnClickListener {
            returnToBuilding()
        }

        // Botón BCK para volver
        btnB2.setOnClickListener {
            returnToBuilding()
        }

        // Botón B1 para iniciar/detener el minijuego
        btnB1.setOnClickListener {
            if (pacmanGameActive) {
                stopPacmanGame()
            } else {
                startPacmanGame()
            }
        }

        // Botón A para interactuar (no usado en este minijuego)
        buttonA.setOnClickListener {
            // Podría usarse para activar power-ups o pausar el juego
            if (pacmanGameActive) {
                // Alguna acción especial durante el juego
                Toast.makeText(this, "¡Power-up activado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnToBuilding() {
        // Si hay un juego activo, detenerlo primero
        if (pacmanGameActive) {
            stopPacmanGame()
        }

        // Obtener la posición previa
        val previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
            ?: Pair(15, 16) // Por defecto, volver al pasillo principal

        // Crear intent para volver al Edificio 2
        val intent = Intent(this, BuildingNumber4::class.java).apply {
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
                serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.MAP_SALON1212)
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
                    as? HashMap<String, BuildingNumber4.GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        // Restaurar estado del minijuego Pacman
        pacmanGameActive = savedInstanceState.getBoolean("PACMAN_GAME_ACTIVE", false)
        gameScore = savedInstanceState.getInt("PACMAN_SCORE", 0)
        lives = savedInstanceState.getInt("PACMAN_LIVES", 3)

        // Reconectar si es necesario
        if (gameState.isConnected) {
            connectToOnlineServer()
        }

        // Restaurar el minijuego si estaba activo
        if (pacmanGameActive) {
            pacmanController.startGame(gameScore, lives)
        }
    }

    // Implementación MapTransitionListener
    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.MAP_BUILDING2 -> {
                returnToBuilding()
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
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.MAP_SALON1212)
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

                                // Obtener el mapa y normalizarlo
                                val mapStr = playerData.optString("map", playerData.optString("currentMap", "main"))
                                val normalizedMap = MapMatrixProvider.normalizeMapName(mapStr)

                                // Guardar en el estado del juego
                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to BuildingNumber4.GameState.PlayerInfo(
                                            position,
                                            normalizedMap
                                        ))

                                // Obtener el mapa actual normalizado para comparar
                                val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_SALON1212)

                                // Solo mostrar jugadores en el mismo mapa
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

                            // Obtener y normalizar el mapa
                            val mapStr = jsonObject.optString("map", jsonObject.optString("currentmap", "main"))
                            val normalizedMap = MapMatrixProvider.normalizeMapName(mapStr)

                            // Guardar en el estado
                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to BuildingNumber4.GameState.PlayerInfo(
                                        position,
                                        normalizedMap
                                    ))

                            // Obtener el mapa actual normalizado para comparar
                            val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_SALON1212)

                            // Solo mostrar jugadores en el mismo mapa
                            if (normalizedMap == currentMap) {
                                mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                            }
                        }
                    }
                    "pacman_game_command" -> {
                        // Procesar comandos del juego de Pacman
                        when (jsonObject.optString("command")) {
                            "start" -> {
                                if (!pacmanGameActive) {
                                    startPacmanGame()
                                }
                            }
                            "stop" -> {
                                if (pacmanGameActive) {
                                    stopPacmanGame()
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
                            MapMatrixProvider.MAP_SALON1212
                        )
                    }
                    "disconnect" -> {
                        // Manejar desconexión de jugador
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
            putBoolean("PACMAN_GAME_ACTIVE", pacmanGameActive)
            putInt("PACMAN_SCORE", gameScore)
            putInt("PACMAN_LIVES", lives)
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
                MapMatrixProvider.MAP_SALON1212
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()

        // Asegurarse de detener el minijuego al salir
        if (pacmanGameActive) {
            stopPacmanGame()
        }
    }

    override fun onPause() {
        super.onPause()
        movementManager.stopMovement()
    }

    companion object {
        private const val TAG = "SalonPacman"
    }
}