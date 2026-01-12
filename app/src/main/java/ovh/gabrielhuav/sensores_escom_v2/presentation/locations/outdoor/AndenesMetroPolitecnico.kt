package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber2
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.Zacatenco
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider.Companion.INTERACTIVE
import kotlin.math.min

class AndenesMetroPolitecnico : AppCompatActivity(),
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
    private lateinit var buttonA: Button
    private lateinit var btnBackToHome: Button
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var playerName: String
    private var isShowingCollisionDialog = false

    private var gameState = BuildingNumber2.GameState()
    // Metro animation properties
    private val metroList = mutableListOf<Metro>()
    private val metroColor = Color.argb(255, 255, 140, 0) // Naranja característico del Metro CDMX
    private val handler = Handler(Looper.getMainLooper())
    private var isAnimationRunning = false
    private val metroUpdateRunnable = object : Runnable {
        override fun run() {
            if (isAnimationRunning) {
                updateMetros()
                handler.postDelayed(this, 16) // ~60fps
            }
        }
    }

    // Metro class to represent each metro train
    inner class Metro(
        var x: Float,
        var y: Float,
        val width: Float = 850f,
        val height: Float = 35f,
        val speed: Float,
        val direction: Int, // -1 => se mueve a la izquierda (start = derecha); 1 => se mueve a la derecha (start = izquierda)
        val color: Int = metroColor
    ) {
        // Guardar Y inicial
        private val initialY: Float = y
        val rect = RectF(x, y, x + width, y + height)

        fun update() {
            // No actualizar si no hay mapa
            val mapWidth = mapView.mapState.backgroundBitmap?.width?.toFloat() ?: return

            // Movimiento
            x += speed * direction

            // Condición off-screen y reinicio:
            // Si se mueve a la izquierda (direction == -1) y su borde derecho < 0 -> reset a la derecha (mapWidth)
            // Si se mueve a la derecha (direction == 1) y su borde izquierdo > mapWidth -> reset a la izquierda (-width)
            val isOffScreen = when (direction) {
                -1 -> (x + width) < 0f
                1 -> x > mapWidth
                else -> false
            }

            if (isOffScreen) {
                resetPosition(mapWidth)
            }

            // Actualizar rect
            rect.left = x
            rect.right = x + width
            rect.top = y
            rect.bottom = y + height
        }

        private fun resetPosition(mapWidth: Float) {
            when (direction) {
                -1 -> {
                    // se mueve a la izquierda → reiniciar al extremo derecho
                    x = mapWidth
                    y = initialY
                }
                1 -> {
                    // se mueve a la derecha → reiniciar al extremo izquierdo (fuera de pantalla)
                    x = -width
                    y = initialY
                }
                else -> {
                    x = -width
                    y = initialY
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_andenes_metro_politecnico)
        try {
            // Inicializar el mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.andenes_metro // Usa la imagen de la salida
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)
            // Inicializar componentes
            initializeComponents(savedInstanceState)
            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa para los andenes del metro
                mapView.setCurrentMap(MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO, R.drawable.andenes_metro)
                // Configurar el playerManager
                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }
                Log.d(TAG, "Set map to: " + MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO)
                // Importante: Enviar un update inmediato para que otros jugadores sepan dónde estamos
                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO)
                }
                // Initialize metros after mapView is created
                initializeMetros()
                startMetroAnimation()
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
        // Asegurarnos de que nos reconectamos al servidor online
        connectToOnlineServer()
    }

    private fun initializeMetros() {
        // Wait until map is fully loaded
        mapView.post {
            val mapBitmap = mapView.mapState.backgroundBitmap ?: return@post
            val mapWidth = mapBitmap.width.toFloat()
            val mapHeight = mapBitmap.height.toFloat()

            // Clear any existing metros
            metroList.clear()

            // Create metros in both directions
            // Metro moving left (direction = -1). Start at right edge.
            metroList.add(
                Metro(
                    x = mapWidth,
                    y = mapHeight * 0.38f,
                    speed = 6f,
                    direction = -1,
                    width = 380f,
                    height = 36f
                )
            )

            // Metro moving right (direction = 1). Start off-screen left.
            metroList.add(
                Metro(
                    x = -850f,
                    y = mapHeight * 0.48f,
                    speed = 5f,
                    direction = 1,
                    width = 360f,
                    height = 34f
                )
            )

            mapView.setCarRenderer(object : MapView.CarRenderer {
                override fun drawCars(canvas: Canvas) {
                    val paint = Paint().apply {
                        style = Paint.Style.FILL
                    }
                    for (metro in metroList) {
                        paint.color = metro.color

                        canvas.drawRect(metro.rect, paint)

                        // Añadir ventanas
                        paint.color = Color.CYAN
                        val windowCount = 6
                        val windowWidth = metro.width / (windowCount + 2)
                        val windowSpacing = windowWidth * 0.31f
                        var wx = metro.x + windowSpacing

                        for (i in 0 until windowCount) {
                            val windowRect = RectF(
                                wx,
                                metro.y + 5f,
                                wx + windowWidth,
                                metro.y + metro.height - 5f
                            )
                            canvas.drawRect(windowRect, paint)
                            wx += windowWidth + windowSpacing
                        }

                        // (Línea 5)
                        paint.color = Color.BLACK
                        paint.textSize = 20f
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(
                            "Línea 5",
                            metro.x + metro.width / 2,
                            metro.y + metro.height / 2 + 7f,
                            paint
                        )
                    }
                }
            })

            Log.d(TAG, "Metros initialized. Map dimensions: $mapWidth x $mapHeight")
        }
    }

    private fun startMetroAnimation() {
        if (!isAnimationRunning) {
            isAnimationRunning = true
            handler.post(metroUpdateRunnable)
            Log.d(TAG, "Metro iniciado")
        }
    }

    private fun stopMetroAnimation() {
        isAnimationRunning = false
        handler.removeCallbacks(metroUpdateRunnable)
        Log.d(TAG, "Metro stop")
    }
    private fun updateMetros() {
        // Update metro positions
        for (metro in metroList) {
            try {
                metro.update()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating metro: ${e.message}")
            }
        }
        mapView.invalidate()
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
                        MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO
                    )
                    // Solicitar actualizaciones de posición
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    updateBluetoothStatus("Conectado al servidor online - SALIDA METRO")
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
        buttonA = findViewById(R.id.button_a)
        btnBackToHome = findViewById(R.id.button_back_to_home)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        tvBluetoothStatus.text = "Andenes Metro - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.Companion.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@AndenesMetroPolitecnico)
        }
        val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
            setListener(this@AndenesMetroPolitecnico)
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
            returnToMetroPolitecnicoActivity()
        }

        // Configurar el botón BCK si existe
        findViewById<Button?>(R.id.button_small_2)?.setOnClickListener {
            returnToMetroPolitecnicoActivity()
        }

        // Configurar el botón A para interactuar con puntos de interés
        findViewById<Button?>(R.id.button_a)?.setOnClickListener {
            handleButtonAPress()
        }
    }

    // Método para manejar la pulsación del botón A
    private fun handleButtonAPress() {
        val position = gameState.playerPosition
        when {
            canChangeMap && targetDestination == "lineas" -> {
                startLineasMetroActivity()
            }

        }
    }
    private var canChangeMap = false  // Variable para controlar si se puede cambiar de mapa
    private var targetDestination: String? = null  // Variable para almacenar el destino

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        // Comprobar múltiples ubicaciones de transición
        when {
            position.first == 20 && position.second == 10 -> {
                canChangeMap =  true
                targetDestination = "lineas"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al metro", Toast.LENGTH_SHORT).show()
                }
            }
            position.first == 20 && position.second == 26 -> {
                canChangeMap =  true
                targetDestination = "lineas"
                runOnUiThread {
                    Toast.makeText(this, "Presiona A para entrar al metro", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun returnToMetroPolitecnicoActivity() {
        val intent = Intent(this, MetroPolitecnico::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(18, 20))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition) // Guarda la posición actual
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun startLineasMetroActivity() {
        val intent = Intent(this, RedMetro::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(9, 14))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
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
            // Verificar si estamos en un punto de interés
            checkPositionForMapChange(position)
            // Enviar actualización a otros jugadores con el mapa específico
            if (gameState.isConnected) {
                // Enviar la posición con el nombre del mapa correcto
                serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO)
                // Log de debug para confirmar
                Log.d(TAG, "Sending update: Player $playerName at $position in map ${MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO}")
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

        // Reconectar si es necesario
        if (gameState.isConnected) {
            connectToOnlineServer()
        }
    }

    // Implementación MapTransitionListener
    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.Companion.MAP_MAIN -> {
                returnToMetroPolitecnicoActivity()
            }
            else -> {
                Log.d(TAG, "Mapa destino no reconocido: $targetMap")
            }
        }
    }

    // Callbacks de Bluetooth
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDeviceConnected(device: BluetoothDevice) {
        gameState.remotePlayerName = device.name
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            val deviceName = device.name ?: "Unknown"
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO)
            mapView.invalidate()
        }
    }

    // Implementación WebSocketListener
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
                                val map = playerData.getString("map")
                                // Actualizar la posición del jugador en el mapa
                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to BuildingNumber2.GameState.PlayerInfo(
                                            position,
                                            map
                                        ))
                                // Solo mostrar jugadores que estén en el mismo mapa
                                if (map == MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO) {
                                    mapView.updateRemotePlayerPosition(playerId, position, map)
                                    Log.d(TAG, "Updated remote player $playerId position to $position in map $map")
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
                            val map = jsonObject.getString("map")
                            // Actualizar el estado del jugador
                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to BuildingNumber2.GameState.PlayerInfo(
                                        position,
                                        map
                                    ))
                            // Solo mostrar jugadores que estén en el mismo mapa
                            if (map == MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO) {
                                mapView.updateRemotePlayerPosition(playerId, position, map)
                                Log.d(TAG, "Updated remote player $playerId position to $position in map $map")
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
                            MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO
                        )
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
                MapMatrixProvider.Companion.MAP_ANDENES_METRO_POLITECNICO
            )
        }
        // REINICIAR la animación cada vez que se resume la actividad
        startMetroAnimation()
    }

    override fun onPause() {
        super.onPause()
        // Stop metro animation when activity is paused
        stopMetroAnimation()
        movementManager.stopMovement()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to remove callbacks to prevent memory leaks
        stopMetroAnimation()
        try {
            bluetoothManager.cleanup()
        } catch (e: Exception) {
            Log.w(TAG, "Bluetooth cleanup error: ${e.message}")
        }
    }
    companion object {
        private const val TAG = "AndenesMetroPolitecnico"
    }
}
