package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.esimios.EsimioController
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity
import android.view.View

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
    private lateinit var btnNightMode: MaterialButton

    private lateinit var nightOverlay: View

    private lateinit var playerName: String

    private var playerPosition: Pair<Int, Int> = Pair(2, 2)
    private var isServer: Boolean = false
    private var canChangeMap = false
    private var canEnterBuilding = false
    private var currentBuilding: Int? = null

    // Controlador de esimios
    private lateinit var esimioController: EsimioController
    private var esimioGameActive = false
    private var nightMode = false

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
                val normalizedMap = MapMatrixProvider.normalizeMapName(MapMatrixProvider.MAP_ESIME)
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

            // 🔴 DEFINIR RECTÁNGULOS GRANDES PARA BLOQUEAR ACCESO A EDIFICIOS
            collisionAreas.add(Rect(7, 28, 14 ,29))   // Edificio 1
            collisionAreas.add(Rect(16, 28, 17, 29))  // Cuadrado que deja pasillo
            collisionAreas.add(Rect(7, 31, 14, 32))   // Parte inferior

            collisionAreas.add(Rect(7, 22, 14, 23))   // Edificio 2
            collisionAreas.add(Rect(16, 22, 17, 23))  // Cuadrado que deja pasillo
            collisionAreas.add(Rect(7, 25, 14, 26))   // Parte inferior

            collisionAreas.add(Rect(7, 15, 14, 16))   // Edificio 3
            collisionAreas.add(Rect(16, 15, 17, 16))  // Cuadrado que deja pasillo
            collisionAreas.add(Rect(7, 18, 14, 19))   // Parte inferior

            collisionAreas.add(Rect(7, 9, 14, 10))    // Edificio 4
            collisionAreas.add(Rect(16, 9, 17, 10))   // Cuadrado que deja pasillo
            collisionAreas.add(Rect(7, 12, 14, 13))   // Parte inferior

            collisionAreas.add(Rect(7, 3, 14, 4))     // Edificio 5
            collisionAreas.add(Rect(16, 3, 17, 4))    // Cuadrado que deja pasillo
            collisionAreas.add(Rect(7, 6, 14, 7))

            // Pastos y áreas inaccesibles
            collisionAreas.add(Rect(7, 34, 38, 38))
            collisionAreas.add(Rect(32, 29, 38, 38))
            collisionAreas.add(Rect(24, 6, 29, 18))
            collisionAreas.add(Rect(7, 1, 38, 4))

            // BORDES DEL MAPA
            collisionAreas.add(Rect(0, 0, 0, 40))     // Borde izquierdo
            collisionAreas.add(Rect(40, 0, 40, 40))   // Borde derecho
            collisionAreas.add(Rect(0, 0, 40, 0))     // Borde superior
            collisionAreas.add(Rect(0, 40, 40, 40))   // Borde inferior

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

        //Toast.makeText(this,
          //  "🚫 Áreas bloqueadas configuradas\nSolo Edificio 3 es accesible",
            //Toast.LENGTH_LONG
        //).show()
    }

    // 🔴🔴🔴 CONFIGURAR MARCADORES VISUALES DE EDIFICIOS FIJOS 🔴🔴🔴
    private fun setupBuildingMarkers() {
        try {
            addFixedBuildingMarkersToMap()
            showBuildingLocationsDebug()
        } catch (e: Exception) {
            Log.e("ESIME_MARKERS", "Error configurando marcadores: ${e.message}")
            showBuildingCoordinates()
        }
    }

    private fun addFixedBuildingMarkersToMap() {
        try {
            setupBuildingMarkersWithPlayerManager()
        } catch (e: Exception) {
            Log.e("ESIME_MARKERS", "Error configurando marcadores: ${e.message}")
        }
    }

    private fun setupBuildingMarkersWithPlayerManager() {
        try {
            buildingLocations.forEach { (buildingNumber, position) ->
                val buildingPlayerId = "EDIFICIO_$buildingNumber"
                mapView.playerManager.updateRemotePlayerPosition(
                    buildingPlayerId,
                    position,
                    MapMatrixProvider.MAP_ESIME
                )
                Log.d("ESIME_MARKERS", "Marcador FIJO edificio $buildingNumber en: $position")
            }
        } catch (e: Exception) {
            Log.e("ESIME_MARKERS", "Error con PlayerManager: ${e.message}")
        }
    }

    // 🔴 NUEVO: Mantener posiciones FIJAS actualizando constantemente
    private fun startFixedPositionUpdates() {
        android.os.Handler(mainLooper).postDelayed({
            try {
                buildingLocations.forEach { (buildingNumber, position) ->
                    val buildingPlayerId = "EDIFICIO_$buildingNumber"
                    mapView.playerManager.updateRemotePlayerPosition(
                        buildingPlayerId,
                        position,
                        MapMatrixProvider.MAP_ESIME
                    )
                }
                mapView.invalidate()
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

        // Toast.makeText(this,
        //    "🏢 Edificios en columna X=8\n" +
        //          "⬆️ Y=30 (Edif 1) - BLOQUEADO\n" +
        //          "⬇️ Y=5 (Edif 5) - BLOQUEADO\n" +
        //          "Solo Edificio 3 es interactivo",
        //  Toast.LENGTH_LONG
        // ).show()
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
        btnNightMode = findViewById(R.id.btnNightMode)

        nightOverlay = findViewById(R.id.night_overlay)
    }

    private fun initializeManagers() {
        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@Esime)
        }

        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        movementManager = MovementManager(
            mapView = mapView
        ) { position -> updatePlayerPosition(position) }

        setupMovementManagerWithCollisions()
        mapView.setMapTransitionListener(this)
        mapView.playerManager.localPlayerId = playerName
        updatePlayerPosition(playerPosition)
    }

    private fun setupMovementManagerWithCollisions() {
        try {
            Log.d("ESIME_COLLISIONS", "Configurando sistema de colisiones básico")
        } catch (e: Exception) {
            Log.e("ESIME_COLLISIONS", "Error configurando colisiones: ${e.message}")
        }
    }

    private fun setupButtonListeners() {
        btnNorth.setOnTouchListener { _, event -> handleMovementWithCollisions(event, 0, -1); true }
        btnSouth.setOnTouchListener { _, event -> handleMovementWithCollisions(event, 0, 1); true }
        btnEast.setOnTouchListener { _, event -> handleMovementWithCollisions(event, 1, 0); true }
        btnWest.setOnTouchListener { _, event -> handleMovementWithCollisions(event, -1, 0); true }

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

        btnNightMode.setOnClickListener {
            if (esimioGameActive) {
                stopEsimioGame()
            } else {
                showEsimioDifficultyDialog()
            }
        }
    }

    // 🔴 NUEVO MÉTODO PARA MANEJAR MOVIMIENTO CON VERIFICACIÓN DE COLISIONES
    private fun handleMovementWithCollisions(event: MotionEvent, deltaX: Int, deltaY: Int) {
        if (::movementManager.isInitialized) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val newX = playerPosition.first + deltaX
                    val newY = playerPosition.second + deltaY
                    val newPosition = Pair(newX, newY)

                    if (isPositionBlocked(newPosition)) {
                        //Toast.makeText(this, "🚫 Área bloqueada", Toast.LENGTH_SHORT).show()
                        return
                    }
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
        return collisionAreas.any { rect ->
            x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom
        }
    }

    private fun updatePlayerPosition(position: Pair<Int, Int>) {
        runOnUiThread {
            try {
                if (isPositionBlocked(position)) {
                    Log.d("ESIME_COLLISIONS", "🚫 Colisión detectada en: $position")
                    // Toast.makeText(this, "Movimiento bloqueado", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                playerPosition = position
                mapView.updateLocalPlayerPosition(position, forceCenter = true)
                mapView.invalidate()

                if (::serverConnectionManager.isInitialized) {
                    serverConnectionManager.sendUpdateMessage(
                        playerName,
                        position,
                        MapMatrixProvider.MAP_ESIME
                    )
                }

                // Actualizar posición del jugador para esimios
                if (esimioGameActive && ::esimioController.isInitialized) {
                    esimioController.updatePlayerPosition(playerName, position)
                }

                checkPositionForMapChange(position)
                checkPositionForBuildingInteraction(position)

            } catch (e: Exception) {
                Log.e("Esime", "Error en updatePlayerPosition: ${e.message}")
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
        buildingLocations.forEach { (buildingNumber, buildingPos) ->
            if (x in (buildingPos.first - 2)..(buildingPos.first + 2) &&
                y in (buildingPos.second - 2)..(buildingPos.second + 2)) {
                return buildingNumber
            }
        }
        return null
    }

    private fun enterBuilding3() {
        Toast.makeText(this, "Acceso al Edificio 3 - En desarrollo", Toast.LENGTH_LONG).show()
        Log.d("Esime", "Intento de acceso al Edificio 3 desde posición: $playerPosition")
    }

    private fun showBuildingNotAvailable(buildingNumber: Int) {
        Toast.makeText(this, "Edificio $buildingNumber: No se ha creado base de edificio", Toast.LENGTH_LONG).show()
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.MAP_ZACATENCO -> returnToZacatenco()
            else -> Log.d("Esime", "Mapa destino no reconocido: $targetMap")
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
                    "esimio_position" -> {
                        val esimioId = jsonObject.optString("id", "esimio")
                        val x = jsonObject.getInt("x")
                        val y = jsonObject.getInt("y")
                        val esimioPosition = Pair(x, y)

                        if (::esimioController.isInitialized) {
                            esimioController.setEsimioPosition(esimioId, esimioPosition)
                        }

                        mapView.updateSpecialEntity(esimioId, esimioPosition, MapMatrixProvider.MAP_ESIME)
                        mapView.invalidate()
                    }
                    "esimio_game_command" -> {
                        when (jsonObject.optString("command")) {
                            "start" -> {
                                if (!esimioGameActive) {
                                    val gameDifficulty = jsonObject.optInt("difficulty", 1)
                                    startEsimioGame(gameDifficulty)
                                }
                            }
                            "stop" -> {
                                if (esimioGameActive) {
                                    stopEsimioGame()
                                }
                            }
                            "caught" -> {
                                val caughtPlayer = jsonObject.optString("player")
                                if (caughtPlayer == playerName && esimioGameActive) {
                                    onEsimioCaughtPlayer()
                                }
                            }
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
        if (esimioGameActive && ::esimioController.isInitialized) {
            esimioController.stopGame()
        }
        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }
    }

    companion object {
        private const val TAG = "Esime"
    }

    // ========== MÉTODOS PARA ESIMIOS ==========


    private fun showEsimioDifficultyDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Modo Nocturno en ESIME")
            .setMessage("Los esimios rondan por el campus de noche.\n" +
                    "¡Si te atrapan, te enviarán de regreso a Zacatenco!\n\n" +
                    "¿Quieres activar el modo nocturno?")
            .setPositiveButton("¡Iniciar!") { dialog, _ ->
                dialog.dismiss()
                showDifficultySelectionDialog()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showDifficultySelectionDialog() {
        val options = arrayOf("Fácil (3 esimios)", "Medio (5 esimios)", "Difícil (8 esimios)")
        var selectedDifficulty = 1

        AlertDialog.Builder(this)
            .setTitle("Selecciona la dificultad")
            .setSingleChoiceItems(options, 0) { _, which ->
                selectedDifficulty = which + 1
            }
            .setPositiveButton("Comenzar") { dialog, _ ->
                dialog.dismiss()
                startEsimioGame(selectedDifficulty)
            }
            .setNegativeButton("Atrás") { dialog, _ ->
                dialog.dismiss()
                showEsimioDifficultyDialog() // Vuelve al diálogo anterior
            }
            .setCancelable(false)
            .show()
    }

    private fun startEsimioGame(difficulty: Int) {
        esimioGameActive = true
        nightMode = true

        // Inicializar controlador de esimios
        esimioController = EsimioController(
            onEsimioPositionChanged = { esimioId, position ->
                runOnUiThread {
                    mapView.updateSpecialEntity(
                        esimioId,
                        position,
                        MapMatrixProvider.MAP_ESIME
                    )
                    mapView.invalidate()
                }
            },
            onPlayerCaught = {
                onEsimioCaughtPlayer()
            }
        )

        esimioController.startGame(difficulty)

        runOnUiThread {
            nightOverlay.visibility = View.VISIBLE
            btnNightMode.text = "☀️"
            btnNightMode.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFA500"))
            Toast.makeText(this,
                "🌙 MODO NOCTURNO ACTIVADO\n¡Cuidado con los esimios!",
                Toast.LENGTH_LONG
            ).show()
        }

        sendEsimioGameUpdate("start", difficulty = difficulty)
    }

    private fun stopEsimioGame() {
        esimioGameActive = false
        nightMode = false

        if (::esimioController.isInitialized) {
            esimioController.stopGame()
        }

        runOnUiThread {
            nightOverlay.visibility = View.GONE
            btnNightMode.text = "🌙"
            btnNightMode.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#424242"))
            clearEsimioEntities()
            mapView.invalidate()
            Toast.makeText(this, "Modo nocturno desactivado", Toast.LENGTH_SHORT).show()
        }

        sendEsimioGameUpdate("stop")
    }

    private fun clearEsimioEntities() {
        for (i in 0 until 10) {
            mapView.removeSpecialEntity("esimio_$i")
        }
    }

    private fun onEsimioCaughtPlayer() {
        if (esimioGameActive) {
            stopEsimioGame()

            runOnUiThread {
                AlertDialog.Builder(this)
                    .setTitle("😱 ¡FUISTE ATRAPADO!")
                    .setMessage("Un esimio te atrapó en ESIME.\n\n" +
                            "Serás enviado de regreso a Zacatenco.")
                    .setPositiveButton("Entendido") { dialog, _ ->
                        dialog.dismiss()
                        returnToZacatencoAfterCaught()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun returnToZacatencoAfterCaught() {
        if (::mapView.isInitialized) {
            mapView.playerManager.cleanup()
        }

        val intent = Intent(this, Zacatenco::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            putExtra("INITIAL_POSITION", Pair(8, 18)) // Posición de spawn en Zacatenco
            putExtra("PREVIOUS_POSITION", playerPosition)
            putExtra("WAS_CAUGHT_BY_ESIMIO", true) // Opcional: para tracking
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        startActivity(intent)
        finish()
    }

    private fun sendEsimioGameUpdate(action: String, difficulty: Int = 1) {
        try {
            val message = JSONObject().apply {
                put("type", "esimio_game_update")
                put("action", action)
                put("player", playerName)
                put("map", MapMatrixProvider.MAP_ESIME)

                if (action == "start") {
                    put("difficulty", difficulty)
                }
            }

            serverConnectionManager.onlineServerManager.queueMessage(message.toString())
        } catch (e: Exception) {
            Log.e("Esime", "Error enviando actualización: ${e.message}")
        }
    }
}