package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.gobierno

import android.annotation.SuppressLint
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
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber2
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.penales.PenalesActivity

class Canchasgestion : AppCompatActivity(),
    BluetoothManager.BluetoothManagerCallback,
    BluetoothGameManager.ConnectionListener,
    OnlineServerManager.WebSocketListener,
    MapView.MapTransitionListener {

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
    private lateinit var btnB2: Button
    private lateinit var btnA: Button
    private lateinit var playerName: String
    private var gameState = BuildingNumber2.GameState()

    private var canChangeMap = false
    private var targetDestination: String? = null

    // --- Implementación de OnlineServerManager.WebSocketListener ---
    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(message)
                when (jsonObject.getString("type")) {
                    "positions" -> {
                        val players = jsonObject.getJSONObject("players")
                        players.keys().forEach { playerId ->
                            if (playerId != playerName) {
                                val playerData = players.getJSONObject(playerId.toString())
                                val position = Pair(playerData.getInt("x"), playerData.getInt("y"))
                                val map = playerData.optString("map", "main")
                                val normalizedMap = MapMatrixProvider.Companion.normalizeMapName(map)

                                gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                        (playerId to BuildingNumber2.GameState.PlayerInfo(position, normalizedMap))

                                if (normalizedMap == MapMatrixProvider.Companion.MAP_CANCHAS_GESTION) {
                                    mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                                }
                            }
                        }
                    }
                    "update" -> {
                        val playerId = jsonObject.getString("id")
                        if (playerId != playerName) {
                            val position = Pair(jsonObject.getInt("x"), jsonObject.getInt("y"))
                            val map = jsonObject.optString("map", "main")
                            val normalizedMap = MapMatrixProvider.Companion.normalizeMapName(map)

                            gameState.remotePlayerPositions = gameState.remotePlayerPositions +
                                    (playerId to BuildingNumber2.GameState.PlayerInfo(position, normalizedMap))

                            if (normalizedMap == MapMatrixProvider.Companion.MAP_CANCHAS_GESTION) {
                                mapView.updateRemotePlayerPosition(playerId, position, normalizedMap)
                            }
                        }
                    }
                    "join" -> {
                        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                        serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, MapMatrixProvider.Companion.MAP_CANCHAS_GESTION)
                    }
                    "disconnect" -> {
                        val disconnectedId = jsonObject.getString("id")
                        gameState.remotePlayerPositions = gameState.remotePlayerPositions - disconnectedId
                        mapView.removeRemotePlayer(disconnectedId)
                    }
                }
                mapView.invalidate()
            } catch (e: Exception) {
                Log.e("CanchasGestion", "Error procesando mensaje: ${e.message}")
            }
        }
    }

    // --- Implementación de Bluetooth Callbacks ---
    @SuppressLint("MissingPermission")
    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {
        runOnUiThread {
            val deviceName = try { device.name ?: "Dispositivo" } catch (e: SecurityException) { "Dispositivo" }
            tvBluetoothStatus.text = "BT: Conectado con $deviceName"
            Toast.makeText(this, "Conectado a $deviceName", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            val deviceName = try { device.name ?: "Desconocido" } catch (e: SecurityException) { "Desconocido" }
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.Companion.MAP_CANCHAS_GESTION)
            mapView.invalidate()
        }
    }

    override fun onBluetoothConnectionFailed(error: String) {
        runOnUiThread { tvBluetoothStatus.text = "BT: Error $error" }
    }

    override fun onConnectionComplete() {
        runOnUiThread { tvBluetoothStatus.text = "Conexión BT Establecida" }
    }

    override fun onConnectionFailed(message: String) {
        runOnUiThread { tvBluetoothStatus.text = "Error de conexión: $message" }
    }

    @SuppressLint("MissingPermission")
    override fun onDeviceConnected(device: BluetoothDevice) {
        gameState.remotePlayerName = try { device.name } catch (e: SecurityException) { "Remoto" }
    }

    // --- Ciclo de vida y Lógica Core ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canchasgestion)

        try {
            mapView = MapView(context = this, mapResourceId = R.drawable.escom_canchas_gestion)
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            initializeComponents(savedInstanceState)

            mapView.post {
                mapView.setCurrentMap(MapMatrixProvider.Companion.MAP_CANCHAS_GESTION, R.drawable.escom_canchas_gestion)
                mapView.playerManager.setCurrentMap(MapMatrixProvider.Companion.MAP_CANCHAS_GESTION)
                mapView.updateLocalPlayerPosition(gameState.playerPosition)

                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, MapMatrixProvider.Companion.MAP_CANCHAS_GESTION)
                }
            }
        } catch (e: Exception) {
            Log.e("CanchasGestion", "Error inicializando: ${e.message}")
            finish()
        }
    }

    private fun initializeComponents(savedInstanceState: Bundle?) {
        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"

        if (savedInstanceState == null) {
            gameState.isConnected = intent.getBooleanExtra("IS_CONNECTED", false)
            gameState.playerPosition = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int> ?: Pair(38, 20)
        }

        initializeViews()
        initializeManagers()
        movementManager.setPosition(gameState.playerPosition)
        setupButtonListeners()

        if (gameState.isConnected) connectToOnlineServer()

        // Iniciar conexión Bluetooth si se seleccionó un dispositivo previamente
        val selectedDevice = intent.getParcelableExtra<BluetoothDevice>("SELECTED_DEVICE")
        selectedDevice?.let { device ->
            if (bluetoothManager.isBluetoothEnabled()) {
                bluetoothManager.connectToDevice(device)
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
        btnB2 = findViewById(R.id.button_small_2)
        btnA = findViewById(R.id.button_a)
        tvBluetoothStatus.text = "Canchas Gestión - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.Companion.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@Canchasgestion)
        }
        val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
            setListener(this@Canchasgestion)
        }
        serverConnectionManager = ServerConnectionManager(this, onlineServerManager)
        mapView.playerManager.localPlayerId = playerName
        movementManager = MovementManager(mapView) { position -> updatePlayerPosition(position) }
        mapView.setMapTransitionListener(this)
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> movementManager.handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> movementManager.handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> movementManager.handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> movementManager.handleMovement(event, -1, 0); true }

        btnBackToHome.setOnClickListener {              onMapTransitionRequested(MapMatrixProvider.Companion.MAP_MAIN, Pair(8, 35)) }
        btnB2.setOnClickListener {     onMapTransitionRequested(MapMatrixProvider.Companion.MAP_MAIN, Pair(8, 35)) }

        btnA.setOnClickListener {
            if (canChangeMap) {
                when (targetDestination) {
                    "minijuego_penales" -> {
                        val intent = Intent(this, PenalesActivity::class.java).apply {
                            putExtra("PLAYER_NAME", playerName)
                        }
                        startActivity(intent)
                    }
                    MapMatrixProvider.Companion.MAP_MAIN -> {
                        onMapTransitionRequested(MapMatrixProvider.Companion.MAP_MAIN, Pair(8, 35))
                    }
                }
            }
        }
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        val x = position.first
        val y = position.second

        runOnUiThread {
            gameState.playerPosition = position
            mapView.updateLocalPlayerPosition(position)
            mapView.forceRecenterOnPlayer()

            if ((x in 18..20) && (y in 17..19)) {
                canChangeMap = true
                targetDestination = "minijuego_penales"
                Toast.makeText(this, "Presiona A para Jugar Penales", Toast.LENGTH_SHORT).show()
            } else if ((x in 34..36) && (y in 19..21)) {
                canChangeMap = true
                targetDestination = MapMatrixProvider.Companion.MAP_MAIN
                Toast.makeText(this, "Presiona A para Salir", Toast.LENGTH_SHORT).show()
            } else {
                canChangeMap = false
                targetDestination = null
            }

            if (gameState.isConnected) {
                serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.Companion.MAP_CANCHAS_GESTION)
            }
        }
    }

    private fun connectToOnlineServer() {
        serverConnectionManager.connectToServer { success ->
            runOnUiThread {
                gameState.isConnected = success
                if (success) {
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)
                    tvBluetoothStatus.text = "Canchas Gestión - En Línea"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        movementManager.setPosition(gameState.playerPosition)
        if (gameState.isConnected) connectToOnlineServer()
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        if (targetMap == MapMatrixProvider.Companion.MAP_MAIN) {
            val intent = Intent(this, GameplayActivity::class.java).apply {
                putExtra("PLAYER_NAME", playerName)
                putExtra("IS_CONNECTED", gameState.isConnected)
                putExtra("INITIAL_POSITION", initialPosition)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}
