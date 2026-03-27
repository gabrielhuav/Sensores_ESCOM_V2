package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.buildingIA

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
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothWebSocketBridge
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber2
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.basketball.BasketballGame

class CanchaIA : AppCompatActivity(),
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

    // Estado del juego
    private var gameState = BuildingNumber2.GameState()
    private var isGameDialogShowing = false // Bandera para evitar múltiples diálogos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_palapas_ia)

        try {
            initializeManagersEarly()
            
            playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"
            if (savedInstanceState == null) {
                gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
                gameState.isConnected = intent.getBooleanExtra("IS_CONNECTED", false)
                gameState.playerPosition = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
                    ?: Pair(20, 20)
            } else {
                restoreState(savedInstanceState)
            }

            initializeViews()

            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_cancha_ia
            )
            
            val container = findViewById<FrameLayout>(R.id.map_container)
            container.addView(mapView)

            initializeMovementManager()
            setupButtonListeners()

            mapView.post {
                mapView.setCurrentMap(MapMatrixProvider.Companion.MAP_CANCHA_IA, R.drawable.escom_cancha_ia)
                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.Companion.MAP_CANCHA_IA)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }
                
                mapView.setMapTransitionListener(this)
                updatePlayerPosition(gameState.playerPosition)
                
                if (gameState.isConnected) {
                    connectToOnlineServer()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error inicializando Cancha IA.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeManagersEarly() {
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        bluetoothManager = BluetoothManager.Companion.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@CanchaIA)
        }
        bluetoothBridge = BluetoothWebSocketBridge.Companion.getInstance()
        
        val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
            setListener(this@CanchaIA)
        }
        
        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )
    }

    private fun initializeMovementManager() {
        movementManager = MovementManager(
            mapView = mapView
        ) { position -> updatePlayerPosition(position) }
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

        tvBluetoothStatus.text = "Cancha IA - Iniciando..."
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

        btnBackToHome.setOnClickListener { returnToMainMap() }
        btnB2.setOnClickListener { returnToMainMap() }

        buttonA.setOnClickListener {
            Toast.makeText(this, "¡Estás en la Cancha de IA!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToOnlineServer() {
        updateBluetoothStatus("Conectando al servidor...")
        serverConnectionManager.connectToServer { success ->
            runOnUiThread {
                gameState.isConnected = success
                if (success) {
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        gameState.playerPosition,
                        MapMatrixProvider.Companion.MAP_CANCHA_IA
                    )
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    updateBluetoothStatus("Cancha IA - Conectado")
                } else {
                    updateBluetoothStatus("Error de conexión")
                }
            }
        }
    }

    private fun returnToMainMap() {
        val previousPosition = Pair(33, 28)

        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected)
            putExtra("INITIAL_POSITION", previousPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (::mapView.isInitialized) mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) {
            movementManager.handleMovement(event, deltaX, deltaY)
        }
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            if (!::mapView.isInitialized) return@runOnUiThread
            
            gameState.playerPosition = position
            mapView.updateLocalPlayerPosition(position)
            mapView.forceRecenterOnPlayer()

            if (gameState.isConnected) {
                serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.Companion.MAP_CANCHA_IA)
            }

            // --- DETECCIÓN DEL JUEGO DE BASKET (Área de 2x2 alrededor de la canasta) ---
            val hoopX = 13
            val hoopY = 14
            if (!isGameDialogShowing && Math.abs(position.first - hoopX) <= 2 && Math.abs(position.second - hoopY) <= 2) {
                showBasketballGame()
            }
        }
    }

    private fun showBasketballGame() {
        isGameDialogShowing = true
        val gameDialog = BasketballGame(this)
        gameDialog.setOnDismissListener {
            isGameDialogShowing = false
        }
        gameDialog.show()
    }

    private fun restoreState(savedInstanceState: Bundle) {
        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int> ?: Pair(20, 20)
        }
    }

    private fun updateBluetoothStatus(status: String) {
        runOnUiThread { tvBluetoothStatus.text = status }
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        if (targetMap == MapMatrixProvider.Companion.MAP_MAIN) {
            returnToMainMap()
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(message)
                if (jsonObject.getString("type") == "update" || jsonObject.getString("type") == "positions") {
                    // Actualizar remotos...
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error message: ${e.message}")
            }
        }
    }

    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {}
    override fun onBluetoothConnectionFailed(error: String) {}
    override fun onConnectionComplete() {}
    override fun onConnectionFailed(message: String) {}
    override fun onDeviceConnected(device: BluetoothDevice) {}
    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {}

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("IS_SERVER", gameState.isServer)
        outState.putBoolean("IS_CONNECTED", gameState.isConnected)
        outState.putSerializable("PLAYER_POSITION", gameState.playerPosition)
    }

    override fun onResume() {
        super.onResume()
        if (::movementManager.isInitialized) {
            movementManager.setPosition(gameState.playerPosition)
        }
        if (gameState.isConnected) connectToOnlineServer()
    }

    override fun onPause() {
        super.onPause()
        if (::movementManager.isInitialized) {
            movementManager.stopMovement()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothManager.isInitialized) {
            bluetoothManager.cleanup()
        }
    }

    companion object {
        private const val TAG = "Cancha IA"
    }
}
