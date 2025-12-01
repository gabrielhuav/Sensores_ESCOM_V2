package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.esia

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothWebSocketBridge
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.esia.EdificioESIA

class SalonESIA : AppCompatActivity(),
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

    private lateinit var playerName: String
    private lateinit var bluetoothBridge: BluetoothWebSocketBridge

    // Game state similar structure
    private var gameState = GameState()
    private var returnToEdificioPosition: Pair<Int, Int> = Pair(0, 0)

    data class GameState(
        var isServer: Boolean = false,
        var isConnected: Boolean = false,
        var playerPosition: Pair<Int, Int> = Pair(20, 20), // Posición inicial en el salón
        var remotePlayerPositions: Map<String, PlayerInfo> = emptyMap(),
        var remotePlayerName: String? = null
    ) {
        data class PlayerInfo(
            val position: Pair<Int, Int>,
            val map: String
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "=== SalonESIA onCreate INICIADO ===")

        try {
            setContentView(R.layout.activity_salon2009)

            // Inicializar el mapView con la imagen del salón
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_salon2009
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            // Inicializar componentes
            initializeComponents(savedInstanceState)

            // Esperar a que el mapView esté listo
            // En SalonESIA.kt, en el método onCreate(), mapView.post:

            mapView.post {
                try {
                    Log.d(TAG, "Configurando mapa SalonESIA")
                    // Configurar el mapa para el salón ESIA
                    val salonMap = MapMatrixProvider.MAP_SALON_ESIA
                    mapView.setCurrentMap(salonMap, R.drawable.escom_salon2009)

                    // Configurar el playerManager
                    mapView.playerManager.apply {
                        setCurrentMap(salonMap)
                        localPlayerId = playerName
                        updateLocalPlayerPosition(gameState.playerPosition)
                    }

                    // ✅ QUITAR ESTA LÍNEA - MovementManager no tiene setCurrentMap público
                    // movementManager.setCurrentMap(salonMap)

                    // En su lugar, solo configurar la posición
                    if (::movementManager.isInitialized) {
                        movementManager.setPosition(gameState.playerPosition)
                        Log.d(TAG, "✓ MovementManager configurado con posición: ${gameState.playerPosition}")
                    }

                    Log.d(TAG, "Mapa configurado: $salonMap")

                    // Enviar update para notificar posición
                    if (gameState.isConnected) {
                        serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, salonMap)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en mapView.post: ${e.message}", e)
                }
            }

            Log.d(TAG, "=== SalonESIA onCreate COMPLETADO ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error inicializando SalonESIA.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        playerName = intent.getStringExtra("PLAYER_NAME") ?: run {
            Toast.makeText(this, "Nombre de jugador no encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Obtener posición de retorno al edificio
        val returnX = intent.getIntExtra("RETURN_X", 0)
        val returnY = intent.getIntExtra("RETURN_Y", 0)
        returnToEdificioPosition = Pair(returnX, returnY)



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
                        MapMatrixProvider.MAP_SALON_ESIA
                    )
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    updateBluetoothStatus("Conectado al servidor online - SalonESIA")
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
        tvBluetoothStatus.text = "SalonESIA - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@SalonESIA)
        }

        bluetoothBridge = BluetoothWebSocketBridge.getInstance()

        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@SalonESIA)
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
        btnBackToHome.setOnClickListener { returnToEdificio() }
        btnBackToHome.setOnClickListener {
            Log.d(TAG, "SalonESIA: btnBackToHome click - retornando a EdificioESIA")
            returnToEdificio()
        }
    }

    private fun returnToEdificio() {
        Log.d(TAG, "Regresando al EdificioESIA")

        val intent = Intent(this, EdificioESIA::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected)
            putExtra("RETURN_FROM_SALON", true)
            putExtra("SALON_RETURN_X", returnToEdificioPosition.first)
            putExtra("SALON_RETURN_Y", returnToEdificioPosition.second)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }
        startActivity(intent)
        finish()
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) {
            movementManager.handleMovement(event, deltaX, deltaY)
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
                    as? HashMap<String, GameState.PlayerInfo>)?.toMap() ?: emptyMap()
            remotePlayerName = savedInstanceState.getString("REMOTE_PLAYER_NAME")
        }

        returnToEdificioPosition = savedInstanceState.getSerializable("RETURN_TO_EDIFICIO_POSITION") as? Pair<Int, Int>
            ?: Pair(0, 0)

        if (gameState.isConnected) {
            connectToOnlineServer()
        }
    }


    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                // 1. Actualizar estado del juego
                gameState.playerPosition = position
                Log.d("SalonESIA", "Jugador movido a: $position") // LOG DE DEPURACIÓN

                // 2. Actualizar el mapa visual
                if (::mapView.isInitialized) {
                    mapView.updateLocalPlayerPosition(position, forceCenter = true)
                }

                // 3. Enviar posición al servidor si está conectado
                if (gameState.isConnected && ::serverConnectionManager.isInitialized) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.MAP_SALON_ESIA)
                }

                // 4. VERIFICAR SI DEBE SALIR DEL SALÓN
                // Llamamos a la función de chequeo aquí mismo
                checkPositionForMapChange(position)

            } catch (e: Exception) {
                Log.e(TAG, "Error en updatePlayerPosition: ${e.message}", e)
            }
        }
    }

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        // Coordenada de la puerta (ajusta si es necesario)
        val exitPosition = Pair(0, 8)

        if (position == exitPosition) {
            Log.d("SalonESIA", "¡PUERTA DETECTADA en $position! Iniciando retorno a Edificio...")
            Toast.makeText(this, "Saliendo del salón...", Toast.LENGTH_SHORT).show()
            returnToEdificio()
        }
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        if (targetMap == MapMatrixProvider.MAP_EDIFICIO_ESIA) {
            Log.d(TAG, "SalonESIA: onMapTransitionRequested -> EdificioESIA")
            returnToEdificio()

        }
    }

    // Implementaciones de callbacks Bluetooth/WebSocket
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
            if (::mapView.isInitialized) {
                mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.MAP_SALON_ESIA)
                mapView.invalidate()
            }
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(message)
                val currentMap = MapMatrixProvider.MAP_SALON_ESIA

                when (jsonObject.getString("type")) {
                    "positions" -> {
                        val players = jsonObject.getJSONObject("players")
                        players.keys().forEach { playerId ->
                            if (playerId != playerName && ::mapView.isInitialized) {
                                val playerData = players.getJSONObject(playerId)
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
                    "join" -> {
                        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                        serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, currentMap)
                    }
                    "disconnect" -> {
                        val disconnectedId = jsonObject.getString("id")
                        if (disconnectedId != playerName && ::mapView.isInitialized) {
                            mapView.removeRemotePlayer(disconnectedId)
                        }
                    }
                }
                if (::mapView.isInitialized) {
                    mapView.invalidate()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}", e)
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
            putSerializable("RETURN_TO_EDIFICIO_POSITION", returnToEdificioPosition)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::movementManager.isInitialized) {
            // ✅ QUITAR ESTA LÍNEA
            // movementManager.setCurrentMap(MapMatrixProvider.MAP_SALON_ESIA)

            // Solo configurar la posición
            movementManager.setPosition(gameState.playerPosition)
        }
        if (gameState.isConnected) {
            connectToOnlineServer()
        }
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

    companion object {
        private const val TAG = "SalonESIA"
    }
}