package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
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
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.Zacatenco
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.esime.buildings.Edificio1Activity
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.esime.buildings.Edificio2Activity
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.esime.buildings.Edificio4Activity
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.esime.buildings.Edificio5Activity

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

    // Mapa de activities por número de edificio
    private val buildingActivityMap = mapOf(
        1 to Edificio1Activity::class.java,
        2 to Edificio2Activity::class.java,
        4 to Edificio4Activity::class.java,
        5 to Edificio5Activity::class.java
    )

    // 🔴 POSICIONES FIJAS de los edificios
    private val buildingLocations = mapOf(
        1 to Pair(8, 30),   // Edificio 1
        2 to Pair(8, 24),   // Edificio 2
        3 to Pair(8, 17),   // Edificio 3 - INTERACTIVO
        4 to Pair(8, 11),   // Edificio 4
        5 to Pair(8, 5)     // Edificio 5
    )

    // 🔴 ÁREAS DE COLISIÓN - Rectángulos grandes bloqueados
    private val collisionAreas = mutableListOf<Rect>()

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

                // 🔴 CONFIGURAR COLISIONES PRIMERO
                setupCollisionAreas()

                // 🔴 CONFIGURAR MARCADORES DE EDIFICIOS FIJOS
                setupBuildingMarkers()

                // 🔴 INICIAR ACTUALIZACIONES CONSTANTES PARA POSICIONES FIJAS
                startFixedPositionUpdates()

                // Configurar colores de jugadores
                configureRedPlayers()

                // Inicializar managers (CON COLISIONES)
                initializeManagers()

                // Configurar listeners de botones
                setupButtonListeners()

                Log.d("Esime", "Mapa ESIME inicializado: $normalizedMap")
                Log.d("Esime", "Áreas de colisión: ${collisionAreas.size}")
            }
        } catch (e: Exception) {
            Log.e("Esime", "Error en onCreate: ${e.message}")
            Toast.makeText(this, "Error inicializando ESIME.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // 🔴 CONFIGURAR ÁREAS DE COLISIÓN PARA BLOQUEAR EDIFICIOS
    private fun setupCollisionAreas() {
        try {
            collisionAreas.clear()

            // =====================================================================
            // LAYOUT DEL MAPA ESIME
            // Pasillos horizontales en Y: 5, 8, 11, 14, 17, 21, 24, 27, 30, 33
            // Pasillos verticales en X:  15, 18, 30
            // Edificios en columna X=8: (8,30), (8,24), (8,17), (8,11), (8,5)
            // Entrada de cada edificio: X=7-8 en la fila Y del edificio
            // Interior (fondo) de cada edificio: X=9-14 (bloqueado)
            // =====================================================================

            // -------------------------------------------------------
            // EDIFICIO 1  —  posición base (8, 30)
            // -------------------------------------------------------
            collisionAreas.add(Rect(7, 28, 14, 29))  // Pared superior (2 filas)
            collisionAreas.add(Rect(7, 31, 14, 32))  // Pared inferior (2 filas)
            collisionAreas.add(Rect(9, 30, 14, 30))  // Interior fondo: bloquea X=9-14 en Y=30
            collisionAreas.add(Rect(16, 28, 17, 29)) // Separador corredor vertical derecho

            // -------------------------------------------------------
            // EDIFICIO 2  —  posición base (8, 24)
            // -------------------------------------------------------
            collisionAreas.add(Rect(7, 22, 14, 23))  // Pared superior (2 filas)
            collisionAreas.add(Rect(7, 25, 14, 26))  // Pared inferior (2 filas)
            collisionAreas.add(Rect(9, 24, 14, 24))  // Interior fondo: bloquea X=9-14 en Y=24
            collisionAreas.add(Rect(16, 22, 17, 23)) // Separador corredor vertical derecho

            // -------------------------------------------------------
            // EDIFICIO 3  —  posición base (8, 17)
            // -------------------------------------------------------
            collisionAreas.add(Rect(7, 15, 14, 16))  // Pared superior (2 filas)
            collisionAreas.add(Rect(7, 18, 14, 19))  // Pared inferior (2 filas)
            collisionAreas.add(Rect(9, 17, 14, 17))  // Interior fondo: bloquea X=9-14 en Y=17
            collisionAreas.add(Rect(16, 15, 17, 16)) // Separador corredor vertical derecho
            // Relleno: entre pared inferior de Ed3 (Y=18-19) y corredor (Y=21) hay Y=20 abierto
            collisionAreas.add(Rect(7, 20, 14, 20))  // Cierra hueco Y=20 en zona de edificios

            // -------------------------------------------------------
            // EDIFICIO 4  —  posición base (8, 11)
            // -------------------------------------------------------
            collisionAreas.add(Rect(7, 9, 14, 10))   // Pared superior (2 filas)
            collisionAreas.add(Rect(7, 12, 14, 13))  // Pared inferior (2 filas)
            collisionAreas.add(Rect(9, 11, 14, 11))  // Interior fondo: bloquea X=9-14 en Y=11
            collisionAreas.add(Rect(16, 9, 17, 10))  // Separador corredor vertical derecho

            // -------------------------------------------------------
            // EDIFICIO 5  —  posición base (8, 5)
            // (La zona inaccesible norte Rect(7,1,38,4) ya cubre la pared superior)
            // -------------------------------------------------------
            collisionAreas.add(Rect(7, 6, 14, 7))    // Pared inferior (2 filas)
            collisionAreas.add(Rect(9, 5, 14, 5))    // Interior fondo: bloquea X=9-14 en Y=5
            collisionAreas.add(Rect(16, 3, 17, 4))   // Separador corredor vertical derecho

            // -------------------------------------------------------
            // ZONAS NATURALES / INACCESIBLES
            // -------------------------------------------------------
            collisionAreas.add(Rect(7, 34, 38, 38))  // Pasto sur
            collisionAreas.add(Rect(32, 29, 38, 38)) // Pasto esquina sureste
            collisionAreas.add(Rect(24, 6, 29, 18))  // Estructura central del campus
            collisionAreas.add(Rect(7, 1, 38, 4))    // Zona inaccesible norte

            // -------------------------------------------------------
            // BORDES DEL MAPA
            // -------------------------------------------------------
            collisionAreas.add(Rect(0, 0, 0, 40))    // Borde izquierdo
            collisionAreas.add(Rect(40, 0, 40, 40))  // Borde derecho
            collisionAreas.add(Rect(0, 0, 40, 0))    // Borde superior
            collisionAreas.add(Rect(0, 40, 40, 40))  // Borde inferior

            Log.d("ESIME_COLLISIONS", "✅ ${collisionAreas.size} áreas de colisión configuradas")
            showCollisionAreasDebug()

        } catch (e: Exception) {
            Log.e("ESIME_COLLISIONS", "Error configurando colisiones: ${e.message}")
        }
    }

    // 🔴 MOSTRAR INFORMACIÓN DE DEBUG SOBRE COLISIONES
    private fun showCollisionAreasDebug() {
        val debugMessage = StringBuilder("🚫 ÁREAS DE COLISIÓN:\n")
        collisionAreas.forEachIndexed { index, rect ->
            debugMessage.append("Area $index: [${rect.left},${rect.top}]->[${rect.right},${rect.bottom}]\n")
        }
        Log.d("ESIME_COLLISIONS", debugMessage.toString())

        Toast.makeText(this,
            "🏢 Todos los edificios son accesibles\n" +
                    "Acércate a cualquier edificio y presiona A",
            Toast.LENGTH_LONG
        ).show()
    }

    // 🔴🔴🔴 CONFIGURAR MARCADORES VISUALES DE EDIFICIOS FIJOS 🔴🔴🔴
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
            // Configurar marcadores de edificios usando PlayerManager
            setupBuildingMarkersWithPlayerManager()
        } catch (e: Exception) {
            Log.e("ESIME_MARKERS", "Error configurando marcadores: ${e.message}")
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
            debugMessage.append("Edificio $building: (${pos.first}, ${pos.second}) - ✅ INTERACTIVO\n")
        }

        Log.d("ESIME_DEBUG", debugMessage.toString())

        // Mostrar organización en Toast
        Toast.makeText(this,
            "🏢 Edificios en columna X=8\n" +
                    "✅ Todos los edificios son accesibles",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showBuildingCoordinates() {
        buildingLocations.forEach { (building, position) ->
            Log.d("ESIME_BUILDINGS", "🏢 Edificio $building -> POSICIÓN FIJA: (${position.first}, ${position.second})")
        }
    }

    // Configurar colores de jugadores
    private fun configureRedPlayers() {
        try {
            Log.d("ESIME_COLOR", "Configurando colores de jugadores")
            mapView.invalidate()
        } catch (e: Exception) {
            Log.e("ESIME_COLOR", "Error configurando colores: ${e.message}")
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

        // 🔴 INICIALIZAR MOVEMENT MANAGER CON COLISIONES
        movementManager = MovementManager(
            mapView = mapView
        ) { position -> updatePlayerPosition(position) }

        // 🔴 CONFIGURAR ÁREAS DE COLISIÓN EN EL MOVEMENT MANAGER
        setupMovementManagerWithCollisions()

        mapView.setMapTransitionListener(this)
        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(playerPosition)
    }

    // Configurar MovementManager con verificación básica de colisiones
    private fun setupMovementManagerWithCollisions() {
        try {
            Log.d("ESIME_COLLISIONS", "Configurando sistema de colisiones básico")
            // La verificación de colisiones se manejará en handleMovementWithCollisions
        } catch (e: Exception) {
            Log.e("ESIME_COLLISIONS", "Error configurando colisiones: ${e.message}")
        }
    }

    private fun setupButtonListeners() {
        // 🔴 MODIFICAR LOS LISTENERS PARA VERIFICAR COLISIONES
        btnNorth.setOnTouchListener { _, event ->
            handleMovementWithCollisions(event, 0, -1); true
        }
        btnSouth.setOnTouchListener { _, event ->
            handleMovementWithCollisions(event, 0, 1); true
        }
        btnEast.setOnTouchListener { _, event ->
            handleMovementWithCollisions(event, 1, 0); true
        }
        btnWest.setOnTouchListener { _, event ->
            handleMovementWithCollisions(event, -1, 0); true
        }

        buttonA.setOnClickListener {
            when {
                canChangeMap -> returnToZacatenco()
                canEnterBuilding -> enterBuilding(currentBuilding!!)
                else -> Toast.makeText(this, "No hay interacción disponible aquí", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            returnToZacatenco()
        }
    }

    // 🔴 NUEVO MÉTODO PARA MANEJAR MOVIMIENTO CON VERIFICACIÓN DE COLISIONES
    private fun handleMovementWithCollisions(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Verificar si el movimiento futuro causaría colisión
                    val newX = playerPosition.first + deltaX
                    val newY = playerPosition.second + deltaY
                    val newPosition = Pair(newX, newY)

                    if (isPositionBlocked(newPosition)) {
                        // 🔴 POSICIÓN BLOQUEADA - Mostrar mensaje y no mover
                        Toast.makeText(this, "🚫 Área bloqueada", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // 🔴 MOVIMIENTO PERMITIDO - Proceder normalmente
                    movementManager.handleMovement(event, deltaX, deltaY)
                }
                else -> {
                    movementManager.handleMovement(event, deltaX, deltaY)
                }
            }
        }
    }

    // 🔴 VERIFICAR SI UNA POSICIÓN ESTÁ BLOQUEADA
    private fun isPositionBlocked(position: Pair<Int, Int>): Boolean {
        val (x, y) = position

        // Verificar todas las áreas de colisión
        return collisionAreas.any { rect ->
            x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
        }
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                // 🔴 VERIFICAR COLISIÓN ANTES DE ACTUALIZAR
                if (isPositionBlocked(position)) {
                    Log.d("ESIME_COLLISIONS", "🚫 Colisión detectada en: $position")
                    Toast.makeText(this, "Movimiento bloqueado", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

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

        // Verificar si está en área bloqueada
        if (isPositionBlocked(position)) {
            Log.d("ESIME_POSITION", "🚫 POSICIÓN BLOQUEADA: ($x, $y)")
        }

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
        canEnterBuilding = (buildingNumber != null)

        if (buildingNumber != null) {
            runOnUiThread {
                Toast.makeText(this, "Presiona A para entrar al Edificio $buildingNumber", Toast.LENGTH_SHORT).show()
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

    private fun enterBuilding(buildingNumber: Int) {
        Log.d("Esime", "Entrando al Edificio $buildingNumber")

        val activityClass: Class<*> = when (buildingNumber) {
            1 -> Edificio1Activity::class.java
            2 -> Edificio2Activity::class.java
            3 -> ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.locations.esime.buildings.Edificio3Activity::class.java
            4 -> Edificio4Activity::class.java
            5 -> Edificio5Activity::class.java
            else -> {
                Toast.makeText(this, "Edificio $buildingNumber: No disponible", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val intent = Intent(this, activityClass).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
        }
        startActivity(intent)
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