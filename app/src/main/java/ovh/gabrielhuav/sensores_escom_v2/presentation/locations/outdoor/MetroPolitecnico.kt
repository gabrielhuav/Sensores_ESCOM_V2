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


class MetroPolitecnico : AppCompatActivity(),
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
        setContentView(R.layout.activity_metro_politecnico)
        try {
            // Inicializar el mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.metro_politecnico // Usa el croquis del metro Politécnico
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)
            // Inicializar componentes
            initializeComponents(savedInstanceState)
            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa para la salida del metro
                mapView.setCurrentMap(
                    MapMatrixProvider.Companion.MAP_METRO_POLITECNICO,
                    R.drawable.metro_politecnico
                )
                // Configurar el playerManager
                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.Companion.MAP_METRO_POLITECNICO)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }
                Log.d(TAG, "Set map to: " + MapMatrixProvider.Companion.MAP_METRO_POLITECNICO)
                // Importante: Enviar un update inmediato para que otros jugadores sepan dónde estamos
                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        gameState.playerPosition,
                        MapMatrixProvider.Companion.MAP_METRO_POLITECNICO
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
                        MapMatrixProvider.Companion.MAP_METRO_POLITECNICO
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

    private fun initializeViews() {// Obtener referencias a los botones de movimiento
        btnNorth = findViewById(R.id.button_north)
        btnSouth = findViewById(R.id.button_south)
        btnEast = findViewById(R.id.button_east)
        btnWest = findViewById(R.id.button_west)
        buttonA = findViewById(R.id.button_a)
        btnBackToHome = findViewById(R.id.button_back_to_home)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        tvBluetoothStatus.text = "Metro Politécnico - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.Companion.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@MetroPolitecnico)
        }
        val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
            setListener(this@MetroPolitecnico)
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
            returnSalidaMetroActivity()
        }

        // Configurar el botón BCK si existe
        findViewById<Button?>(R.id.button_small_2)?.setOnClickListener {
            returnSalidaMetroActivity()
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
            position.first == 21 && position.second == 11 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Taquilla",
                    "Línea 5 del Metro - Estación Politécnico\n\nHorario: 5:00 - 24:00\nTarifa: $5.00 MXN\nGracias por su compra!",
                )
            }

            position.first == 21 && position.second == 12 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Taquilla",
                    "Línea 5 del Metro - Estación Politécnico\n\nHorario: 5:00 - 24:00\nTarifa: $5.00 MXN\nGracias por su compra!",
                )
            }

            position.first == 31 && position.second == 27 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Taquilla",
                    "Línea 5 del Metro - Estación Politécnico\n\nHorario: 5:00 - 24:00\nTarifa: $5.00 MXN\nGracias por su compra!",
                )
            }

            position.first == 31 && position.second == 28 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Taquilla",
                    "Línea 5 del Metro - Estación Politécnico\n\nHorario: 5:00 - 24:00\nTarifa: $5.00 MXN\nGracias por su compra!",
                )
            }

            position.first == 14 && position.second == 27 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Recarga tu saldo",
                    "Ingresa monedas o billetes",
                    null,
                    R.drawable.maquina
                )
            }

            position.first == 14 && position.second == 28 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Recarga tu saldo",
                    "Ingresa monedas o billetes",
                    null,
                    R.drawable.maquina
                )
            }

            position.first == 12 && position.second == 21 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Puesto de venta",
                    "¿Qué desea comprar?",
                    null,
                    R.drawable.puesto
                )
            }
            position.first == 32 && position.second == 20 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Puesto de venta",
                    "¿Qué desea comprar?",
                    null,
                    R.drawable.puesto
                )
            }
            //ABAJO
            position.first == 29 && position.second == 21 -> {
                startAndenesMetroPolitecnicoActivityAbajo()
            }
            //ARRIBA
            position.first == 29 && position.second == 18 -> {
                startAndenesMetroPolitecnicoActivityArriba()
            }
            //ARRIBA
            position.first == 6 && position.second == 18 -> {
                startAndenesMetroPolitecnicoActivityArriba()
            }
            //ABAJO
            position.first == 6 && position.second == 21 -> {
                startAndenesMetroPolitecnicoActivityAbajo()
            }

            position.first == 12 && position.second == 24 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Linea 5",
                    "Estaciones",
                    null,
                    R.drawable.linea5
                )
            }

            position.first == 1 && position.second == 19 -> {
                // Mostrar información del Metro y enlace
                showInfoDialog(
                    "Linea 5",
                    "Mural",
                    null,
                    R.drawable.mural
                )
            }


        }
    }

    // Método para mostrar un diálogo con información y opcionalmente un enlace web
    private fun showInfoDialog(
        title: String,
        message: String,
        url: String? = null,
        imageResId: Int? = null
    ) {
        // Crear un contenedor vertical
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 10)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Si hay imagen, agregarla arriba
        if (imageResId != null) {
            val imageView = ImageView(this).apply {
                setImageResource(imageResId)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    400 // altura en px, puedes ajustar
                ).apply {
                    bottomMargin = 20
                }
            }
            layout.addView(imageView)
        }

        // Agregar el mensaje debajo de la imagen (si existe)
        val textView = TextView(this).apply {
            text = message
            setTextColor(Color.BLACK)
            textSize = 16f
            setPadding(0, 10, 0, 10)
        }
        layout.addView(textView)

        // Construir el diálogo
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        // Si hay un enlace web, agregar botón adicional
        if (url != null) {
            builder.setNeutralButton("Visitar sitio web") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al abrir URL: $url", e)
                    Toast.makeText(this, "No se pudo abrir el enlace.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.show()
    }


    private var metro = false
    private var salida = false
    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        if (position.first == 21 && position.second == 11 ||
            position.first == 21 && position.second == 12 ||
            position.first == 31 && position.second == 27 ||
            position.first == 31 && position.second == 28
        ) {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Presiona A para usar la taquilla del metro Politécnico",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (position.first == 14 && position.second == 27 ||
                   position.first == 14 && position.second == 28) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para utilizar la máquina", Toast.LENGTH_SHORT).show()
            }
        }else if (position.first == 6 && position.second == 21  ||
            position.first == 6 && position.second == 18        ||
            position.first == 29 && position.second == 18       ||
            position.first == 29 && position.second == 21
            ) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para ingresar a los andenes", Toast.LENGTH_SHORT).show()
            }
        }else if (position.first == 32 && position.second == 20 ||
            position.first == 12 && position.second == 21) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para comprar en el puesto de venta", Toast.LENGTH_SHORT).show()
            }
        }
        else if (position.first == 35 && position.second == 5) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para ver datos del Metro", Toast.LENGTH_SHORT).show()
            }
        } else if (position.first == 31 && position.second == 27) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para ver datos del Trolebus", Toast.LENGTH_SHORT).show()
            }
        } else if (position.first == 17 && position.second == 22) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para ver datos de la Ford", Toast.LENGTH_SHORT).show()
            }
        } else if (position.first == 12 && position.second == 24) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para ver las estaciones de la Linea 5", Toast.LENGTH_SHORT).show()
            }
        } else if (position.first == 1 && position.second == 19) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para ver el mural", Toast.LENGTH_SHORT).show()
            }
        } else if (position.first == 10 && position.second == 20 ||
                   position.first == 10 && position.second == 21 ||
                   position.first == 26 && position.second == 20 ||
                   position.first == 26 && position.second == 21) {
            if (!metro) {
                metro = true
                showInfoDialog(
                    "Viaje Pagado",
                    "¡Bienvenido!",
                    null,
                    R.drawable.saldo
                )
            } else {
                metro = false
            }
        }else if(position.first == 10 && position.second == 18 ||
                 position.first == 26 && position.second == 18){
                     if (!salida){
                        salida = true
                        showInfoDialog(
                            "Únicamente salida",
                            "¡Regresa Pronto!",
                            null,
                            R.drawable.salida
                        )
                     }else{
                         salida = false
                     }
        }

    }

    private fun startAndenesMetroPolitecnicoActivityArriba() {
        val intent = Intent(this, AndenesMetroPolitecnico::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(20, 3))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun startAndenesMetroPolitecnicoActivityAbajo() {
        val intent = Intent(this, AndenesMetroPolitecnico::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(20, 35))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun returnSalidaMetroActivity() {
            val intent = Intent(this, SalidaMetro::class.java).apply {
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
                    serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.Companion.MAP_METRO_POLITECNICO)
                    // Log de debug para confirmar
                    Log.d(TAG, "Sending update: Player $playerName at $position in map ${MapMatrixProvider.Companion.MAP_METRO_POLITECNICO}")
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
                    returnSalidaMetroActivity()
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
                mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.Companion.MAP_METRO_POLITECNICO)
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
                                    if (map == MapMatrixProvider.Companion.MAP_METRO_POLITECNICO) {
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
                                if (map == MapMatrixProvider.Companion.MAP_METRO_POLITECNICO) {
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
                                MapMatrixProvider.Companion.MAP_METRO_POLITECNICO
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
                    MapMatrixProvider.Companion.MAP_METRO_POLITECNICO
                )
            }

        }

        companion object {
            private const val TAG = "SalidaMetro"
        }
}