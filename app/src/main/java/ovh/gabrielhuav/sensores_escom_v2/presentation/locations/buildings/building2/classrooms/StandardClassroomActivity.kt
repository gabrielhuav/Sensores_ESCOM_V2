package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.buildingNumber2.classrooms

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
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber2Piso1
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber2Piso2
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber4
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber4Piso1
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber4Piso2
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView

class StandardClassroomActivity : AppCompatActivity(),
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
    private lateinit var currentMapId: String
    private lateinit var returnMapId: String
    private lateinit var returnPosition: Pair<Int, Int>
    private var gameState = BuildingNumber2.GameState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salon2000)

        try {
            // 1. PRIMERO: Leemos los Intents básicos para saber qué mapa cargar
            readIntentData(savedInstanceState)

            // 2. SEGUNDO: Inicializamos el MapView (ahora ya sabemos qué ID de mapa usar)
            // Seleccionamos la imagen correcta según el ID del mapa
            val mapDrawable = getMapResource(currentMapId)

            mapView = MapView(
                context = this,
                mapResourceId = mapDrawable
            )

            // Agregamos el mapa al layout (Asegúrate que 'map_container' existe en activity_salon2000.xml)
            val container = findViewById<FrameLayout>(R.id.map_container)
            if (container != null) {
                container.addView(mapView)
            } else {
                throw Exception("No se encontró el contenedor map_container en el XML")
            }

            // 3. TERCERO: Inicializamos los componentes y managers (ahora mapView ya existe)
            initializeComponentsAfterMapCreated()

            // 4. CUARTO: Configuración final del mapa
            mapView.post {
                mapView.setCurrentMap(currentMapId, mapDrawable)

                mapView.playerManager.apply {
                    setCurrentMap(currentMapId)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }

                Log.d(TAG, "Mapa configurado: $currentMapId")

                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, currentMapId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
            e.printStackTrace() // Esto te ayudará a ver el error real en Logcat
            Toast.makeText(this, "Error inicializando actividad de salón: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Nueva función para leer datos antes de crear nada
    private fun readIntentData(savedInstanceState: Bundle?) {
        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Unknown"
        currentMapId = intent.getStringExtra("CURRENT_MAP_ID") ?: MapMatrixProvider.MAP_SALON2001
        returnMapId = intent.getStringExtra("RETURN_MAP_ID") ?: MapMatrixProvider.MAP_BUILDING2
        returnPosition = intent.getSerializableExtra("RETURN_POSITION") as? Pair<Int, Int>
            ?: Pair(20, 20)

        if (savedInstanceState == null) {
            gameState.isServer = intent.getBooleanExtra("IS_SERVER", false)
            gameState.isConnected = intent.getBooleanExtra("IS_CONNECTED", false)
            gameState.playerPosition = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
                ?: Pair(20, 20)
        } else {
            restoreState(savedInstanceState)
        }
    }

    // Lógica separada que requiere que mapView ya exista
    private fun initializeComponentsAfterMapCreated() {
        initializeViews()
        initializeManagers() // Aquí es donde tronaba antes
        setupButtonListeners()

        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(gameState.playerPosition)

        if(gameState.isConnected) {
            connectToOnlineServer()
        }
    }

    // Helper para elegir la imagen correcta según el salón
    private fun getMapResource(mapId: String): Int {
        return when {
            mapId.contains("2009") -> R.drawable.escom_salon2009
            mapId.contains("2010") -> R.drawable.escom_salon2009 // O su propia imagen si tienes
            // Puedes agregar un case para un drawable genérico de salón si tienes 'escom_salon_generico'
            // mapId.contains("salon") -> R.drawable.escom_salon_generico
            else -> R.drawable.escom_salon2009 // Default fallback
        }
    }

    // ... El resto de tus funciones (connectToOnlineServer, initializeViews, etc) siguen igual ...

    private fun connectToOnlineServer() {
        updateBluetoothStatus("Conectando...")
        serverConnectionManager.connectToServer { success ->
            runOnUiThread {
                gameState.isConnected = success
                if (success) {
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        gameState.playerPosition,
                        currentMapId
                    )
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    updateBluetoothStatus("Conectado: $currentMapId")
                } else {
                    updateBluetoothStatus("Error conexión")
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
        tvBluetoothStatus.text = "Salón: $currentMapId"
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@StandardClassroomActivity)
        }

        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@StandardClassroomActivity)
        }

        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        // ESTO ERA LO QUE CAUSABA EL ERROR SI MAPVIEW ERA NULL
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
        btnBackToHome.setOnClickListener { returnToBuilding() }
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        movementManager.handleMovement(event, deltaX, deltaY)
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            gameState.playerPosition = position
            mapView.updateLocalPlayerPosition(position, forceCenter = true)

            if (gameState.isConnected) {
                serverConnectionManager.sendUpdateMessage(playerName, position, currentMapId)
            }

            // Salida automática al tocar la puerta (coordenada 0,6 según createSalonMatrix)
            if (position.first == 0 && position.second == 6) {
                returnToBuilding()
            }
        }
    }

    private fun returnToBuilding() {
        val targetClass = when (returnMapId) {
            MapMatrixProvider.MAP_BUILDING2 -> BuildingNumber2::class.java
            MapMatrixProvider.MAP_BUILDING2_PISO1 -> BuildingNumber2Piso1::class.java
            MapMatrixProvider.MAP_BUILDING2_PISO2 -> BuildingNumber2Piso2::class.java

            MapMatrixProvider.MAP_BUILDING4 -> BuildingNumber4::class.java
            MapMatrixProvider.MAP_BUILDING4_PISO1 -> BuildingNumber4Piso1::class.java
            MapMatrixProvider.MAP_BUILDING4_PISO2 -> BuildingNumber4Piso2::class.java
            else -> BuildingNumber2::class.java
        }

        val intent = Intent(this, targetClass).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("IS_CONNECTED", gameState.isConnected)
            putExtra("INITIAL_POSITION", returnPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        if (targetMap == returnMapId || targetMap == MapMatrixProvider.MAP_BUILDING2) {
            returnToBuilding()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putBoolean("IS_SERVER", gameState.isServer)
            putBoolean("IS_CONNECTED", gameState.isConnected)
            putSerializable("PLAYER_POSITION", gameState.playerPosition)
            putString("CURRENT_MAP_ID", currentMapId)
            putString("RETURN_MAP_ID", returnMapId)
            putSerializable("RETURN_POSITION", returnPosition)
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        currentMapId = savedInstanceState.getString("CURRENT_MAP_ID") ?: MapMatrixProvider.MAP_SALON2001
        returnMapId = savedInstanceState.getString("RETURN_MAP_ID") ?: MapMatrixProvider.MAP_BUILDING2
        returnPosition = savedInstanceState.getSerializable("RETURN_POSITION") as? Pair<Int, Int> ?: Pair(20,20)

        gameState.apply {
            isServer = savedInstanceState.getBoolean("IS_SERVER", false)
            isConnected = savedInstanceState.getBoolean("IS_CONNECTED", false)
            playerPosition = savedInstanceState.getSerializable("PLAYER_POSITION") as? Pair<Int, Int> ?: Pair(20, 20)
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(message)
                val normalizedCurrentMap = MapMatrixProvider.normalizeMapName(currentMapId)

                when (jsonObject.getString("type")) {
                    "positions", "update" -> {
                        val players = if (jsonObject.getString("type") == "positions") jsonObject.getJSONObject("players") else null
                        val playerIds = players?.keys()?.asSequence()?.toList() ?: listOf(jsonObject.getString("id"))

                        for (playerId in playerIds) {
                            if (playerId != playerName) {
                                val playerData = players?.getJSONObject(playerId) ?: jsonObject
                                val position = Pair(playerData.getInt("x"), playerData.getInt("y"))
                                val mapStr = playerData.optString("map", playerData.optString("currentmap", "main"))
                                val normalizedRemoteMap = MapMatrixProvider.normalizeMapName(mapStr)

                                if (normalizedRemoteMap == normalizedCurrentMap) {
                                    mapView.updateRemotePlayerPosition(playerId, position, normalizedRemoteMap)
                                }
                            }
                        }
                    }
                }
                mapView.invalidate()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}")
            }
        }
    }

    // Callbacks de Bluetooth vacíos o simples por ahora
    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {
        gameState.remotePlayerName = device.name
        updateBluetoothStatus("Conectado a ${device.name}")
    }
    override fun onBluetoothConnectionFailed(error: String) {
        updateBluetoothStatus("Error: $error")
        showToast(error)
    }
    override fun onConnectionComplete() { updateBluetoothStatus("Conexión completa.") }
    override fun onConnectionFailed(message: String) { onBluetoothConnectionFailed(message) }
    override fun onDeviceConnected(device: BluetoothDevice) { gameState.remotePlayerName = device.name }
    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        runOnUiThread {
            val deviceName = device.name ?: "Unknown"
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), currentMapId)
            mapView.invalidate()
        }
    }

    private fun updateBluetoothStatus(status: String) {
        runOnUiThread { tvBluetoothStatus.text = status }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        movementManager.setPosition(gameState.playerPosition)
        if (gameState.isConnected) connectToOnlineServer()
    }
    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()
    }
    override fun onPause() {
        super.onPause()
        movementManager.stopMovement()
    }

    companion object {
        private const val TAG = "StandardClassroom"
    }
}