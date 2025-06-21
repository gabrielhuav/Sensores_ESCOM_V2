package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.salidaMetro

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.Uri // <-- Importar Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
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
    }    // Clase para manejar semáforos simplificada
    inner class TrafficLight(
        val x: Float,
        val y: Float,
        val isVertical: Boolean // true para norte-sur, false para este-oeste
    ) {
        var isGreen = true // Empezar en verde
        val rect = RectF(x, y, x + 25f, y + 35f) // Hacer más grande
          fun shouldStopCar(car: Car): Boolean {
            if (isGreen) return false // Si está en verde, no detener
            
            // Los autos se mueven hacia la izquierda (x decrece)
            // Solo detener si el semáforo está ADELANTE del auto
            val isTrafficLightAhead = car.x > x
            
            if (!isTrafficLightAhead) return false // Si ya pasó el semáforo, no detener
            
            val distance = if (isVertical) {
                kotlin.math.abs(car.x - x)
            } else {
                kotlin.math.abs(car.y - y)
            }
            
            // Solo detener si está cerca Y el semáforo está adelante
            return distance < 80f
        }
        
        fun shouldStopCar(fordCar: FordCar): Boolean {
            if (isGreen) return false // Si está en verde, no detener
            
            // El auto de la Ford también se mueve hacia la izquierda cuando está en la carretera
            if (fordCar.movingDown) return false // Si está bajando, no verificar semáforos
            
            // Solo detener si el semáforo está ADELANTE del auto de la Ford
            val isTrafficLightAhead = fordCar.x > x
            
            if (!isTrafficLightAhead) return false // Si ya pasó el semáforo, no detener
            
            val distance = if (isVertical) {
                kotlin.math.abs(fordCar.x - x)
            } else {
                kotlin.math.abs(fordCar.y - y)
            }
            
            // Solo detener si está cerca Y el semáforo está adelante
            return distance < 80f
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
    }    private fun updateTrafficLights() {
        trafficLightCycle = (trafficLightCycle + 1) % 4 // Ciclo de 4 fases para más variación
        
        trafficLights.forEachIndexed { index, light ->
            // Alternar los semáforos de manera más visible
            when (trafficLightCycle) {
                0 -> light.isGreen = (index == 0) // Solo el primer semáforo en verde
                1 -> light.isGreen = true         // Todos en verde
                2 -> light.isGreen = (index == 1) // Solo el segundo semáforo en verde
                3 -> light.isGreen = false        // Todos en rojo
            }
        }
        
        Log.d("TrafficLight", "Updated lights - Cycle: $trafficLightCycle")
        Log.d("TrafficLight", "Light states: ${trafficLights.mapIndexed { index, light -> 
            "Light $index at (${light.x.toInt()}, ${light.y.toInt()}): ${if (light.isGreen) "GREEN" else "RED"}" 
        }}")
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
    
    // Método para resetear el estado del easter egg al entrar al mapa
    private fun resetEasterEggState() {
        easterEggTriggered = false
        timeAtEasterEggPosition = 0L
        isAtEasterEggPosition = false
        fordCarAnimation = null
        showingEasterEggMessage = false
        Log.d(TAG, "Easter egg state reset para nueva entrada al mapa")
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
        }    }    // Sistema de clima dinámico 
    private enum class WeatherState { SUNNY, CLOUDY, RAINY, NIGHT }
    private var currentWeather = WeatherState.SUNNY
    private val rainDrops = mutableListOf<RainDrop>()
    private val WEATHER_CHANGE_INTERVAL = 30000L
    private var weatherAlpha = 0f
    
    // Easter Egg del auto de la Ford
    private var easterEggTriggered = false
    private var timeAtEasterEggPosition = 0L
    private var isAtEasterEggPosition = false
    private val EASTER_EGG_POSITION = Pair(7, 22)
    private val EASTER_EGG_TIME_REQUIRED = 5000L // 5 segundos
    private var fordCarAnimation: FordCar? = null
    private var showingEasterEggMessage = false
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
    }    // Clase para el auto de la Ford del easter egg
    inner class FordCar(
        var x: Float,
        var y: Float
    ) {
        val width = 60f
        val height = 30f
        val speed = 3f // Velocidad más lenta
        var completed = false
        val rect = RectF(x, y, x + width, y + height)
          // Estados del movimiento
        var movingDown = true
        private val targetRoadY: Float
        
        init {
            // Calcular la Y de la carretera (70% del mapa hacia abajo)
            val mapBitmap = mapView.mapState.backgroundBitmap
            targetRoadY = if (mapBitmap != null) {
                mapBitmap.height * 0.75f // Ir hacia el área de la carretera
            } else {
                y + 100f
            }
        }
        
        fun update() {
            if (completed) return
            
            if (movingDown) {
                // Fase 1: Moverse hacia abajo hasta llegar a la carretera
                y += speed
                
                // Verificar si llegó a la carretera
                if (y >= targetRoadY) {
                    movingDown = false
                    y = targetRoadY // Ajustar posición exacta
                }
            } else {
                // Fase 2: Moverse hacia la izquierda como los demás autos
                x -= speed
                
                // Obtener dimensiones del mapa
                val mapBitmap = mapView.mapState.backgroundBitmap
                if (mapBitmap != null) {
                    // Si el auto sale de la pantalla por la izquierda, marcarlo como completado
                    if (x < -width) {
                        completed = true
                        // Quitar de la lista después de un tiempo
                        handler.postDelayed({
                            fordCarAnimation = null
                        }, 1000L)
                    }
                }
            }
            
            // Actualizar rectángulo
            rect.left = x
            rect.right = x + width
            rect.top = y
            rect.bottom = y + height
        }
    }
      private val weatherChangeRunnable = object : Runnable {
        override fun run() {
            changeWeather()
            handler.postDelayed(this, WEATHER_CHANGE_INTERVAL)
        }
    }
    
    // Método para mostrar el mensaje del easter egg
    private fun showEasterEggMessage() {
        if (showingEasterEggMessage) return
        showingEasterEggMessage = true
          runOnUiThread {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("easter egg xd")
            builder.setMessage("te atropello el que se robo el auto de la ford!!")
            builder.setPositiveButton("ok") { dialog, _ -> 
                dialog.dismiss()
                showingEasterEggMessage = false
                fordCarAnimation = null
            }
            builder.setCancelable(false)
            builder.show()
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
                // Reset easter egg state for new map entry
                resetEasterEggState()
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
        // espera a que cargue el mapa
        mapView.post {
            val mapBitmap = mapView.mapState.backgroundBitmap ?: return@post            
            // crea 5 carros
            val mapWidth = mapBitmap.width.toFloat()
            val mapHeight = mapBitmap.height.toFloat()
            // solo usa el 30% inferior del mapa para las carreteras
            val minY = mapHeight * 0.7f
            val maxY = mapHeight * 0.95f
            // limpiar la lista de carros
            carList.clear()
            // diferenciar los colores de los carros
            for (i in 0 until 5) {
                val speed = 2f + (Math.random().toFloat() * 4f) // velcidad aleatoria entre 2 y 6
                // Distribuir los carros en 3 carriles
                val laneCount = 3
                val laneHeight = (maxY - minY) / laneCount
                val lane = i % laneCount
                val y = minY + (lane * laneHeight) + (Math.random().toFloat() * (laneHeight * 0.6f))
                // distribuir los carros en el ancho del mapa
                val x = mapWidth * (i / 5f) + (Math.random().toFloat() * mapWidth * 0.2f)
                val color = carColors[i % carColors.size]
                carList.add(Car(x, y, 40f, 20f, speed, color))
            }            // Configurar el renderer de carros
            mapView.setCarRenderer(object : MapView.CarRenderer {
                override fun drawCars(canvas: Canvas) {
                    val paint = Paint().apply {
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    
                    // dibujar cada carro
                    for (car in carList) {
                        paint.color = car.color
                        canvas.drawRect(car.rect, paint)
                        
                        // bordear carro
                        paint.color = Color.BLACK
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 2f
                        canvas.drawRect(car.rect, paint)
                        paint.style = Paint.Style.FILL
                    }
                    
                    // Dibujar el auto de la Ford del easter egg
                    fordCarAnimation?.let { fordCar ->
                        // Auto de la Ford especial - más grande y distintivo
                        paint.color = Color.parseColor("#1E3A8A") // Azul Ford
                        canvas.drawRect(fordCar.rect, paint)
                        
                        // Borde especial para el auto de la Ford
                        paint.color = Color.WHITE
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 4f
                        canvas.drawRect(fordCar.rect, paint)
                        paint.style = Paint.Style.FILL
                        
                        // Agregar logo "FORD" en el auto
                        paint.color = Color.WHITE
                        paint.textSize = 12f
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(
                            "FORD", 
                            fordCar.rect.centerX(), 
                            fordCar.rect.centerY() + 4f, 
                            paint
                        )
                        
                        // Efectos de velocidad (líneas de movimiento)
                        paint.color = Color.LTGRAY
                        paint.strokeWidth = 2f
                        paint.style = Paint.Style.STROKE
                        for (i in 1..3) {
                            canvas.drawLine(
                                fordCar.x - (i * 15f),
                                fordCar.y + (i * 3f),
                                fordCar.x - (i * 10f),
                                fordCar.y + (i * 3f),
                                paint
                            )
                        }
                        paint.style = Paint.Style.FILL
                    }
                    
                    // semaforos
                    for (light in trafficLights) {
                        paint.color = if (light.isGreen) Color.GREEN else Color.RED
                        canvas.drawRect(light.rect, paint)
                        
                        // borde al semáforo
                        paint.color = Color.BLACK
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 3f
                        canvas.drawRect(light.rect, paint)
                        paint.style = Paint.Style.FILL
                        
                        // dibujar la barra de espera
                        paint.color = Color.GRAY
                        canvas.drawRect(
                            light.rect.centerX() - 3f,
                            light.rect.bottom,
                            light.rect.centerX() + 3f,
                            light.rect.bottom + 25f,
                            paint
                        )
                    }
                    
                    // efectos del clima
                    drawWeatherEffects(canvas, paint)
                }
                
                private fun drawWeatherEffects(canvas: Canvas, paint: Paint) {
                    when (currentWeather) {
                        WeatherState.RAINY -> {
                            // dibujar gotas de lluvia
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
                            // oscurecer la pantalla
                            paint.color = Color.argb((weatherAlpha * 255).toInt(), 0, 0, 100)
                            paint.style = Paint.Style.FILL
                            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
                        }
                        WeatherState.CLOUDY -> {
                            // efecto nublado
                            paint.color = Color.argb((weatherAlpha * 100).toInt(), 128, 128, 128)
                            paint.style = Paint.Style.FILL
                            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
                        }
                        WeatherState.SUNNY -> {
                            // sin efectos adicionales
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
        // actualizar cada carro
        for (car in carList) {
            // verificar semáforos antes de actualizar
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
          // Actualizar el auto de la Ford del easter egg
        fordCarAnimation?.let { fordCar ->
            // Solo verificar semáforos si ya está en la carretera (moviendo hacia la izquierda)
            if (!fordCar.movingDown) {
                var shouldStop = false
                for (light in trafficLights) {
                    if (light.shouldStopCar(fordCar)) {
                        shouldStop = true
                        break
                    }
                }
                
                if (!shouldStop) {
                    fordCar.update()
                }
            } else {
                // Si está bajando, actualizar sin verificar semáforos
                fordCar.update()
            }
        }
        
        // hacer que no choquen los carros entre sí
        preventCollisions()
        
        // verificar colisiones con el jugador
        checkPlayerCarCollisions()
        
        // efectos del clima
        updateWeather()
        
        // volver a dibujar el mapa
        mapView.invalidate()
    }

    private fun checkPlayerCarCollisions() {
        // posicion del jugador en el mapa
        val playerPosition = gameState.playerPosition
        val playerRect = getPlayerRect(playerPosition)
        
        if (playerRect != null) {
            // verificar colisiones con los carros
            for (car in carList) {
                if (RectF.intersects(car.rect, playerRect)) {
                    // dialogo de colision
                    showCarCollisionDialog(car)
                    
                    // efecto de colisión con el jugador
                    val newPosition = Pair(
                        playerPosition.first - 1,
                        playerPosition.second - 1
                    )
                    updatePlayerPosition(newPosition)
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
    }    private fun preventCollisions() {
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
          // Verificar colisiones entre el auto de la Ford y los demás autos (solo cuando esté en la carretera)
        fordCarAnimation?.let { fordCar ->
            if (!fordCar.movingDown) { // Solo verificar colisiones cuando esté en la carretera
                for (car in carList) {
                    // Verificar si están en el mismo carril
                    if (Math.abs(fordCar.y - car.y) < fordCar.height * 1.2f) {
                        // Verificar si el auto normal está delante del auto de la Ford
                        if (car.x < fordCar.x && fordCar.x - (car.x + car.width) < fordCar.width * 1.5f) {
                            // El auto de la Ford se ralentiza para evitar chocar
                            fordCar.x += fordCar.speed * 0.5f
                            // Actualizar rectángulo del auto de la Ford
                            fordCar.rect.left = fordCar.x
                            fordCar.rect.right = fordCar.x + fordCar.width
                            fordCar.rect.top = fordCar.y
                            fordCar.rect.bottom = fordCar.y + fordCar.height
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
    }    // Método para manejar la pulsación del botón A
    private fun handleButtonAPress() {
        val position = gameState.playerPosition
        
        // Verificar easter egg PRIMERO
        if (position == EASTER_EGG_POSITION && !easterEggTriggered && isAtEasterEggPosition) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - timeAtEasterEggPosition >= EASTER_EGG_TIME_REQUIRED) {
                triggerEasterEgg()
                return
            }
        }
        
        // Continuar con puntos de interés normales
        when {
            // Puntos de interés principales
            position.first == 35 && position.second == 5 -> {
                showInfoDialog(
                    "Metro CDMX",
                    "Línea 6 - Estación Instituto del Petróleo\n\n" +
                    "🚇 Horario: 5:00 - 24:00\n" +
                    "Tarifa: $5.00 MXN\n" +
                    "Conexiones: Línea 6 completa",
                    "https://www.metro.cdmx.gob.mx/"
                )
            }
            position.first == 31 && position.second == 27 -> {
                showInfoDialog(
                    "Trolebús",
                    "Línea K - Estación Politécnico\n\n" +
                    "🚌 Horario: 5:30 - 23:30\n" +
                    "Tarifa: $4.00 MXN\n" +
                    "Ruta: Politécnico - Metro Indios Verdes",
                    "https://www.ste.cdmx.gob.mx/trolebus"
                )
            }
            position.first == 17 && position.second == 22 -> {
                showInfoDialog(
                    "Ford Lindavista",
                    "Agencia Automotriz Ford\n\n" +
                    "Horario: 9:00 - 18:00\n" +
                    "Servicios: Ventas y mantenimiento\n" +
                    "Atención al cliente disponible",
                    "https://www.fordmylsa.mx/"
                )
            }
            // Nuevos puntos de interés
            position.first == 28 && position.second == 8 -> {
                showInfoDialog(
                    "Farmacia San Pablo",
                    "Farmacia 24 horas\n\n" +
                    "Horario: 24/7\n" +
                    "Servicios: Medicamentos, consultas médicas\n" +
                    "Acepta tarjetas y efectivo"
                )
            }
            position.first == 35 && position.second == 15 -> {
                showInfoDialog(
                    "Cajero Automático (pq no?)",
                    "BBVA Bancomer\n\n" +
                    "Disponible 24/7\n" +
                    "Retiros, consultas, depósitos\n" +
                    "Sin comisión para clientes BBVA"
                )
            }
            position.first == 25 && position.second == 30 -> {
                showInfoDialog(
                    "Restaurante El Buen Sazón (inventado)",
                    "Comida mexicana tradicional\n\n" +
                    "Horario: 8:00 - 22:00\n" +
                    "Especialidad: Tacos y quesadillas\n" +
                    "Precios accesibles"
                )
            }
            position.first == 8 && position.second == 12 -> {
                showInfoDialog(
                    "OXXO (más a la derecha)",
                    "Tienda de conveniencia\n\n" +
                    "Horario: 24/7\n" +
                    "Productos: Comida, bebidas, servicios\n" +
                    "Pago de servicios disponible"
                )
            }
            position.first == 12 && position.second == 25 -> {
                showInfoDialog(
                    "Parada de Autobús",
                    "Transporte público urbano\n\n" +
                    "Rutas: 1, 15, 42, 108\n" +
                    "Cada 10-15 minutos\n" +
                    "Tarifa: $5.50 MXN"
                )
            }
            position.first == 30 && position.second == 18 -> {
                showInfoDialog(
                    "Plaza Lindavista (ya se que no existe aqui xd)",
                    "Centro comercial\n\n" +
                    "Horario: 10:00 - 22:00\n" +
                    "Tiendas: Ropa, electrónicos, comida\n" +
                    "Área de comidas en planta alta"
                )
            }
        }
    }
      // Método para activar el easter egg
    private fun triggerEasterEgg() {
        if (easterEggTriggered) return
        
        easterEggTriggered = true
        Log.d(TAG, "auto de la Ford apareciendo...")
        
        // Obtener coordenadas de la posición (7, 20) en píxeles
        val mapBitmap = mapView.mapState.backgroundBitmap ?: return
        val cellWidth = mapBitmap.width / MapMatrixProvider.MAP_WIDTH.toFloat()
        val cellHeight = mapBitmap.height / MapMatrixProvider.MAP_HEIGHT.toFloat()
        
        // Posición inicial del auto de la Ford en (7, 20)
        val startX = 7 * cellWidth
        val startY = 20 * cellHeight
        
        // Crear el auto de la Ford en la coordenada (7, 20)
        fordCarAnimation = FordCar(startX, startY)
          // Mostrar mensaje de aviso
        Toast.makeText(this, "Se robaron un auto de la ford!", Toast.LENGTH_SHORT).show()
          // Programar el mensaje del atropello después de 3 segundos
        handler.postDelayed({
            if (!showingEasterEggMessage) {
                showEasterEggMessage()
            }
        }, 3000L)
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
    }    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        // Verificar easter egg position
        checkEasterEggPosition(position)
          // Definir mensajes específicos para cada punto de interés
        val message = when {
            position.first == 35 && position.second == 5 -> "Presiona A para ver información del Metro"
            position.first == 31 && position.second == 27 -> "Presiona A para ver información del Trolebús"
            position.first == 17 && position.second == 22 -> "Presiona A para ver información de Ford"
            position.first == 28 && position.second == 8 -> "Presiona A para ver información de la Farmacia"
            position.first == 35 && position.second == 15 -> "Presiona A para usar el Cajero Automático"
            position.first == 25 && position.second == 30 -> "Presiona A para ver menú del Restaurante"
            position.first == 8 && position.second == 12 -> "Presiona A para ver servicios de OXXO"
            position.first == 12 && position.second == 25 -> "Presiona A para ver horarios de Autobús"
            position.first == 30 && position.second == 18 -> "Presiona A para ver tiendas de la Plaza"
            // Easter egg hint
            position == EASTER_EGG_POSITION && !easterEggTriggered -> {
                if (isAtEasterEggPosition) {
                    val timeElapsed = (System.currentTimeMillis() - timeAtEasterEggPosition) / 1000
                    val timeRemaining = 5 - timeElapsed
                    if (timeRemaining > 0) {
                        "quédate ${timeRemaining}s más y presiona A..."
                    } else {
                        "Presiona A!"
                    }
                } else null
            }
            else -> null
        }
        
        message?.let {
            runOnUiThread {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Método para verificar la posición del easter egg
    private fun checkEasterEggPosition(position: Pair<Int, Int>) {
        if (easterEggTriggered) return
        
        if (position == EASTER_EGG_POSITION) {
            if (!isAtEasterEggPosition) {                // Jugador acaba de llegar a la posición
                isAtEasterEggPosition = true
                timeAtEasterEggPosition = System.currentTimeMillis()
                Log.d(TAG, "Jugador llegó a la posición del easter egg: $position")
                Toast.makeText(this, "Lugar extraño...", Toast.LENGTH_LONG).show()
            }
        } else {
            if (isAtEasterEggPosition) {
                // Jugador se movió de la posición
                isAtEasterEggPosition = false
                timeAtEasterEggPosition = 0L
                Log.d(TAG, "Jugador se movió de la posición del easter egg")
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
            )        }
        // Restart car animation
        startCarAnimation()
        // Restart traffic light cycle
        startTrafficLightCycle()
        // Restart weather system
        startWeatherSystem()
    }override fun onPause() {
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