package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.Manifest
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ovh.gabrielhuav.sensores_escom_v2.R
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
import org.json.JSONObject
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
import kotlin.math.min
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.Gravity
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider.Companion.INTERACTIVE


class RedMetro : AppCompatActivity(),
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
    private lateinit var buttonA: Button
    private lateinit var btnSouth: Button
    private lateinit var btnEast: Button
    private lateinit var btnWest: Button
    private lateinit var btnBackToHome: Button
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var playerName: String
    private var gameState = BuildingNumber2.GameState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_red_metro)
        try {
            // Inicializar el mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.red_metro // Usa el croquis del metro Politécnico
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)
            // Inicializar componentes
            initializeComponents(savedInstanceState)
            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa para la red del metro
                mapView.setCurrentMap(
                    MapMatrixProvider.Companion.MAP_RED_METRO,
                    R.drawable.red_metro
                )
                // Configurar el playerManager
                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.Companion.MAP_RED_METRO)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }
                Log.d(TAG, "Set map to: " + MapMatrixProvider.Companion.MAP_RED_METRO)
                // Importante: Enviar un update inmediato para que otros jugadores sepan dónde estamos
                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        gameState.playerPosition,
                        MapMatrixProvider.Companion.MAP_RED_METRO
                    )
                }
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
            gameState.isConnected =
                intent.getBooleanExtra("IS_CONNECTED", false) // Preservar estado de conexión
            gameState.playerPosition =
                intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>
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


    // Helper method to get player rectangle in map coordinates
    private fun getPlayerRect(position: Pair<Int, Int>): RectF? {
        val bitmap = mapView.mapState.backgroundBitmap ?: return null

        // Calculate the cell size based on the bitmap dimensions and matrix size
        val cellWidth = bitmap.width / MapMatrixProvider.Companion.MAP_WIDTH.toFloat()
        val cellHeight = bitmap.height / MapMatrixProvider.Companion.MAP_HEIGHT.toFloat()

        // Calculate player position in pixels
        val playerX = position.first * cellWidth
        val playerY = position.second * cellHeight

        // Create a rectangle for the player (make it slightly smaller than a cell)
        val playerSize = min(cellWidth, cellHeight) * 0.8f
        return RectF(
            playerX + (cellWidth - playerSize) / 2,
            playerY + (cellHeight - playerSize) / 2,
            playerX + (cellWidth + playerSize) / 2,
            playerY + (cellHeight + playerSize) / 2
        )
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
                        MapMatrixProvider.Companion.MAP_RED_METRO
                    )
                    // Solicitar actualizaciones de posición
                    serverConnectionManager.onlineServerManager.requestPositionsUpdate()
                    updateBluetoothStatus("Conectado al servidor online - RED METRO")
                } else {
                    updateBluetoothStatus("Error al conectar al servidor online")
                }
            }
        }
    }

    private fun initializeViews() {// Obtener referencias a los botones de movimiento
        btnNorth = findViewById(R.id.button_north)
        btnSouth = findViewById(R.id.button_south)
        btnEast = findViewById(R.id.button_east)
        btnWest = findViewById(R.id.button_west)
        buttonA = findViewById(R.id.button_a)
        btnBackToHome = findViewById(R.id.button_back_to_home)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        tvBluetoothStatus.text = "Red Metro - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.Companion.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@RedMetro)
        }
        val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
            setListener(this@RedMetro)
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
        // Botón para volver a la salida del metro
        btnBackToHome.setOnClickListener {
            returnAndenesMetroPolitecnicoActivity()
        }

        // Configurar el botón BCK si existe
        findViewById<Button?>(R.id.button_small_2)?.setOnClickListener {
            returnAndenesMetroPolitecnicoActivity()
        }

        // Configurar el botón A para interactuar con puntos de interés
        findViewById<Button?>(R.id.button_a)?.setOnClickListener {
            handleButtonAPress()
        }
    }

    // Método para manejar la pulsación del botón A
    // Método alternativo más organizado
    private fun getStationCoordinates(position: Pair<Int, Int>): Pair<Double, Double>? {
        return when {
            //Línea 5
            position.first == 14 && position.second == 9 -> Pair(19.500610,-99.149179)  // Politécnico
            position.first == 15 && position.second == 11 -> Pair(19.489394, -99.144726) // Instituto del Petróleo
            position.first == 15 && position.second == 13 -> Pair(19.478915, -99.140606) // Autobuses del Norte
            position.first == 16 && position.second == 14 -> Pair(19.469682, -99.136548) // La Raza
            position.first == 17 && position.second == 15 -> Pair(19.463173, -99.130438) // Misterios
            position.first == 19 && position.second == 16 -> Pair(19.459016, -99.120095) // Valle Gómez
            position.first == 20 && position.second == 16 -> Pair(19.455096, -99.113524) // Consulado
            position.first == 22 && position.second == 17 -> Pair(19.451373, -99.105311) // Eduardo Molina
            position.first == 24 && position.second == 16 -> Pair(19.451226, -99.096240) // Aragón
            position.first == 25 && position.second == 18 -> Pair(19.444706, -99.086723) // Oceanía
            position.first == 25 && position.second == 19 -> Pair(19.433946, -99.088241) // Terminal Aérea
            position.first == 25 && position.second == 21 -> Pair(19.424223, -99.088011) // Hangares
            position.first == 28 && position.second == 22 -> Pair(19.415167, -99.074514) // Pantitlán
            //Línea 6
            position.first == 5 && position.second == 9 -> Pair(19.504653, -99.199945)
            position.first == 6 && position.second == 10 -> Pair(19.494923, -99.196026)
            position.first == 7 && position.second == 11 -> Pair(19.49075, -99.18627)
            position.first == 9 && position.second == 11 -> Pair(19.49065, -99.17406)
            position.first == 11 && position.second == 11 -> Pair(19.48856, -99.16298) //Norte 45
            position.first == 13 && position.second == 11 -> Pair(19.489715, -99.156117)
            position.first == 16 && position.second == 11 -> Pair(19.48785, -99.13509)
            position.first == 18 && position.second == 12 -> Pair(19.484971, -99.125530)
            position.first == 20 && position.second == 12 -> Pair(19.481492, -99.118545)
            position.first == 22 && position.second == 12 -> Pair(19.482635, -99.106818)
            //Línea 4
            position.first == 21 && position.second == 13 -> Pair(19.474351, -99.107966)
            position.first == 20 && position.second == 14 -> Pair(19.464539, -99.112043)
            position.first == 20 && position.second == 17 -> Pair(19.448747, -99.116185)
            position.first == 19 && position.second == 18 -> Pair(19.439673, -99.119039)
            position.first == 19 && position.second == 20 -> Pair(19.428796, -99.119393)
            position.first == 19 && position.second == 21 -> Pair(19.42143, -99.12041)
            position.first == 18 && position.second == 22 -> Pair(19.41066, -99.12176)
            position.first == 19 && position.second == 23 -> Pair(19.40370, -99.12153)
            //Línea 7 naranja
            position.first == 5 && position.second == 11 -> Pair(19.490549, -99.195246)
            position.first == 6 && position.second == 12 -> Pair(19.479100, -99.190026)
            position.first == 6 && position.second == 14 -> Pair(19.469607, -99.190463)
            position.first == 6 && position.second == 15 -> Pair(19.459605, -99.188202)
            position.first == 6 && position.second == 17 -> Pair(19.444944, -99.191791)
            position.first == 6 && position.second == 19 -> Pair(19.433506, -99.190919)
            position.first == 6 && position.second == 20 -> Pair(19.424969, -99.192263)
            position.first == 6 && position.second == 22 -> Pair(19.411757, -99.191394)
            position.first == 7 && position.second == 23 -> Pair(19.402073, -99.186813)
            position.first == 7 && position.second == 25 -> Pair(19.391116, -99.185913)
            position.first == 7 && position.second == 26 -> Pair(19.384440, -99.186578)
            position.first == 6 && position.second == 28 -> Pair(19.376215, -99.187564)
            position.first == 6 && position.second == 29 -> Pair(19.361529, -99.189221)
            //Línea 12
            position.first == 8 && position.second == 28 -> Pair(19.373553, -99.178412)
            position.first == 9 && position.second == 28 -> Pair(19.371873, -99.170773)
            position.first == 11 && position.second == 28 -> Pair(19.370537, -99.164797)
            position.first == 12 && position.second == 29 -> Pair(19.370547, -99.158757)
            position.first == 13 && position.second == 30 -> Pair(19.361346, -99.151568)
            position.first == 15 && position.second == 30 -> Pair(19.35992, -99.14305)
            position.first == 19 && position.second == 30 -> Pair(19.35755, -99.12269)
            position.first == 22 && position.second == 30 -> Pair(19.35542, -99.10123)
            position.first == 21 && position.second == 32 -> Pair(19.336809, -99.108921)
            position.first == 21 && position.second == 33 -> Pair(19.32797, -99.10393)
            position.first == 22 && position.second == 35 -> Pair(19.322088, -99.095682)
            position.first == 24 && position.second == 35 -> Pair(19.320418, -99.085897)
            position.first == 25 && position.second == 35 -> Pair(19.317623, -99.074589)
            position.first == 27 && position.second == 36 -> Pair(19.306253, -99.065201)
            position.first == 29 && position.second == 36 -> Pair(19.304288, -99.059494)
            position.first == 30 && position.second == 36 -> Pair(19.299863, -99.045954)
            position.first == 32 && position.second == 37 -> Pair(19.29652, -99.03419)
            position.first == 33 && position.second == 37 -> Pair(19.29413, -99.02392)
            position.first == 34 && position.second == 38 -> Pair(19.28591, -99.01409)
            //Línea A
            position.first == 28 && position.second == 23 -> Pair(19.404565, -99.069506)
            position.first == 30 && position.second == 24 -> Pair(19.398652, -99.059451)
            position.first == 32 && position.second == 25 -> Pair(19.391214, -99.046340)
            position.first == 33 && position.second == 27 -> Pair(19.385020, -99.035472)
            position.first == 33 && position.second == 28 -> Pair(19.373280, -99.016954)
            position.first == 34 && position.second == 30 -> Pair(19.364575, -99.005570)
            position.first == 34 && position.second == 31 -> Pair(19.360203, -98.995142)
            position.first == 35 && position.second == 32 -> Pair(19.35901, -98.97682)
            position.first == 36 && position.second == 33 -> Pair(19.35030, -98.96143)
            //Línea 3
            position.first == 19 && position.second == 10 -> Pair(19.495232, -99.119511)
            position.first == 17 && position.second == 13 -> Pair(19.47689, -99.13204)
            position.first == 14 && position.second == 16 -> Pair(19.45482, -99.14316)
            position.first == 14 && position.second == 17 -> Pair(19.44488, -99.14513)
            position.first == 14 && position.second == 18 -> Pair(19.43752, -99.14657)
            position.first == 14 && position.second == 19 -> Pair(19.43339, -99.14764)
            position.first == 14 && position.second == 20 -> Pair(19.42669, -99.14215)
            position.first == 13 && position.second == 21 -> Pair(19.41912, -99.15017)
            position.first == 13 && position.second == 22 -> Pair(19.41368, -99.15324)
            position.first == 13 && position.second == 23 -> Pair(19.40641, -99.15513)
            position.first == 12 && position.second == 25 -> Pair(19.39575, -99.15597)
            position.first == 12 && position.second == 26 -> Pair(19.38585, -99.15717)
            position.first == 12 && position.second == 27 -> Pair(19.37897, -99.15959)
            position.first == 10 && position.second == 30 -> Pair(19.36124, -99.17103)
            position.first == 9 && position.second == 31 -> Pair(19.35409, -99.17530)
            position.first == 8 && position.second == 32 -> Pair(19.346153, -99.180783)
            position.first == 9 && position.second == 33 -> Pair(19.33585, -99.17715)
            position.first == 9 && position.second == 35 -> Pair(19.32431, -99.17391)
            //Línea B
            position.first == 36 && position.second == 4 -> Pair(19.53433, -99.02750)
            position.first == 35 && position.second == 5 -> Pair(19.52835, -99.03025)
            position.first == 34 && position.second == 6 -> Pair(19.52113, -99.03355)
            position.first == 34 && position.second == 8 -> Pair(19.51482, -99.03636)
            position.first == 33 && position.second == 9 -> Pair(19.50147, -99.04207)
            position.first == 32 && position.second == 11 -> Pair(19.490200, -99.047059)
            position.first == 31 && position.second == 12 -> Pair(19.485679, -99.049087)
            position.first == 31 && position.second == 14 -> Pair(19.472833, -99.054612)
            position.first == 30 && position.second == 15 -> Pair(19.461565, -99.061393)
            position.first == 28 && position.second == 16 -> Pair(19.458014, -99.069332)
            position.first == 26 && position.second == 17 -> Pair(19.450983, -99.079245)
            position.first == 23 && position.second == 18 -> Pair(19.440765, -99.094416)
            position.first == 22 && position.second == 19 -> Pair(19.436587, -99.103696)
            position.first == 20 && position.second == 20 -> Pair(19.429970, -99.114318)
            position.first == 18 && position.second == 18 -> Pair(19.442687, -99.124210)
            position.first == 17 && position.second == 18 -> Pair(19.443143, -99.131817)
            position.first == 15 && position.second == 17 -> Pair(19.443841, -99.138801)
            position.first == 13 && position.second == 17 -> Pair(19.446208, -99.152652)
            //Línea 9
            position.first == 26 && position.second == 23 -> Pair(19.407193, -99.082515)
            position.first == 25 && position.second == 23 -> Pair(19.408433, -99.091342)
            position.first == 22 && position.second == 23 -> Pair(19.408544, -99.103063)
            position.first == 20 && position.second == 23 -> Pair(19.408296, -99.112671)
            position.first == 16 && position.second == 23 -> Pair(19.408615, -99.133973)
            position.first == 14 && position.second == 23 -> Pair(19.407137, -99.144412)
            position.first == 10 && position.second == 23 -> Pair(19.405862, -99.168536)
            position.first == 8  && position.second == 23 -> Pair(19.405979, -99.178739)
            //Línea 2
            position.first == 2 && position.second == 15 -> Pair(19.459987, -99.215856)
            position.first == 4 && position.second == 15 -> Pair(19.458560, -99.203078)
            position.first == 8 && position.second == 16 -> Pair(19.45710, -99.18135)
            position.first == 9 && position.second == 16 -> Pair(19.45255, -99.17547)
            position.first == 10 && position.second == 17 -> Pair(19.44915, -99.17189)
            position.first == 11 && position.second == 18 -> Pair(19.44456, -99.16736)
            position.first == 12 && position.second == 18 -> Pair(19.44187, -99.16067)
            position.first == 13 && position.second == 18 -> Pair(19.43918, -99.15425)
            position.first == 15 && position.second == 18 -> Pair(19.43612, -99.14063)
            position.first == 16 && position.second == 19 -> Pair(19.43541, -99.13715)
            position.first == 17 && position.second == 19 -> Pair(19.43240, -99.13245)
            position.first == 17 && position.second == 20 -> Pair(19.42598, -99.13303)
            position.first == 16 && position.second == 22 -> Pair(19.41593, -99.13460)
            position.first == 16 && position.second == 24 -> Pair(19.40059, -99.13683)
            position.first == 16 && position.second == 25 -> Pair(19.39510, -99.13786)
            position.first == 16 && position.second == 26 -> Pair(19.38747, -99.13895)
            position.first == 15 && position.second == 27 -> Pair(19.37953, -99.14015)
            position.first == 15 && position.second == 28 -> Pair(19.36974, -99.14155)
            position.first == 14 && position.second == 31 -> Pair(19.35306, -99.14498)
            position.first == 15 && position.second == 32 -> Pair(19.34354, -99.14037)
            //Línea 8
            position.first == 15 && position.second == 19 -> Pair(19.431164, -99.141333)
            position.first == 15 && position.second == 20 -> Pair(19.426338, -99.137857)
            position.first == 15 && position.second == 21 -> Pair(19.421349, -99.143189)
            position.first == 15 && position.second == 22 -> Pair(19.413123, -99.143994)
            position.first == 18 && position.second == 23 -> Pair(19.40654, -99.12627)
            position.first == 20 && position.second == 24 -> Pair(19.39820, -99.11344)
            position.first == 20 && position.second == 26 -> Pair(19.38789, -99.11198)
            position.first == 21 && position.second == 27 -> Pair(19.38190, -99.11080)
            position.first == 21 && position.second == 28 -> Pair(19.37316, -99.10758)
            position.first == 21 && position.second == 29 -> Pair(19.36480, -99.10960)
            position.first == 24 && position.second == 30 -> Pair(19.35757, -99.09365)
            position.first == 25 && position.second == 31 -> Pair(19.35603, -99.08576)
            position.first == 27 && position.second == 31 -> Pair(19.35073, -99.07484)
            position.first == 29 && position.second == 32 -> Pair(19.34579, -99.06372)
            //Línea 1
            position.first == 26 && position.second == 22 -> Pair(19.412490, -99.082351)
            position.first == 24 && position.second == 22 -> Pair(19.416351, -99.090264)
            position.first == 23 && position.second == 21 -> Pair(19.419740, -99.096347)
            position.first == 22 && position.second == 21 -> Pair(19.423069, -99.102495)
            position.first == 21 && position.second == 20 -> Pair(19.426348, -99.109801)
            position.first == 18 && position.second == 20 -> Pair(19.42538, -99.12522)
            position.first == 16 && position.second == 20 -> Pair(19.42627, -99.13775)
            position.first == 13 && position.second == 20 -> Pair(19.42550, -99.15466)
            position.first == 11 && position.second == 21 -> Pair(19.42351, -99.16316)
            position.first == 10 && position.second == 21 -> Pair(19.42173, -99.17071)
            position.first == 8 && position.second == 21 -> Pair(19.42058, -99.17680)
            position.first == 8 && position.second == 22 -> Pair(19.41289, -99.18202)
            position.first == 4 && position.second == 24 -> Pair(19.39826, -99.20034)




            else -> null
        }
    }

    // Uso en handleButtonAPress()
    private fun handleButtonAPress() {
        val position = gameState.playerPosition
        val coordinates = getStationCoordinates(position)

        coordinates?.let { (lat, lon) ->
            Log.d(TAG, "Abriendo coordenadas: $lat, $lon para posición $position")
            salirMetroActivity(lat, lon)
        } ?: run {
            Toast.makeText(this, "No hay estación de metro en esta ubicación", Toast.LENGTH_SHORT).show()
        }
    }
    private fun salirMetroActivity(latitude: Double, longitude: Double) {
        try {
            val intent = Intent(this, SalirMetro::class.java).apply {
                putExtra("LATITUDE", latitude)
                putExtra("LONGITUDE", longitude)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir SalirMetro: ${e.message}")
            Toast.makeText(this, "Error al abrir el mapa", Toast.LENGTH_SHORT).show()
        }
    }

    private var metro = false
    private var salida = false
    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        if (position.first == 14 && position.second == 9 ||
            position.first == 15 && position.second == 11 ||
            position.first == 15 && position.second == 13 ||
            position.first == 16 && position.second == 14 ||
            position.first == 17 && position.second == 15 ||
            position.first == 19 && position.second == 16 ||
            position.first == 20 && position.second == 16 ||
            position.first == 22 && position.second == 17 ||
            position.first == 24 && position.second == 16 ||
            position.first == 25 && position.second == 18 ||
            position.first == 25 && position.second == 19 ||
            position.first == 25 && position.second == 21 ||
            position.first == 28 && position.second == 22 ||
            position.first == 5 && position.second == 9   ||
            position.first == 6 && position.second == 10  ||
            position.first == 7 && position.second == 11  ||
            position.first == 9 && position.second == 11  ||
            position.first == 11 && position.second == 11 ||
            position.first == 13 && position.second == 11 ||
            position.first == 16 && position.second == 11 ||
            position.first == 18 && position.second == 12 ||
            position.first == 20 && position.second == 12 ||
            position.first == 22 && position.second == 12 ||
            //Línea 4
            position.first == 21 && position.second == 13 ||
            position.first == 20 && position.second == 14 ||
            position.first == 20 && position.second == 17 ||
            position.first == 19 && position.second == 18 ||
            position.first == 19 && position.second == 20 ||
            position.first == 19 && position.second == 21 ||
            position.first == 18 && position.second == 22 ||
            position.first == 19 && position.second == 23 ||
            //Línea 7 naranja
            position.first == 5 && position.second == 11  ||
            position.first == 6 && position.second == 12  ||
            position.first == 6 && position.second == 14  ||
            position.first == 6 && position.second == 15  ||
            position.first == 6 && position.second == 17  ||
            position.first == 6 && position.second == 19  ||
            position.first == 6 && position.second == 20  ||
            position.first == 6 && position.second == 22  ||
            position.first == 7 && position.second == 23  ||
            position.first == 7 && position.second == 25  ||
            position.first == 7 && position.second == 26  ||
            position.first == 6 && position.second == 28  ||
            position.first == 6 && position.second == 29  ||
            //Línea 12
            position.first == 8 && position.second == 28  ||
            position.first == 9 && position.second == 28  ||
            position.first == 11 && position.second == 28 ||
            position.first == 12 && position.second == 29 ||
            position.first == 13 && position.second == 30 ||
            position.first == 15 && position.second == 30 ||
            position.first == 19 && position.second == 30 ||
            position.first == 22 && position.second == 30 ||
            position.first == 21 && position.second == 32 ||
            position.first == 21 && position.second == 33 ||
            position.first == 22 && position.second == 35 ||
            position.first == 24 && position.second == 35 ||
            position.first == 25 && position.second == 35 ||
            position.first == 27 && position.second == 36 ||
            position.first == 29 && position.second == 36 ||
            position.first == 30 && position.second == 36 ||
            position.first == 32 && position.second == 37 ||
            position.first == 33 && position.second == 37 ||
            position.first == 34 && position.second == 38 ||
            //Línea A
            position.first == 28 && position.second == 23 ||
            position.first == 30 && position.second == 24 ||
            position.first == 32 && position.second == 25 ||
            position.first == 33 && position.second == 27 ||
            position.first == 33 && position.second == 28 ||
            position.first == 34 && position.second == 30 ||
            position.first == 34 && position.second == 31 ||
            position.first == 35 && position.second == 32 ||
            position.first == 36 && position.second == 33 ||
            //Línea 3
            position.first == 19 && position.second == 10 ||
            position.first == 17 && position.second == 13 ||
            position.first == 14 && position.second == 16 ||
            position.first == 14 && position.second == 17 ||
            position.first == 14 && position.second == 18 ||
            position.first == 14 && position.second == 19 ||
            position.first == 14 && position.second == 20 ||
            position.first == 13 && position.second == 21 ||
            position.first == 13 && position.second == 22 ||
            position.first == 13 && position.second == 23 ||
            position.first == 12 && position.second == 25 ||
            position.first == 12 && position.second == 26 ||
            position.first == 12 && position.second == 27 ||
            position.first == 10 && position.second == 30 ||
            position.first == 9 && position.second == 31  ||
            position.first == 9 && position.second == 32  ||
            position.first == 9 && position.second == 33  ||
            position.first == 9 && position.second == 35  ||
            //Línea B
            position.first == 36 && position.second == 4  ||
            position.first == 35 && position.second == 5  ||
            position.first == 34 && position.second == 6  ||
            position.first == 34 && position.second == 8  ||
            position.first == 33 && position.second == 9  ||
            position.first == 32 && position.second == 11 ||
            position.first == 31 && position.second == 12 ||
            position.first == 31 && position.second == 14 ||
            position.first == 30 && position.second == 15 ||
            position.first == 28 && position.second == 16 ||
            position.first == 26 && position.second == 17 ||
            position.first == 23 && position.second == 18 ||
            position.first == 22 && position.second == 19 ||
            position.first == 20 && position.second == 20 ||
            position.first == 18 && position.second == 18 ||
            position.first == 17 && position.second == 18 ||
            position.first == 15 && position.second == 17 ||
            position.first == 13 && position.second == 17 ||
            //Línea 9
            position.first == 26 && position.second == 23 ||
            position.first == 25 && position.second == 23 ||
            position.first == 22 && position.second == 23 ||
            position.first == 20 && position.second == 23 ||
            position.first == 16 && position.second == 23 ||
            position.first == 14 && position.second == 23 ||
            position.first == 10 && position.second == 23 ||
            position.first == 8 && position.second == 23  ||
            // Línea 2
            position.first == 2 && position.second == 15  ||
            position.first == 4 && position.second == 15  ||
            position.first == 8 && position.second == 16  ||
            position.first == 9 && position.second == 16  ||
            position.first == 10 && position.second == 17 ||
            position.first == 11 && position.second == 18 ||
            position.first == 12 && position.second == 18 ||
            position.first == 13 && position.second == 18 ||
            position.first == 15 && position.second == 18 ||
            position.first == 16 && position.second == 19 ||
            position.first == 17 && position.second == 19 ||
            position.first == 17 && position.second == 20 ||
            position.first == 16 && position.second == 22 ||
            position.first == 16 && position.second == 24 ||
            position.first == 16 && position.second == 25 ||
            position.first == 16 && position.second == 26 ||
            position.first == 15 && position.second == 27 ||
            position.first == 15 && position.second == 28 ||
            position.first == 14 && position.second == 31 ||
            position.first == 15 && position.second == 32 ||
            // Línea 8
            position.first == 15 && position.second == 19 ||
            position.first == 15 && position.second == 20 ||
            position.first == 15 && position.second == 21 ||
            position.first == 15 && position.second == 22 ||
            position.first == 18 && position.second == 23 ||
            position.first == 20 && position.second == 24 ||
            position.first == 20 && position.second == 26 ||
            position.first == 21 && position.second == 27 ||
            position.first == 21 && position.second == 28 ||
            position.first == 21 && position.second == 29 ||
            position.first == 24 && position.second == 30 ||
            position.first == 25 && position.second == 31 ||
            position.first == 27 && position.second == 31 ||
            position.first == 29 && position.second == 32 ||
            // Línea 1
            position.first == 26 && position.second == 22 ||
            position.first == 24 && position.second == 22 ||
            position.first == 23 && position.second == 21 ||
            position.first == 22 && position.second == 21 ||
            position.first == 21 && position.second == 20 ||
            position.first == 18 && position.second == 20 ||
            position.first == 16 && position.second == 20 ||
            position.first == 13 && position.second == 20 ||
            position.first == 11 && position.second == 21 ||
            position.first == 10 && position.second == 21 ||
            position.first == 8 && position.second == 21  ||
            position.first == 8 && position.second == 22  ||
            position.first == 4 && position.second == 24

                ) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Presiona A ver el mapa de la estación",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }  else{
                salida = false
            }
    }


    private fun returnAndenesMetroPolitecnicoActivity() {
        val intent = Intent(this, AndenesMetroPolitecnico::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(36, 5))
            putExtra("PREVIOUS_POSITION", gameState.playerPosition) // Guarda la posición actual
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
                serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.Companion.MAP_RED_METRO)
                // Log de debug para confirmar
                Log.d(TAG, "Sending update: Player $playerName at $position in map ${MapMatrixProvider.Companion.MAP_RED_METRO}")
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
                returnAndenesMetroPolitecnicoActivity()
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
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.Companion.MAP_RED_METRO)
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
                                if (map == MapMatrixProvider.Companion.MAP_RED_METRO) {
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
                            if (map == MapMatrixProvider.Companion.MAP_RED_METRO) {
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
                            MapMatrixProvider.Companion.MAP_RED_METRO
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
                MapMatrixProvider.Companion.MAP_RED_METRO
            )
        }

    }

    companion object {
        private const val TAG = "RedMetro"
    }
}