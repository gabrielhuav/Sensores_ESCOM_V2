package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.locations.esime.buildings.Edificio3Activity

class Esime : AppCompatActivity(), OnlineServerManager.WebSocketListener, MapView.MapTransitionListener {

    private lateinit var movementManager: MovementManager
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var mapView: MapView

    // Botones
    private lateinit var btnNorth: MaterialButton
    private lateinit var btnSouth: MaterialButton
    private lateinit var btnEast: MaterialButton
    private lateinit var btnWest: MaterialButton
    private lateinit var buttonA: MaterialButton
    private lateinit var btnBack: MaterialButton

    private lateinit var playerName: String

    private var playerPosition: Pair<Int, Int> = Pair(2, 2)
    private var isServer: Boolean = false
    private var canChangeMap = false
    private var canEnterBuilding = false
    private var currentBuilding: Int? = null

    // 🔴 CORREGIDO: Cada edificio en posición ÚNICA (sin duplicados)
    private val buildingLocations = mapOf(
        1 to Pair(8, 30),   // Edificio 1 - MÁS ARRIBA
        2 to Pair(8, 24),   // Edificio 2
        3 to Pair(8, 17),   // Edificio 3 - INTERACTIVO (posición única)
        4 to Pair(8, 11),   // Edificio 4
        5 to Pair(8, 5)    // Edificio 5 - MÁS ABAJO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_esime)

        try {
            // Inicializar vistas
            initializeViews()

            // Inicializar mapView
            mapView = MapView(
                context = this,
                mapResourceId = R.drawable.esimemapa
            )
            findViewById<FrameLayout>(R.id.map_container).addView(mapView)

            // Obtener datos del Intent
            playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"
            isServer = intent.getBooleanExtra("IS_SERVER", false)
            playerPosition = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int> ?: Pair(2, 2)

            // Esperar a que el mapView esté listo
            mapView.post {
                // Configurar el mapa
                val normalizedMap = MapMatrixProvider.Companion.normalizeMapName(MapMatrixProvider.Companion.MAP_ESIME)
                mapView.setCurrentMap(normalizedMap, R.drawable.esimemapa)

                // Configurar playerManager
                mapView.playerManager.apply {
                    setCurrentMap(normalizedMap)
                    localPlayerId = playerName
                    updateLocalPlayerPosition(playerPosition)
                }

                // 🔴🔴🔴 CONFIGURAR MARCADORES DE EDIFICIOS FIJOS 🔴🔴🔴
                setupBuildingMarkers()

                // 🔴 INICIAR ACTUALIZACIONES CONSTANTES PARA POSICIONES FIJAS
                startFixedPositionUpdates()

                // Configurar colores de jugadores
                configureRedPlayers()

                // Inicializar managers
                initializeManagers()

                // Configurar listeners de botones
                setupButtonListeners()

                Log.d("Esime", "Mapa ESIME inicializado: $normalizedMap")
            }
        } catch (e: Exception) {
            Log.e("Esime", "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error inicializando ESIME.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // 🔴🔴🔴 NUEVO: CONFIGURAR MARCADORES VISUALES DE EDIFICIOS FIJOS 🔴🔴🔴
    private fun setupBuildingMarkers() {
        try {
            // Usar reflexión para agregar marcadores FIJOS
            addFixedBuildingMarkersToMap()

            // Mostrar información de debug
            showBuildingLocationsDebug()

        } catch (e: Exception) {
            Log.e("ESIME_MARKERS", "Error configurando marcadores: ${e.message}")
            // Fallback: mostrar coordenadas por Toast
            showBuildingCoordinates()
        }
    }

    private fun addFixedBuildingMarkersToMap() {
        try {
            // 🔴 SOLUCIÓN: Usar un enfoque diferente para marcadores estáticos
            // Intentar acceder al método de dibujo personalizado
            setupStaticMarkersWithReflection()

            // También intentar con PlayerManager pero con posiciones FIJAS
            setupBuildingMarkersWithPlayerManager()

        } catch (e: Exception) {
            Log.e("ESIME_MARKERS", "No se pudieron agregar marcadores automáticos: ${e.message}")
        }
    }

    private fun setupStaticMarkersWithReflection() {
        try {
            // Intentar encontrar un método para agregar marcadores estáticos
            val mapViewClass = mapView.javaClass

            // Buscar métodos para agregar marcadores estáticos
            val possibleMethods = arrayOf(
                "addStaticMarker", "setStaticMarkers", "addBuildingMarker",
                "setBuildingLocations", "addFixedPoint"
            )

            for (methodName in possibleMethods) {
                try {
                    val method = mapViewClass.getDeclaredMethod(methodName, Map::class.java)
                    method.isAccessible = true
                    method.invoke(mapView, buildingLocations)
                    Log.d("ESIME_MARKERS", "✅ Método encontrado: $methodName")
                    break
                } catch (e: NoSuchMethodException) {
                    continue
                }
            }

        } catch (e: Exception) {
            Log.d("ESIME_MARKERS", "No se encontraron métodos para marcadores estáticos")
        }
    }

    private fun setupBuildingMarkersWithPlayerManager() {
        try {
            // 🔴 SOLUCIÓN: Actualizar constantemente las posiciones FIJAS
            buildingLocations.forEach { (buildingNumber, position) ->
                val buildingPlayerId = "EDIFICIO_$buildingNumber"

                // Actualizar la posición FIJA cada vez
                mapView.playerManager.updateRemotePlayerPosition(
                    buildingPlayerId,
                    position, // 🔴 SIEMPRE la misma posición FIJA
                    MapMatrixProvider.Companion.MAP_ESIME
                )

                Log.d("ESIME_MARKERS", "Marcador FIJO edificio $buildingNumber en: $position")
            }

        } catch (e: Exception) {
            Log.e("ESIME_MARKERS", "Error con PlayerManager: ${e.message}")
        }
    }

    // 🔴 NUEVO: Mantener posiciones FIJAS actualizando constantemente
    private fun startFixedPositionUpdates() {
        // Actualizar cada 2 segundos para mantener posiciones fijas
        android.os.Handler(mainLooper).postDelayed({
            try {
                buildingLocations.forEach { (buildingNumber, position) ->
                    val buildingPlayerId = "EDIFICIO_$buildingNumber"
                    mapView.playerManager.updateRemotePlayerPosition(
                        buildingPlayerId,
                        position, // 🔴 SIEMPRE la posición FIJA original
                        MapMatrixProvider.Companion.MAP_ESIME
                    )
                }
                mapView.invalidate()

                // 🔴 REPETIR PARA MANTENER POSICIONES FIJAS
                startFixedPositionUpdates()
            } catch (e: Exception) {
                Log.e("ESIME_FIXED", "Error en actualización fija: ${e.message}")
            }
        }, 2000)
    }

    private fun showBuildingLocationsDebug() {
        val debugMessage = StringBuilder("🏢 EDIFICIOS (FIJOS en X=8):\n")
        buildingLocations.entries.sortedByDescending { it.value.second }.forEach { (building, pos) ->
            val status = if (building == 3) "✅ INTERACTIVO" else "❌ No disponible"
            debugMessage.append("Edificio $building: (${pos.first}, ${pos.second}) - $status\n")
        }

        Log.d("ESIME_DEBUG", debugMessage.toString())

        // Mostrar organización en Toast
        Toast.makeText(this,
            "🏢 Edificios en columna X=8\n" +
                    "⬆️ Y=35 (Edif 1)\n" +
                    "⬇️ Y=15 (Edif 5)\n" +
                    "Solo Edificio 3 es interactivo",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showBuildingCoordinates() {
        buildingLocations.forEach { (building, position) ->
            Log.d("ESIME_BUILDINGS", "🏢 Edificio $building -> POSICIÓN FIJA: (${position.first}, ${position.second})")
        }
    }

    // 🔴🔴🔴 MÉTODO PARA CONFIGURAR COLORES ROJOS 🔴🔴🔴
    private fun configureRedPlayers() {
        try {
            val mapViewClass = mapView.javaClass

            // Intentar encontrar y modificar el paint de jugadores
            val possiblePaintFields = arrayOf(
                "playerPaint", "remotePlayerPaint", "mPlayerPaint",
                "playerCirclePaint", "defaultPlayerPaint"
            )

            for (fieldName in possiblePaintFields) {
                try {
                    val field = mapViewClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    val paint = field.get(mapView) as? android.graphics.Paint
                    paint?.let {
                        it.color = Color.RED
                        it.strokeWidth = 4f
                        it.isAntiAlias = true
                        Log.d("ESIME_COLOR", "✅ Paint modificado: $fieldName -> ROJO")
                    }
                } catch (e: NoSuchFieldException) {
                    continue
                }
            }

            // Intentar modificar el radio de los jugadores
            val possibleRadiusFields = arrayOf(
                "playerRadius", "mPlayerRadius", "defaultPlayerRadius",
                "circleRadius", "playerCircleRadius"
            )

            for (fieldName in possibleRadiusFields) {
                try {
                    val field = mapViewClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    field.setFloat(mapView, 25f)
                    Log.d("ESIME_COLOR", "✅ Radio modificado: $fieldName -> 25f")
                } catch (e: NoSuchFieldException) {
                    continue
                }
            }

            mapView.invalidate()

        } catch (e: Exception) {
            Log.e("ESIME_COLOR", "❌ No se pudo configurar colores: ${e.message}")
            tryFallbackColorConfiguration()
        }
    }

    // 🔴 Método fallback si la reflexión no funciona
    private fun tryFallbackColorConfiguration() {
        try {
            val playerManagerField = mapView.javaClass.getDeclaredField("playerManager")
            playerManagerField.isAccessible = true
            val playerManager = playerManagerField.get(mapView)

            val playerManagerClass = playerManager.javaClass
            val colorFields = arrayOf("remotePlayerColor", "defaultPlayerColor", "playerColor")

            for (fieldName in colorFields) {
                try {
                    val field = playerManagerClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    field.setInt(playerManager, Color.RED)
                    Log.d("ESIME_COLOR", "✅ Color en PlayerManager: $fieldName -> ROJO")
                } catch (e: NoSuchFieldException) {
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e("ESIME_COLOR", "❌ Fallback también falló: ${e.message}")
        }
    }

    private fun initializeViews() {
        btnNorth = findViewById(R.id.btnNorth)
        btnSouth = findViewById(R.id.btnSouth)
        btnEast = findViewById(R.id.btnEast)
        btnWest = findViewById(R.id.btnWest)
        buttonA = findViewById(R.id.buttonA)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun initializeManagers() {
        val onlineServerManager = OnlineServerManager.Companion.getInstance(this).apply {
            setListener(this@Esime)
        }

        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        movementManager = MovementManager(
            mapView = mapView
        ) { position -> updatePlayerPosition(position) }

        mapView.setMapTransitionListener(this)
        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(playerPosition)
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> handleMovement(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovement(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovement(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovement(event, -1, 0); true }

        buttonA.setOnClickListener {
            when {
                canChangeMap -> returnToZacatenco()
                canEnterBuilding -> enterBuilding3()
                currentBuilding != null -> showBuildingNotAvailable(currentBuilding!!)
                else -> Toast.makeText(this, "No hay interacción disponible aquí", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            returnToZacatenco()
        }
    }

    private fun handleMovement(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) {
            movementManager.handleMovement(event, deltaX, deltaY)
        }
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                playerPosition = position
                mapView.updateLocalPlayerPosition(position, forceCenter = true)
                mapView.invalidate()

                if (::serverConnectionManager.isInitialized) {
                    serverConnectionManager.sendUpdateMessage(playerName, position, MapMatrixProvider.Companion.MAP_ESIME)
                }

                checkPositionForMapChange(position)
                checkPositionForBuildingInteraction(position)

                // 🔴 NUEVO: Mostrar coordenadas actuales para debug
                showCurrentPositionDebug(position)

            } catch (e: Exception) {
                Log.e("Esime", "Error en updatePlayerPosition: ${e.message}")
            }
        }
    }

    // 🔴 NUEVO: Mostrar posición actual y edificios cercanos
    private fun showCurrentPositionDebug(position: Pair<Int, Int>) {
        val (x, y) = position

        // Encontrar edificio más cercano
        val closestBuilding = buildingLocations.minByOrNull { (_, buildingPos) ->
            Math.abs(buildingPos.first - x) + Math.abs(buildingPos.second - y)
        }

        closestBuilding?.let { (buildingNum, buildingPos) ->
            val distance = Math.abs(buildingPos.first - x) + Math.abs(buildingPos.second - y)
            if (distance <= 5) {
                Log.d("ESIME_POSITION", "📍 Posición actual: ($x, $y) - Cerca del Edificio $buildingNum (distancia: $distance)")
            }
        }
    }

    private fun checkPositionForMapChange(position: Pair<Int, Int>) {
        canChangeMap = (position.first == 5 && position.second == 35)

        if (canChangeMap) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para salir de ESIME", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPositionForBuildingInteraction(position: Pair<Int, Int>) {
        val buildingNumber = detectBuildingAtPosition(position)
        currentBuilding = buildingNumber
        canEnterBuilding = (buildingNumber == 3)

        if (buildingNumber != null) {
            runOnUiThread {
                if (buildingNumber == 3) {
                    Toast.makeText(this, "Presiona A para entrar al Edificio 3", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Edificio $buildingNumber - No disponible", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            canEnterBuilding = false
            currentBuilding = null
        }
    }

    private fun detectBuildingAtPosition(position: Pair<Int, Int>): Int? {
        val (x, y) = position

        // Usar las ubicaciones definidas en buildingLocations
        buildingLocations.forEach { (buildingNumber, buildingPos) ->
            if (x in (buildingPos.first - 2)..(buildingPos.first + 2) &&
                y in (buildingPos.second - 2)..(buildingPos.second + 2)) {
                return buildingNumber
            }
        }

        return null
    }

    private fun enterBuilding3() {
        val intent = Intent(this, Edificio3Activity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            putExtra("INITIAL_POSITION", Pair(2, 2))
            putExtra("PREVIOUS_POSITION", playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun showBuildingNotAvailable(buildingNumber: Int) {
        Toast.makeText(this, "Edificio $buildingNumber: No se ha creado base de edificio", Toast.LENGTH_LONG).show()
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.Companion.MAP_ZACATENCO -> {
                returnToZacatenco()
            }
            else -> {
                Log.d("Esime", "Mapa destino no reconocido: $targetMap")
            }
        }
    }

    private fun returnToZacatenco() {
        val intent = Intent(this, Zacatenco::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            putExtra("INITIAL_POSITION", Pair(8, 18))
            putExtra("PREVIOUS_POSITION", playerPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }
        startActivity(intent)
        finish()
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(message)
                when (jsonObject.getString("type")) {
                    "positions", "update" -> {
                        val playerId = jsonObject.getString("id")
                        if (playerId != playerName) {
                            val position = Pair(
                                jsonObject.getInt("x"),
                                jsonObject.getInt("y")
                            )
                            val map = jsonObject.getString("map")
                            mapView.updateRemotePlayerPosition(playerId, position, map)
                            mapView.invalidate()
                        }
                    }
                }
                mapView.invalidate()
            } catch (e: Exception) {
                Log.e("Esime", "Error procesando mensaje: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::movementManager.isInitialized) {
            movementManager.setPosition(playerPosition)
        }
        mapView?.invalidate()
    }

    override fun onPause() {
        super.onPause()
        if (::movementManager.isInitialized) {
            movementManager.stopMovement()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }
    }

    companion object {
        private const val TAG = "Esime"
    }
}