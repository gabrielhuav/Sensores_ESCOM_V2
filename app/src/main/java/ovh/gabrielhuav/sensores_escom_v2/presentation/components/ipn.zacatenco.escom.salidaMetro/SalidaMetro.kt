package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.salidaMetro

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.Uri // <-- Importar Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.BuildingNumber2
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.GameplayActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.Zacatenco
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min

class SalidaMetro : AppCompatActivity(),
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
    private var isShowingCollisionDialog = false

    private var gameState = BuildingNumber2.GameState()
    // Car animation properties
    private val carList = mutableListOf<Car>()
    private val carColors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN)
    private val handler = Handler(Looper.getMainLooper())
    private val carUpdateRunnable = object : Runnable {
        override fun run() {
            updateCars()
            handler.postDelayed(this, 16) // ~60fps
        }
    }
      // Sistema de semáforos mejorado
    private val trafficLights = mutableListOf<TrafficLight>()
    private var trafficLightCycle = 0
    private val TRAFFIC_LIGHT_CYCLE_DURATION = 3000L // 3 segundos por fase
    private val trafficLightRunnable = object : Runnable {
        override fun run() {
            updateTrafficLights()
            handler.postDelayed(this, TRAFFIC_LIGHT_CYCLE_DURATION)
        }
    }
      // Clase para manejar semáforos simplificada
    inner class TrafficLight(
        val x: Float,
        val y: Float,
        val isVertical: Boolean // true para norte-sur, false para este-oeste
    ) {
        var isGreen = true // Empezar en verde
        val rect = RectF(x, y, x + 25f, y + 35f) // Hacer más grande
        
        fun shouldStopCar(car: Car): Boolean {
            val distance = if (isVertical) {
                kotlin.math.abs(car.x - x)
            } else {
                kotlin.math.abs(car.y - y)
            }
            return !isGreen && distance < 80f // Aumentar distancia de detección
        }
    }
    
    private fun initializeTrafficLights() {
        // Colocar semáforos en intersecciones críticas
        val mapBitmap = mapView.mapState.backgroundBitmap ?: return
        
        // Limpiar semáforos existentes
        trafficLights.clear()
        
        // Semáforo principal cerca del metro
        trafficLights.add(TrafficLight(
            mapBitmap.width * 0.8f,
            mapBitmap.height * 0.75f,
            false // horizontal
        ))
        
        // Semáforo secundario
        trafficLights.add(TrafficLight(
            mapBitmap.width * 0.4f,
            mapBitmap.height * 0.8f,
            true // vertical
        ))
        
        // Semáforo adicional para más realismo
        trafficLights.add(TrafficLight(
            mapBitmap.width * 0.6f,
            mapBitmap.height * 0.85f,
            false // horizontal
        ))
    }
    
    private fun updateTrafficLights() {
        trafficLightCycle = (trafficLightCycle + 1) % 4 // Ciclo de 4 fases
        
        trafficLights.forEachIndexed { index, light ->
            // Alternar entre verde y rojo de manera más natural
            when (trafficLightCycle) {
                0 -> light.isGreen = (index % 2 == 0) // Semáforos pares en verde
                1 -> light.isGreen = true // Todos en verde (fase de transición)
                2 -> light.isGreen = (index % 2 == 1) // Semáforos impares en verde
                3 -> light.isGreen = false // Todos en rojo (fase de parada)
            }
        }
          Log.d("TrafficLight", "Updated lights - Cycle: $trafficLightCycle")
    }
    
    // Funciones del sistema de clima
    private fun initializeWeather() {
        currentWeather = WeatherState.SUNNY
        weatherAlpha = 0f
        rainDrops.clear()
        
        // Inicializar algunas gotas de lluvia
        val mapBitmap = mapView.mapState.backgroundBitmap
        if (mapBitmap != null) {
            for (i in 0 until 50) {
                rainDrops.add(RainDrop(
                    Math.random().toFloat() * mapBitmap.width,
                    Math.random().toFloat() * mapBitmap.height
                ))
            }
        }
    }
    
    private fun changeWeather() {
        val newWeather = WeatherState.values()[(Math.random() * WeatherState.values().size).toInt()]
        currentWeather = newWeather
        
        when (currentWeather) {
            WeatherState.SUNNY -> weatherAlpha = 0f
            WeatherState.CLOUDY -> weatherAlpha = 0.3f
            WeatherState.RAINY -> weatherAlpha = 0.5f
            WeatherState.NIGHT -> weatherAlpha = 0.7f
        }
        
        Log.d("Weather", "Weather changed to: $currentWeather")
    }
    
    private fun updateWeather() {
        if (currentWeather == WeatherState.RAINY) {
            // Actualizar gotas de lluvia
            for (drop in rainDrops) {
                drop.update()
            }
        }
    }
    
    private fun startWeatherSystem() {
        handler.post(weatherChangeRunnable)
    }

    // Car class to represent each moving car - update to use map coordinates
    inner class Car(
        var x: Float,
        var y: Float,
        val width: Float = 40f,  // Reduced from 60f to 40f
        val height: Float = 20f,  // Reduced from 30f to 20f
        val speed: Float,
        val color: Int
    ) {
        val rect = RectF(x, y, x + width, y + height)
        
        fun update() {
            x -= speed
            // Get map bitmap dimensions for proper positioning
            val mapBitmap = mapView.mapState.backgroundBitmap
            if (mapBitmap != null) {
                val mapWidth = mapBitmap.width.toFloat()
                
                // If car goes off left side, reset to right side
                if (x < -width) {
                    x = mapWidth
                    // Position in the bottom area of the map (road area)
                    y = mapBitmap.height * (0.7f + Math.random().toFloat() * 0.25f)
                }
                // Update rectangle position
                rect.left = x
                rect.right = x + width
                rect.top = y
                rect.bottom = y + height
            }
        }    }

    // Sistema de clima dinámico 
    private enum class WeatherState { SUNNY, CLOUDY, RAINY, NIGHT }
    private var currentWeather = WeatherState.SUNNY
    private val rainDrops = mutableListOf<RainDrop>()
    private val WEATHER_CHANGE_INTERVAL = 30000L
    private var weatherAlpha = 0f
    
    // Clase para gotas de lluvia
    inner class RainDrop(
        var x: Float,
        var y: Float,
        val speed: Float = 8f + (Math.random().toFloat() * 4f),
        val length: Float = 15f + (Math.random().toFloat() * 10f)
    ) {
        fun update() {
            y += speed
            x += speed * 0.3f // Efecto de viento diagonal
            
            // Resetear la gota cuando salga de la pantalla
            val mapBitmap = mapView.mapState.backgroundBitmap
            if (mapBitmap != null && y > mapBitmap.height) {
                y = -length
                x = Math.random().toFloat() * mapBitmap.width
            }
        }
    }
    
    // Runnable para cambios de clima
    private val weatherChangeRunnable = object : Runnable {
        override fun run() {
            changeWeather()
            handler.postDelayed(this, WEATHER_CHANGE_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salidametro)
        try {
            // Inicializar el mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.escom_salidametro // Usa la imagen de la salida
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)
            // Inicializar componentes
            initializeComponents(savedInstanceState)
            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa para la salida del metro
                mapView.setCurrentMap(MapMatrixProvider.MAP_SALIDAMETRO, R.drawable.escom_salidametro)
                // Configurar el playerManager
                mapView.playerManager.apply {
                    setCurrentMap(MapMatrixProvider.MAP_SALIDAMETRO)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(gameState.playerPosition)
                }
                Log.d(TAG, "Set map to: " + MapMatrixProvider.MAP_SALIDAMETRO)
                // Importante: Enviar un update inmediato para que otros jugadores sepan dónde estamos
                if (gameState.isConnected) {
                    serverConnectionManager.sendUpdateMessage(playerName, gameState.playerPosition, MapMatrixProvider.MAP_SALIDAMETRO)
                }                // Initialize enhanced systems after mapView is created (autos, semáforos y clima)
                initializeCars()
                initializeTrafficLights()
                initializeWeather()
                startCarAnimation()
                startTrafficLightCycle()
                startWeatherSystem()
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

    private fun initializeCars() {
        // Wait until map is fully loaded
        mapView.post {
            val mapBitmap = mapView.mapState.backgroundBitmap ?: return@post            
            // Create 5 cars with different speeds and positions
            val mapWidth = mapBitmap.width.toFloat()
            val mapHeight = mapBitmap.height.toFloat()
            // Only use bottom 30% of map for cars (road area)
            val minY = mapHeight * 0.7f
            val maxY = mapHeight * 0.95f
            // Clear any existing cars
            carList.clear()
            // Create cars with different positions, speeds and colors
            for (i in 0 until 5) {
                val speed = 2f + (Math.random().toFloat() * 4f) // Random speed between 2-6
                // Create more distinct lanes for cars
                val laneCount = 3
                val laneHeight = (maxY - minY) / laneCount
                val lane = i % laneCount
                val y = minY + (lane * laneHeight) + (Math.random().toFloat() * (laneHeight * 0.6f))
                // Distribute cars horizontally with more spacing
                val x = mapWidth * (i / 5f) + (Math.random().toFloat() * mapWidth * 0.2f)
                val color = carColors[i % carColors.size]
                carList.add(Car(x, y, 40f, 20f, speed, color))
            }            // Set the enhanced renderer for MapView (autos, semáforos y clima)
            mapView.setCarRenderer(object : MapView.CarRenderer {
                override fun drawCars(canvas: Canvas) {
                    val paint = Paint().apply {
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    
                    // Draw cars
                    for (car in carList) {
                        paint.color = car.color
                        canvas.drawRect(car.rect, paint)
                        
                        // Añadir un borde para mejor visualización
                        paint.color = Color.BLACK
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 2f
                        canvas.drawRect(car.rect, paint)
                        paint.style = Paint.Style.FILL
                    }
                    
                    // Draw traffic lights
                    for (light in trafficLights) {
                        // Dibujar el semáforo más visible
                        paint.color = if (light.isGreen) Color.GREEN else Color.RED
                        canvas.drawRect(light.rect, paint)
                        
                        // Añadir borde al semáforo
                        paint.color = Color.BLACK
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 3f
                        canvas.drawRect(light.rect, paint)
                        paint.style = Paint.Style.FILL
                        
                        // Draw light pole
                        paint.color = Color.GRAY
                        canvas.drawRect(
                            light.rect.centerX() - 3f,
                            light.rect.bottom,
                            light.rect.centerX() + 3f,
                            light.rect.bottom + 25f,
                            paint
                        )
                    }
                    
                    // Draw weather effects
                    drawWeatherEffects(canvas, paint)
                }
                
                private fun drawWeatherEffects(canvas: Canvas, paint: Paint) {
                    when (currentWeather) {
                        WeatherState.RAINY -> {
                            // Dibujar gotas de lluvia
                            paint.color = Color.argb(150, 200, 200, 255)
                            paint.strokeWidth = 2f
                            paint.style = Paint.Style.STROKE
                            
                            for (drop in rainDrops) {
                                canvas.drawLine(
                                    drop.x, drop.y,
                                    drop.x + drop.length * 0.3f, drop.y + drop.length,
                                    paint
                                )
                            }
                        }
                        WeatherState.NIGHT -> {
                            // Oscurecer la pantalla
                            paint.color = Color.argb((weatherAlpha * 255).toInt(), 0, 0, 100)
                            paint.style = Paint.Style.FILL
                            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
                        }
                        WeatherState.CLOUDY -> {
                            // Efecto nublado
                            paint.color = Color.argb((weatherAlpha * 100).toInt(), 128, 128, 128)
                            paint.style = Paint.Style.FILL
                            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
                        }
                        WeatherState.SUNNY -> {
                            // Sin efectos adicionales
                        }
                    }
                }
            })
        }
    }

    private fun startCarAnimation() {
        handler.post(carUpdateRunnable)
    }
    
    private fun startTrafficLightCycle() {
        handler.post(trafficLightRunnable)
    }    private fun updateCars() {
        // Update car positions
        for (car in carList) {
            // Verificar semáforos antes de actualizar
            var shouldStop = false
            for (light in trafficLights) {
                if (light.shouldStopCar(car)) {
                    shouldStop = true
                    break
                }
            }
            
            if (!shouldStop) {
                car.update()
            }
        }
        
        // Apply collision prevention between cars
        preventCollisions()
        
        // Check for collisions with player
        checkPlayerCarCollisions()
        
        // Update weather effects
        updateWeather()
        
        // Request redraw
        mapView.invalidate()
    }

    private fun checkPlayerCarCollisions() {
        // Get player position in map coordinates
        val playerPosition = gameState.playerPosition
        val playerRect = getPlayerRect(playerPosition)
        
        if (playerRect != null) {
            // Check collision with each car
            for (car in carList) {
                if (RectF.intersects(car.rect, playerRect)) {
                    // Collision detected! Show dialog
                    showCarCollisionDialog(car)
                    
                    // Move player back slightly to avoid continuous collisions
                    val newPosition = Pair(
                        playerPosition.first - 1,
                        playerPosition.second - 1
                    )
                    updatePlayerPosition(newPosition)
                    
                    // Only handle one collision at a time
                    break
                }
            }
        }
    }
    
    // Helper method to get player rectangle in map coordinates
    private fun getPlayerRect(position: Pair<Int, Int>): RectF? {
        val bitmap = mapView.mapState.backgroundBitmap ?: return null
        
        // Calculate the cell size based on the bitmap dimensions and matrix size
        val cellWidth = bitmap.width / MapMatrixProvider.MAP_WIDTH.toFloat()
        val cellHeight = bitmap.height / MapMatrixProvider.MAP_HEIGHT.toFloat()
        
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
    
    // Dialog to show when player collides with a car
    private fun showCarCollisionDialog(car: Car) {
        // Check if we already have a dialog showing to prevent multiple dialogs
        if (isShowingCollisionDialog) return
        
        isShowingCollisionDialog = true
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("¡Cuidado!")
        builder.setMessage("Has chocado con un vehículo. Ten más cuidado al cruzar la calle.")
        builder.setPositiveButton("OK") { dialog, _ -> 
            dialog.dismiss()
            isShowingCollisionDialog = false
        }
        
        runOnUiThread {
            builder.show()
        }
    }

    private fun preventCollisions() {
        // Check each pair of cars for potential collisions
        for (i in carList.indices) {
            for (j in carList.indices) {
                if (i != j) {
                    val car1 = carList[i]
                    val car2 = carList[j]
                    // Check if cars are in the same lane (similar y position)
                    if (Math.abs(car2.y - car1.y) < car1.height * 1.2f) {
                        // Check if car2 is behind car1 and too close
                        if (car2.x > car1.x && car2.x - (car1.x + car1.width) < car1.width * 1.5f) {
                            // Slow down car2 to prevent collision
                            car2.x += car2.speed * 0.8f
                            // Slightly adjust y position to encourage lane changing
                            car2.y += (Math.random().toFloat() - 0.5f) * car2.height * 0.3f
                            // Update rectangle position after adjustment
                            car2.rect.left = car2.x
                            car2.rect.right = car2.x + car2.width
                            car2.rect.top = car2.y
                            car2.rect.bottom = car2.y + car2.height
                        }
                    }
                }
            }
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
                        MapMatrixProvider.MAP_SALIDAMETRO
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
        btnBackToHome = findViewById(R.id.button_back_to_home)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        tvBluetoothStatus.text = "Salida Metro - Conectando..."
    }

    private fun initializeManagers() {
        bluetoothManager = BluetoothManager.getInstance(this, tvBluetoothStatus).apply {
            setCallback(this@SalidaMetro)
        }
        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@SalidaMetro)
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
            returnToMainActivity()
        }

        // Configurar el botón BCK si existe
        findViewById<Button?>(R.id.button_small_2)?.setOnClickListener {
            startZacatencoActivity()
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
            // Puntos de interés principales
            position.first == 35 && position.second == 5 -> {
                showInfoDialog(
                    "Metro CDMX",
                    "Línea 6 - Estación Instituto del Petróleo\n\n" +
                    "🚇 Horario: 5:00 - 24:00\n" +
                    "💰 Tarifa: $5.00 MXN\n" +
                    "📍 Conexiones: Línea 6 completa",
                    "https://www.metro.cdmx.gob.mx/"
                )
            }
            position.first == 31 && position.second == 27 -> {
                showInfoDialog(
                    "Trolebús",
                    "Línea K - Estación Politécnico\n\n" +
                    "🚌 Horario: 5:30 - 23:30\n" +
                    "💰 Tarifa: $4.00 MXN\n" +
                    "🗺️ Ruta: Politécnico - Metro Indios Verdes",
                    "https://www.ste.cdmx.gob.mx/trolebus"
                )
            }
            position.first == 17 && position.second == 22 -> {
                showInfoDialog(
                    "Ford Lindavista",
                    "Agencia Automotriz Ford\n\n" +
                    "🚗 Horario: 9:00 - 18:00\n" +
                    "🔧 Servicios: Ventas y mantenimiento\n" +
                    "📞 Atención al cliente disponible",
                    "https://www.fordmylsa.mx/"
                )
            }
            // Nuevos puntos de interés
            position.first == 28 && position.second == 8 -> {
                showInfoDialog(
                    "Farmacia San Pablo",
                    "Farmacia 24 horas\n\n" +
                    "💊 Horario: 24/7\n" +
                    "🏥 Servicios: Medicamentos, consultas médicas\n" +
                    "💳 Acepta tarjetas y efectivo"
                )
            }
            position.first == 35 && position.second == 15 -> {
                showInfoDialog(
                    "Cajero Automático",
                    "BBVA Bancomer\n\n" +
                    "💳 Disponible 24/7\n" +
                    "💰 Retiros, consultas, depósitos\n" +
                    "🏦 Sin comisión para clientes BBVA"
                )
            }
            position.first == 25 && position.second == 30 -> {
                showInfoDialog(
                    "Restaurante El Buen Sazón",
                    "Comida mexicana tradicional\n\n" +
                    "🍽️ Horario: 8:00 - 22:00\n" +
                    "🌮 Especialidad: Tacos y quesadillas\n" +
                    "💰 Precios accesibles"
                )
            }
            position.first == 8 && position.second == 12 -> {
                showInfoDialog(
                    "OXXO",
                    "Tienda de conveniencia\n\n" +
                    "🏪 Horario: 24/7\n" +
                    "🛒 Productos: Comida, bebidas, servicios\n" +
                    "💳 Pago de servicios disponible"
                )
            }
            position.first == 12 && position.second == 25 -> {
                showInfoDialog(
                    "Parada de Autobús",
                    "Transporte público urbano\n\n" +
                    "🚌 Rutas: 1, 15, 42, 108\n" +
                    "⏰ Cada 10-15 minutos\n" +
                    "💰 Tarifa: $5.50 MXN"
                )
            }
            position.first == 30 && position.second == 18 -> {
                showInfoDialog(
                    "Plaza Lindavista",
                    "Centro comercial\n\n" +
                    "🛍️ Horario: 10:00 - 22:00\n" +
                    "🏪 Tiendas: Ropa, electrónicos, comida\n" +
                    "🍕 Área de comidas en planta alta"
                )
            }
        }
    }

    // Método para mostrar un diálogo con información y opcionalmente un enlace web
    private fun showInfoDialog(title: String, message: String, url: String? = null) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        // Si se proporciona una URL, añadir un botón para abrirla
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
    
    // Sobrecarga para diálogos sin URL
    private fun showInfoDialog(title: String, message: String) {
        showInfoDialog(title, message, null)
    }

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        // Definir mensajes específicos para cada punto de interés
        val message = when {
            position.first == 35 && position.second == 5 -> "🚇 Presiona A para ver información del Metro"
            position.first == 31 && position.second == 27 -> "🚌 Presiona A para ver información del Trolebús"
            position.first == 17 && position.second == 22 -> "🚗 Presiona A para ver información de Ford"
            position.first == 28 && position.second == 8 -> "💊 Presiona A para ver información de la Farmacia"
            position.first == 35 && position.second == 15 -> "💳 Presiona A para usar el Cajero Automático"
            position.first == 25 && position.second == 30 -> "🍽️ Presiona A para ver menú del Restaurante"
            position.first == 8 && position.second == 12 -> "🏪 Presiona A para ver servicios de OXXO"
            position.first == 12 && position.second == 25 -> "🚌 Presiona A para ver horarios de Autobús"
            position.first == 30 && position.second == 18 -> "🛍️ Presiona A para ver tiendas de la Plaza"
            else -> null
        }
        
        message?.let {
            runOnUiThread {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnToMainActivity() {
        // Obtener la posición previa del intent
        val previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
            ?: Pair(15, 10) // Posición por defecto si no hay previa
        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", previousPosition) // Usar la posición previa
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        // Limpiar datos antes de cambiar de activity
        mapView.playerManager.cleanup()
        startActivity(intent)
        finish()
    }

    private fun startZacatencoActivity() {
        val intent = Intent(this, Zacatenco::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", gameState.isServer)
            putExtra("INITIAL_POSITION", Pair(10, 12))
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
                serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.MAP_SALIDAMETRO)
                // Log de debug para confirmar
                Log.d(TAG, "Sending update: Player $playerName at $position in map ${MapMatrixProvider.MAP_SALIDAMETRO}")
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
            MapMatrixProvider.MAP_MAIN -> {
                returnToMainActivity()
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
            mapView.updateRemotePlayerPosition(deviceName, Pair(x, y), MapMatrixProvider.MAP_SALIDAMETRO)
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
                                if (map == MapMatrixProvider.MAP_CAFETERIA) {
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
                            if (map == MapMatrixProvider.MAP_SALIDAMETRO) {
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
                            MapMatrixProvider.MAP_SALIDAMETRO
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
                MapMatrixProvider.MAP_SALIDAMETRO
            )
        }
        // Restart car animation
        startCarAnimation()
    }    override fun onPause() {
        super.onPause()
        // Stop all animations when activity is paused
        handler.removeCallbacks(carUpdateRunnable)
        handler.removeCallbacks(trafficLightRunnable)
        handler.removeCallbacks(weatherChangeRunnable)
        movementManager.stopMovement()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to remove all callbacks to prevent memory leaks
        handler.removeCallbacks(carUpdateRunnable)
        handler.removeCallbacks(trafficLightRunnable)
        handler.removeCallbacks(weatherChangeRunnable)
        bluetoothManager.cleanup()
    }
    companion object {
        private const val TAG = "SalidaMetro"
    }
}