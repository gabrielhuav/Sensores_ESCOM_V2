package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.buildingIA.classroom

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingEdificioIA_Medio
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity

class Salon4102 : AppCompatActivity(),
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

    private lateinit var playerName: String

    // Estado del juego
    private var gameState = GameplayActivity.GameState()
    
    // Control de asientos
    private var isSeated = false
    private var currentDeskPosition: Pair<Int, Int>? = null
    private var attendanceRegistered = false

    // Lista de posiciones de asientos y su estado (ocupado/libre)
    private val deskPositions = mutableListOf<Pair<Int, Int>>()
    private val occupiedDesks = mutableSetOf<Pair<Int, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salon4102)

        try {
            // Inicializar el mapView con el drawable del salón 4102
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_salon4102
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            // Inicializar componentes
            initializeComponents(savedInstanceState)

            // Inicializar las posiciones de los pupitres
            initializeDeskPositions()

            // Esperar a que el mapView esté listo
            mapView.post {
                mapView.setCurrentMap(MapMatrixProvider.MAP_SALON4102, R.drawable.escom_salon4102)

                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.MAP_SALON4102)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }

                // Configurar el callback para dibujar los indicadores de asientos
                setupDeskIndicators()

                Log.d(TAG, "Set map to: ${MapMatrixProvider.MAP_SALON4102}")

                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(
                        playerName, 
                        gameState.playerPosition, 
                        MapMatrixProvider.MAP_SALON4102
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error inicializando el salón.", Toast.LENGTH_LONG).show()
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
            @Suppress("UNCHECKED_CAST")
            gameState.playerPosition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("INITIAL_POSITION", Pair::class.java) as? Pair<Int, Int>
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
            } ?: Pair(20, 35) // Posición inicial en la entrada del salón
        } else {
            restoreState(savedInstanceState)
        }

        initializeViews()
        initializeManagers()
        setupButtonListeners()

        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(gameState.playerPosition)

        connectToOnlineServer()
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
                        MapMatrixProvider.MAP_SALON4102
                    )
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    
                    // Solicitar asientos ocupados
                    requestOccupiedSeats()
                    
                    updateBluetoothStatus("Conectado - Salón 4102")
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
        updateBluetoothStatus("Salón 4102 - Conectando...")
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@Salon4102)
        }

        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@Salon4102)
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
        btnNorth.setOnTouchListener { view, event ->
            handleMovement(event, 0, -1)
            if (event.action == MotionEvent.ACTION_DOWN) view.performClick()
            true
        }
        btnSouth.setOnTouchListener { view, event ->
            handleMovement(event, 0, 1)
            if (event.action == MotionEvent.ACTION_DOWN) view.performClick()
            true
        }
        btnEast.setOnTouchListener { view, event ->
            handleMovement(event, 1, 0)
            if (event.action == MotionEvent.ACTION_DOWN) view.performClick()
            true
        }
        btnWest.setOnTouchListener { view, event ->
            handleMovement(event, -1, 0)
            if (event.action == MotionEvent.ACTION_DOWN) view.performClick()
            true
        }
        btnBackToHome.setOnClickListener { returnToEdificioIAMedio() }
        
        buttonA.setOnClickListener {
            handleButtonA()
        }
    }

    private fun handleButtonA() {
        val currentPos = gameState.playerPosition
        
        if (!isSeated && checkIfOnDesk(currentPos)) {
            // Intentar sentarse en el pupitre
            sitOnDesk(currentPos)
        } else if (isSeated) {
            // Levantarse del pupitre
            standFromDesk()
        }
    }

    /**
     * Inicializa las posiciones de todos los pupitres del salón
     */
    private fun initializeDeskPositions() {
        deskPositions.clear()
        val numRows = 5
        val numCols = 8
        val rowSpacing = 5
        val colSpacing = 4
        val startY = 15
        val startX = 4

        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val deskY = startY + row * rowSpacing
                val deskX = startX + col * colSpacing
                deskPositions.add(Pair(deskX, deskY))
            }
        }

        Log.d(TAG, "Inicializadas ${deskPositions.size} posiciones de pupitres")
    }

    /**
     * Configura los indicadores visuales de los asientos en el mapa
     */
    private fun setupDeskIndicators() {
        mapView.setCustomDrawCallback(object : MapView.CustomDrawCallback {
            override fun onCustomDraw(canvas: android.graphics.Canvas, cellWidth: Float, cellHeight: Float) {
                val paint = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.FILL
                    alpha = 180 // Semi-transparente
                }

                // Dibujar cada pupitre
                for (deskPos in deskPositions) {
                    val x = deskPos.first * cellWidth
                    val y = deskPos.second * cellHeight

                    // Color según el estado: rojo si está ocupado, verde si está libre
                    paint.color = if (occupiedDesks.contains(deskPos)) {
                        android.graphics.Color.RED
                    } else {
                        android.graphics.Color.GREEN
                    }

                    // Dibujar un pequeño cuadrado en la posición del pupitre
                    val indicatorSize = cellWidth * 0.6f
                    val offsetX = (cellWidth - indicatorSize) / 2
                    val offsetY = (cellHeight - indicatorSize) / 2

                    canvas.drawRect(
                        x + offsetX,
                        y + offsetY,
                        x + offsetX + indicatorSize,
                        y + offsetY + indicatorSize,
                        paint
                    )

                    // Agregar borde para mejor visibilidad
                    val borderPaint = android.graphics.Paint().apply {
                        style = android.graphics.Paint.Style.STROKE
                        color = android.graphics.Color.BLACK
                        strokeWidth = 2f
                    }
                    canvas.drawRect(
                        x + offsetX,
                        y + offsetY,
                        x + offsetX + indicatorSize,
                        y + offsetY + indicatorSize,
                        borderPaint
                    )
                }
            }
        })
    }

    /**
     * Verifica si la posición actual es un pupitre
     * Basado en createSalonMatrix() del servidor
     */
    private fun checkIfOnDesk(position: Pair<Int, Int>): Boolean {
        val numRows = 5
        val numCols = 8
        val rowSpacing = 5
        val colSpacing = 4
        val startY = 15
        val startX = 4
        
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val deskY = startY + row * rowSpacing
                val deskX = startX + col * colSpacing
                
                if (position.first == deskX && position.second == deskY) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Sentarse en un pupitre
     */
    private fun sitOnDesk(position: Pair<Int, Int>) {
        if (!gameState.isConnected) {
            showToast("Debes estar conectado al servidor para sentarte")
            return
        }
        
        val message = JSONObject().apply {
            put("type", "sit")
            put("id", getAndroidDeviceId())
            put("playerName", playerName)
            put("map", MapMatrixProvider.MAP_SALON4102)
            put("x", position.first)
            put("y", position.second)
        }
        
        serverConnectionManager.onlineServerManager.send(message.toString())
        Log.d(TAG, "Intentando sentarse en pupitre (${position.first}, ${position.second})")
    }

    /**
     * Levantarse del pupitre
     */
    private fun standFromDesk() {
        if (!gameState.isConnected) {
            showToast("Debes estar conectado al servidor")
            return
        }
        
        val message = JSONObject().apply {
            put("type", "stand")
            put("id", getAndroidDeviceId())
            put("map", MapMatrixProvider.MAP_SALON4102)
        }
        
        serverConnectionManager.onlineServerManager.send(message.toString())
        Log.d(TAG, "Intentando levantarse del pupitre")
    }

    /**
     * Solicitar lista de asientos ocupados
     */
    private fun requestOccupiedSeats() {
        val message = JSONObject().apply {
            put("type", "get_occupied_seats")
            put("map", MapMatrixProvider.MAP_SALON4102)
        }
        
        serverConnectionManager.onlineServerManager.send(message.toString())
    }

    /**
     * Registrar asistencia al sentarse
     */
    private fun registerAttendanceOnSit() {
        if (attendanceRegistered) {
            return
        }
        
        val deviceId = getAndroidDeviceId()
        val group = "7CV2"
        
        Log.d(TAG, "Registrando asistencia al sentarse - ID: $deviceId, Nombre: $playerName, Grupo: $group")
        
        runOnUiThread {
            Toast.makeText(
                this,
                "📝 Registrando tu asistencia...",
                Toast.LENGTH_SHORT
            ).show()
        }
        
        serverConnectionManager.registerAttendance(
            phoneID = deviceId,
            fullName = playerName,
            group = group
        ) { success, message ->
            runOnUiThread {
                if (success) {
                    attendanceRegistered = true
                    Toast.makeText(
                        this,
                        "✅ $message",
                        Toast.LENGTH_LONG
                    ).show()
                    updateBluetoothStatus("Asistencia registrada - Salón 4102")
                    Log.d(TAG, "Asistencia registrada exitosamente")
                } else {
                    Toast.makeText(
                        this,
                        "⚠️ $message",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.w(TAG, "No se pudo registrar asistencia: $message")
                    
                    if (message.contains("Ya registraste")) {
                        attendanceRegistered = true
                    }
                }
            }
        }
    }

    @Suppress("HardwareIds")
    private fun getAndroidDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun returnToEdificioIAMedio() {
        // Liberar asiento si estaba sentado
        if (isSeated) {
            standFromDesk()
        }

        @Suppress("UNCHECKED_CAST")
        val previousPosition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("PREVIOUS_POSITION", Pair::class.java) as? Pair<Int, Int>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
        } ?: Pair(11, 5)

        // Obtener el mapa anterior para saber a dónde volver (similar a Salon2001)
        val previousMap = intent.getStringExtra("PREVIOUS_MAP") ?: MapMatrixProvider.MAP_EDIFICIO_IA_MEDIO

        // Por ahora solo soportamos volver a BuildingEdificioIA_Medio, pero en el futuro
        // se podría expandir para soportar otros orígenes
        val targetActivity = when (previousMap) {
            MapMatrixProvider.MAP_EDIFICIO_IA_MEDIO -> BuildingEdificioIA_Medio::class.java
            // Agregar aquí otros mapas cuando sea necesario
            else -> BuildingEdificioIA_Medio::class.java
        }

        val intent = Intent(this, targetActivity).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected) // Importante: preservar el estado de conexión
            putExtra("INITIAL_POSITION", previousPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }


        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        // No permitir movimiento si el jugador está sentado
        if (isSeated) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                showToast("Presiona A para levantarte primero")
            }
            return
        }
        movementManager.handleMovement(event, deltaX, deltaY)
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            gameState.playerPosition = position
            mapView.updateLocalPlayerPosition(position, forceCenter = true)

            if (gameState.isConnected) {
                serverConnectionManager.sendUpdateMessage(
                    playerName, 
                    position, 
                    MapMatrixProvider.MAP_SALON4102
                )
            }

            // Mostrar mensaje si está sobre un pupitre y no está sentado
            if (checkIfOnDesk(position) && !isSeated) {
                showToast("Presiona A para sentarte")
            }
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            @Suppress("UNCHECKED_CAST")
            playerPosition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable("PLAYER_POSITION", Pair::class.java) as? Pair<Int, Int>
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int>
            } ?: Pair(20, 35)
            @Suppress("UNCHECKED_CAST")
            remotePlayerPositions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS", HashMap::class.java)
                    as? HashMap<String, GameplayActivity.GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            } else {
                @Suppress("DEPRECATION")
                (savedInstanceState.getSerializable("REMOTE_PLAYER_POSITIONS")
                    as? HashMap<String, GameplayActivity.GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            }
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        isSeated = savedInstanceState.getBoolean("IS_SEATED", false)
        attendanceRegistered = savedInstanceState.getBoolean("ATTENDANCE_REGISTERED", false)

        if (gameState.isConnected) {
            connectToOnlineServer()
        }
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        if (targetMap == MapMatrixProvider.MAP_EDIFICIO_IA_MEDIO) {
            returnToEdificioIAMedio()
        } else {
            Log.d(TAG, "Mapa destino no reconocido: $targetMap")
        }
    }

    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED) {
            gameState.remotePlayerName = device.name
            updateBluetoothStatus("Conectado a ${device.name}")
        }
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED) {
            gameState.remotePlayerName = device.name
        }
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            val deviceName = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) {
                device.name ?: "Unknown"
            } else {
                "Unknown"
            }
            mapView.updateRemotePlayerPosition(
                deviceName, 
                Pair(x, y), 
                MapMatrixProvider.MAP_SALON4102
            )
            mapView.invalidate()
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(message)
                val currentMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_SALON4102)

                when (jsonObject.getString("type")) {
                    "sit_response" -> {
                        val success = jsonObject.getBoolean("success")
                        val msg = jsonObject.getString("message")
                        
                        if (success) {
                            isSeated = true
                            val deskPos = jsonObject.getJSONObject("deskPosition")
                            currentDeskPosition = Pair(deskPos.getInt("x"), deskPos.getInt("y"))
                            
                            // Registrar asistencia al sentarse exitosamente
                            registerAttendanceOnSit()
                        }
                        
                        showToast(msg)
                        Log.d(TAG, "Respuesta sit: $msg")
                    }
                    
                    "stand_response" -> {
                        val success = jsonObject.getBoolean("success")
                        val msg = jsonObject.getString("message")
                        
                        if (success) {
                            isSeated = false
                            currentDeskPosition = null
                        }
                        
                        showToast(msg)
                        Log.d(TAG, "Respuesta stand: $msg")
                    }
                    
                    "player_seated" -> {
                        val playerId = jsonObject.getString("playerId")
                        val x = jsonObject.getInt("x")
                        val y = jsonObject.getInt("y")
                        
                        occupiedDesks.add(Pair(x, y))
                        mapView.invalidate()

                        Log.d(TAG, "Jugador $playerId se sentó en ($x, $y)")
                    }
                    
                    "player_stood" -> {
                        val playerId = jsonObject.getString("playerId")
                        val x = jsonObject.getInt("x")
                        val y = jsonObject.getInt("y")
                        
                        occupiedDesks.remove(Pair(x, y))
                        mapView.invalidate()

                        Log.d(TAG, "Jugador $playerId se levantó de ($x, $y)")
                    }
                    
                    "occupied_seats" -> {
                        val seats = jsonObject.getJSONArray("seats")
                        Log.d(TAG, "Asientos ocupados: ${seats.length()}")
                        
                        occupiedDesks.clear()
                        for (i in 0 until seats.length()) {
                            val seat = seats.getJSONObject(i)
                            val x = seat.getInt("x")
                            val y = seat.getInt("y")
                            val seatPlayerId = seat.getString("playerId")
                            
                            occupiedDesks.add(Pair(x, y))
                            Log.d(TAG, "Asiento ocupado en ($x, $y) por $seatPlayerId")
                        }
                        mapView.invalidate()
                    }
                    
                    "positions", "update" -> {
                        val players = if (jsonObject.getString("type") == "positions") 
                            jsonObject.getJSONObject("players") else null
                        val playerIds = players?.keys()?.asSequence()?.toList() 
                            ?: listOf(jsonObject.getString("id"))

                        for (playerId in playerIds) {
                            if (playerId != playerName) {
                                val playerData = players?.getJSONObject(playerId) ?: jsonObject
                                val position = Pair(playerData.getInt("x"), playerData.getInt("y"))
                                val mapStr = playerData.optString("map", 
                                    playerData.optString("currentmap", "main"))
                                val normalizedMap = MapMatrixProvider.normalizeMapName(mapStr)

                                gameState.remotePlayerPositions = gameState.remotePlayerPositions + 
                                    (playerId to GameplayActivity.GameState.PlayerInfo(position, normalizedMap))

                                if (normalizedMap == currentMap) {
                                    mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                                }
                            }
                        }
                    }
                    
                    "join" -> {
                        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                        serverConnectionManager.sendUpdateMessage(
                            playerName, 
                            gameState.playerPosition, 
                            currentMap
                        )
                    }
                    
                    "disconnect" -> {
                        val disconnectedId = jsonObject.getString("id")
                        if (disconnectedId != playerName) {
                            gameState.remotePlayerPositions = 
                                gameState.remotePlayerPositions - disconnectedId
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
            putBoolean("IS_SEATED", isSeated)
            putBoolean("ATTENDANCE_REGISTERED", attendanceRegistered)
        }
    }

    override fun onResume() {
        super.onResume()
        movementManager.setPosition(gameState.playerPosition)
        if (gameState.isConnected) {
            connectToOnlineServer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Liberar asiento si está sentado
        if (isSeated && gameState.isConnected) {
            standFromDesk()
        }
        
        bluetoothManager.cleanup()
    }

    override fun onPause() {
        super.onPause()
        movementManager.stopMovement()
    }

    companion object {
        private const val TAG = "Salon4102"
    }
}
