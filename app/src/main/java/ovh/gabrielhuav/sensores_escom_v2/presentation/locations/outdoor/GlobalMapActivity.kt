package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import java.util.Random
import java.util.UUID
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.Road
import kotlin.math.min

class GlobalMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var playerMarker: Marker
    private lateinit var returnMarker: Marker
    private lateinit var currentLocation: GeoPoint
    private lateinit var serverConnectionManager: ServerConnectionManager
    private lateinit var roadManager: RoadManager

    // --- UI ---
    private lateinit var tvScore: TextView
    private var score = 0

    // --- Listas de Objetos ---
    private val coinMarkers = mutableListOf<Marker>()
    private val vehicleMarkers = mutableListOf<VehicleMarker>()

    // --- Overlays ---
    private lateinit var compassOverlay: CompassOverlay
    private lateinit var rotationGestureOverlay: RotationGestureOverlay

    private lateinit var playerId: String
    private var isServer: Boolean = false
    private var previousPosition: Pair<Int, Int>? = null
    private lateinit var entryPoint: GeoPoint

    // --- Configuración de Movimiento (CAMINANDO - ~2 km/h) ---
    private val WALK_MIN_STEP = 0.000001
    private val WALK_MAX_STEP = 0.000015
    private val WALK_ACCEL = 0.0000005

    // --- Configuración de Movimiento en Carretera (más rápido) ---
    private val ROAD_MIN_STEP = 0.000005   // 5x más rápido al inicio
    private val ROAD_MAX_STEP = 0.00008    // ~5x más rápido al máximo
    private val ROAD_ACCEL = 0.000002      // Aceleración más rápida

    // --- Estado del Jugador ---
    private var currentVehicle: VehicleType? = null // Null = Caminando
    private var isOnRoad = false  // True = Attached to road (your feature!)

    // Variables dinámicas de movimiento
    private var dynamicMinStep = WALK_MIN_STEP
    private var dynamicMaxStep = WALK_MAX_STEP
    private var dynamicAccel = WALK_ACCEL

    // Loop de movimiento
    private val MOVEMENT_INTERVAL = 50L
    private val RETURN_DISTANCE_THRESHOLD = 15.0
    private val INTERACTION_DISTANCE = 10.0
    private val ROAD_SNAP_DISTANCE = 30.0  // Distance to detect nearby road for snapping

    private val movementHandler = Handler(Looper.getMainLooper())
    private var movementRunnable: Runnable? = null

    private var currentSpeed = 0.0
    private var directionLat = 0.0
    private var directionLon = 0.0

    // --- Road tracking variables ---
    private var roadOverlay: Polyline? = null
    private var isTracking = false
    private var track = mutableListOf<GeoPoint>()

    // --- DEFINICIÓN DE VEHÍCULOS ---
    enum class VehicleType(
        val typeName: String,
        val minStep: Double,
        val maxStep: Double,
        val acceleration: Double,
        val iconColor: Int,
        val systemIcon: Int
    ) {
        DRONE(
            "Dron de Vigilancia",
            0.000005,
            0.00005,
            0.000002,
            Color.CYAN,
            android.R.drawable.ic_menu_view
        ),
        JET(
            "Avión Supersónico",
            0.00001,
            0.00015,
            0.000001,
            Color.BLUE,
            android.R.drawable.ic_menu_send
        ),
        UFO(
            "OVNI Experimental",
            0.00002,
            0.00020,
            0.000010,
            Color.MAGENTA,
            android.R.drawable.ic_menu_compass
        )
    }

    data class VehicleMarker(val marker: Marker, val type: VehicleType)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_map)

        tvScore = findViewById(R.id.tv_score)
        updateScoreUI()

        playerId = intent.getStringExtra("PLAYER_NAME") ?: "Player_${UUID.randomUUID()}"
        if (playerId.isBlank()) playerId = "Player_${UUID.randomUUID()}"
        isServer = intent.getBooleanExtra("IS_SERVER", false)

        @Suppress("UNCHECKED_CAST")
        previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>
        @Suppress("UNCHECKED_CAST")
        val initialPosPair = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>

        val startPoint = if (initialPosPair != null) {
            GeoPoint(initialPosPair.second / 1e6, initialPosPair.first / 1e6)
        } else {
            GeoPoint(19.5049291, -99.1466950)
        }
        entryPoint = startPoint.clone()
        currentLocation = startPoint.clone()

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "Sensores_ESCOM_V2"

        roadManager = OSRMRoadManager(this, Configuration.getInstance().userAgentValue)

        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)

        compassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), mapView)
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)

        val mapController = mapView.controller
        mapController.setZoom(19.5)
        mapController.setCenter(startPoint)

        initReturnPointMarker()
        initPlayerMarker()

        // --- GENERACIÓN DEL MUNDO ---
        spawnCoins(startPoint)
        spawnVehicles(startPoint)

        setupSmoothMovementButtons()
        setupInteractionButton()

        val onlineServerManager = OnlineServerManager.getInstance(this)
        serverConnectionManager = ServerConnectionManager(this, onlineServerManager)
        serverConnectionManager.connectToServer { success ->
            runOnUiThread {
                if (success) {
                    serverConnectionManager.onlineServerManager.apply {
                        sendJoinMessage(playerId)
                        requestPositionsUpdate()
                    }
                    Toast.makeText(this, "Conectado al servidor.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ---------------------------------------------------------
    // LÓGICA DE SPAWN (VEHÍCULOS Y MONEDAS)
    // ---------------------------------------------------------

    private fun spawnCoins(center: GeoPoint) {
        val random = kotlin.random.Random
        for (i in 0 until 10) {
            val latOffset = (random.nextDouble() - 0.5) * 0.001
            val lonOffset = (random.nextDouble() - 0.5) * 0.001
            val coinPos = GeoPoint(center.latitude + latOffset, center.longitude + lonOffset)

            val coinMarker = Marker(mapView)
            coinMarker.position = coinPos
            coinMarker.title = "Moneda ESCOM"
            coinMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            val icon = ContextCompat.getDrawable(this, android.R.drawable.star_big_on)
            icon?.setColorFilter(Color.rgb(255, 215, 0), PorterDuff.Mode.SRC_ATOP)
            coinMarker.icon = icon

            mapView.overlays.add(coinMarker)
            coinMarkers.add(coinMarker)
        }
    }

    private fun spawnVehicles(center: GeoPoint) {
        val types = VehicleType.values()
        val random = Random()

        for (type in types) {
            val latOffset = (random.nextDouble() - 0.5) * 0.002
            val lonOffset = (random.nextDouble() - 0.5) * 0.002
            val vPos = GeoPoint(center.latitude + latOffset, center.longitude + lonOffset)
            createVehicleMarkerOnMap(vPos, type)
        }
        mapView.invalidate()
    }

    private fun createVehicleMarkerOnMap(position: GeoPoint, type: VehicleType) {
        val vMarker = Marker(mapView)
        vMarker.position = position
        vMarker.title = "Vehículo: ${type.typeName}"
        vMarker.snippet = "Presiona A para pilotar"
        vMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

        val icon = ContextCompat.getDrawable(this, type.systemIcon)
        icon?.setColorFilter(type.iconColor, PorterDuff.Mode.SRC_ATOP)
        vMarker.icon = icon

        mapView.overlays.add(vMarker)
        vehicleMarkers.add(VehicleMarker(vMarker, type))
    }

    // ---------------------------------------------------------
    // LÓGICA DE INTERACCIÓN (BOTÓN A) - MERGED WITH ROAD TRACKING
    // ---------------------------------------------------------

    private fun setupInteractionButton() {
        findViewById<Button>(R.id.button_a).setOnClickListener {
            handleInteraction()
        }
    }

    private fun handleInteraction() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // 1. PRIORIDAD: ¿Estamos sobre el punto de retorno?
        if (currentLocation.distanceToAsDouble(entryPoint) < RETURN_DISTANCE_THRESHOLD) {
            if (previousPosition != null) {
                returnToGameplayActivity()
                return
            }
        }

        // 2. ¿Ya estamos en modo carretera? -> SALIR de la carretera
        if (isOnRoad) {
            exitRoadMode()
            vibrator.vibrate(100)
            val message = if (currentVehicle != null) {
                "Vehículo salió de la carretera. Vuelo libre."
            } else {
                "Saliste de la carretera. Movimiento libre."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            return
        }

        // 3. ¿Ya estamos en un vehículo? -> Intentar ENTRAR a carretera o BAJARSE
        if (currentVehicle != null) {
            // Si hay carretera cerca, intentar adjuntarse a ella
            // Si no hay carretera cerca, bajarse del vehículo
            tryEnterRoadModeOrDismount(vibrator)
            return
        }

        // 4. ¿Estamos cerca de un vehículo para subirnos? -> SUBIRSE
        val nearbyVehicle = vehicleMarkers.find {
            currentLocation.distanceToAsDouble(it.marker.position) < INTERACTION_DISTANCE
        }

        if (nearbyVehicle != null) {
            mountVehicle(nearbyVehicle)
            vibrator.vibrate(300)
            Toast.makeText(this, "Pilotando: ${nearbyVehicle.type.typeName}", Toast.LENGTH_SHORT).show()
            return
        }

        // 5. ¿No hay vehículo cerca? -> Intentar ENTRAR a modo carretera (caminando)
        tryEnterRoadMode(vibrator)
    }

    // ---------------------------------------------------------
    // ROAD TRACKING MODE (YOUR FEATURE!)
    // ---------------------------------------------------------

    private fun tryEnterRoadMode(vibrator: Vibrator) {
        Toast.makeText(this, "Buscando carretera cercana...", Toast.LENGTH_SHORT).show()

        // Try to snap to the nearest road
        val waypoints = ArrayList<GeoPoint>()
        waypoints.add(currentLocation)
        val nearbyPoint = currentLocation.destinationPoint(1.0, 0.0)
        waypoints.add(nearbyPoint)

        Thread {
            val road = roadManager.getRoad(waypoints)
            runOnUiThread {
                if (road.mStatus == Road.STATUS_OK && road.mRouteHigh.isNotEmpty()) {
                    // Found a road! Check if close enough
                    val roadPoint = road.mRouteHigh[0]
                    val distanceToRoad = currentLocation.distanceToAsDouble(roadPoint)

                    if (distanceToRoad < ROAD_SNAP_DISTANCE) {
                        enterRoadMode(road, vibrator)
                    } else {
                        Toast.makeText(this, "No hay carretera cerca. Acércate más.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No se encontró carretera.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun tryEnterRoadModeOrDismount(vibrator: Vibrator) {
        val vehicleName = currentVehicle?.typeName ?: "Vehículo"
        Toast.makeText(this, "Buscando carretera para $vehicleName...", Toast.LENGTH_SHORT).show()

        // Try to snap to the nearest road
        val waypoints = ArrayList<GeoPoint>()
        waypoints.add(currentLocation)
        val nearbyPoint = currentLocation.destinationPoint(1.0, 0.0)
        waypoints.add(nearbyPoint)

        Thread {
            val road = roadManager.getRoad(waypoints)
            runOnUiThread {
                if (road.mStatus == Road.STATUS_OK && road.mRouteHigh.isNotEmpty()) {
                    // Found a road! Check if close enough
                    val roadPoint = road.mRouteHigh[0]
                    val distanceToRoad = currentLocation.distanceToAsDouble(roadPoint)

                    if (distanceToRoad < ROAD_SNAP_DISTANCE) {
                        // Attach vehicle to road
                        enterRoadMode(road, vibrator)
                    } else {
                        // No road nearby, dismount vehicle
                        dismountVehicle()
                        vibrator.vibrate(100)
                        Toast.makeText(this, "No hay carretera cerca. Te bajaste del vehículo.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // No road found, dismount vehicle
                    dismountVehicle()
                    vibrator.vibrate(100)
                    Toast.makeText(this, "No se encontró carretera. Te bajaste del vehículo.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun enterRoadMode(road: Road, vibrator: Vibrator) {
        isOnRoad = true
        isTracking = true

        // Snap to road
        track.clear()
        track.addAll(road.mRouteHigh)
        currentLocation = track[0].clone()
        playerMarker.position = currentLocation
        mapView.controller.animateTo(currentLocation)

        // Show road overlay
        roadOverlay?.let { mapView.overlays.remove(it) }
        roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay?.outlinePaint?.color = Color.rgb(0, 150, 255)  // Blue road
        roadOverlay?.outlinePaint?.strokeWidth = 8f
        mapView.overlays.add(roadOverlay)

        // Update movement physics based on whether we're in a vehicle or walking
        if (currentVehicle != null) {
            // Use vehicle's speed settings
            dynamicMinStep = currentVehicle!!.minStep
            dynamicMaxStep = currentVehicle!!.maxStep
            dynamicAccel = currentVehicle!!.acceleration
            currentSpeed = dynamicMinStep

            // Keep vehicle icon but add road indicator color
            val vehicleIcon = ContextCompat.getDrawable(this, currentVehicle!!.systemIcon)
            vehicleIcon?.setColorFilter(Color.rgb(0, 150, 255), PorterDuff.Mode.SRC_ATOP) // Blue tint for road mode
            playerMarker.icon = vehicleIcon
            playerMarker.title = "Tú (${currentVehicle!!.typeName} en carretera)"

            vibrator.vibrate(200)
            Toast.makeText(this, "¡${currentVehicle!!.typeName} en carretera! Presiona A para salir.", Toast.LENGTH_SHORT).show()
        } else {
            // Walking on road - use road speed settings
            dynamicMinStep = ROAD_MIN_STEP
            dynamicMaxStep = ROAD_MAX_STEP
            dynamicAccel = ROAD_ACCEL
            currentSpeed = dynamicMinStep

            // Change player icon to indicate road mode
            val roadIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)
            roadIcon?.setColorFilter(Color.rgb(0, 150, 255), PorterDuff.Mode.SRC_ATOP)
            playerMarker.icon = roadIcon
            playerMarker.title = "Tú (En carretera)"

            vibrator.vibrate(200)
            Toast.makeText(this, "¡En carretera! Movimiento rápido. Presiona A para salir.", Toast.LENGTH_SHORT).show()
        }

        mapView.invalidate()
    }

    private fun exitRoadMode() {
        isOnRoad = false
        isTracking = false
        track.clear()

        // Remove road overlay
        roadOverlay?.let { mapView.overlays.remove(it) }
        roadOverlay = null

        // Reset movement physics based on whether we're in a vehicle or walking
        if (currentVehicle != null) {
            // Stay in vehicle, restore vehicle speed settings
            dynamicMinStep = currentVehicle!!.minStep
            dynamicMaxStep = currentVehicle!!.maxStep
            dynamicAccel = currentVehicle!!.acceleration
            currentSpeed = dynamicMinStep

            // Restore vehicle icon with original color
            val vehicleIcon = ContextCompat.getDrawable(this, currentVehicle!!.systemIcon)
            vehicleIcon?.setColorFilter(currentVehicle!!.iconColor, PorterDuff.Mode.SRC_ATOP)
            playerMarker.icon = vehicleIcon
            playerMarker.title = "Tú (${currentVehicle!!.typeName})"
        } else {
            // Reset to walking
            dynamicMinStep = WALK_MIN_STEP
            dynamicMaxStep = WALK_MAX_STEP
            dynamicAccel = WALK_ACCEL
            currentSpeed = dynamicMinStep

            // Reset player icon
            try {
                playerMarker.icon = ContextCompat.getDrawable(this, R.drawable.pasajero)
                playerMarker.icon?.clearColorFilter()
            } catch (e: Exception) {
                playerMarker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)
            }
            playerMarker.title = "Tú (A pie)"
        }

        mapView.invalidate()
    }

    // ---------------------------------------------------------
    // VEHICLE MOUNTING (FROM MAIN)
    // ---------------------------------------------------------

    private fun mountVehicle(vMarker: VehicleMarker) {
        currentVehicle = vMarker.type
        dynamicMinStep = vMarker.type.minStep
        dynamicMaxStep = vMarker.type.maxStep
        dynamicAccel = vMarker.type.acceleration
        currentSpeed = dynamicMinStep

        val vehicleIcon = ContextCompat.getDrawable(this, vMarker.type.systemIcon)
        vehicleIcon?.setColorFilter(vMarker.type.iconColor, PorterDuff.Mode.SRC_ATOP)
        playerMarker.icon = vehicleIcon
        playerMarker.title = "Tú (${vMarker.type.typeName})"

        mapView.overlays.remove(vMarker.marker)
        vehicleMarkers.remove(vMarker)
        mapView.invalidate()
    }

    private fun dismountVehicle() {
        currentVehicle?.let { type ->
            createVehicleMarkerOnMap(currentLocation.clone(), type)
        }

        currentVehicle = null
        dynamicMinStep = WALK_MIN_STEP
        dynamicMaxStep = WALK_MAX_STEP
        dynamicAccel = WALK_ACCEL
        currentSpeed = dynamicMinStep

        try {
            playerMarker.icon = ContextCompat.getDrawable(this, R.drawable.pasajero)
            playerMarker.icon?.clearColorFilter()
        } catch (e: Exception) {
            playerMarker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)
        }
        playerMarker.title = "Tú (A pie)"
        mapView.invalidate()
    }

    // ---------------------------------------------------------
    // LÓGICA DE MOVIMIENTO
    // ---------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSmoothMovementButtons() {
        val btnNorth = findViewById<Button>(R.id.button_north)
        val btnSouth = findViewById<Button>(R.id.button_south)
        val btnEast = findViewById<Button>(R.id.button_east)
        val btnWest = findViewById<Button>(R.id.button_west)

        btnNorth.setOnTouchListener { _, event -> handleTouch(event, 0.0, 1.0) }
        btnSouth.setOnTouchListener { _, event -> handleTouch(event, 0.0, -1.0) }
        btnEast.setOnTouchListener { _, event -> handleTouch(event, 1.0, 0.0) }
        btnWest.setOnTouchListener { _, event -> handleTouch(event, -1.0, 0.0) }
    }

    private fun handleTouch(event: MotionEvent, dirLon: Double, dirLat: Double): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                directionLon = dirLon
                directionLat = dirLat
                currentSpeed = dynamicMinStep
                startMovementLoop()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopMovementLoop()
                return true
            }
        }
        return false
    }

    private fun startMovementLoop() {
        if (movementRunnable == null) {
            movementRunnable = object : Runnable {
                override fun run() {
                    if (isOnRoad) {
                        // When on road, move along the road
                        moveOnRoad()
                    } else {
                        // Free movement
                        val deltaLon = directionLon * currentSpeed
                        val deltaLat = directionLat * currentSpeed
                        movePlayer(deltaLon, deltaLat)
                    }

                    currentSpeed = min(currentSpeed + dynamicAccel, dynamicMaxStep)
                    movementHandler.postDelayed(this, MOVEMENT_INTERVAL)
                }
            }
            movementHandler.post(movementRunnable!!)
        }
    }

    private fun stopMovementLoop() {
        movementRunnable?.let {
            movementHandler.removeCallbacks(it)
        }
        movementRunnable = null
        directionLat = 0.0
        directionLon = 0.0
        currentSpeed = dynamicMinStep
    }

    private fun moveOnRoad() {
        // Calculate bearing based on direction buttons
        val bearing = when {
            directionLat > 0 -> 0.0    // North
            directionLat < 0 -> 180.0  // South
            directionLon > 0 -> 90.0   // East
            directionLon < 0 -> 270.0  // West
            else -> return
        }

        // Calculate distance based on current speed (converted to meters approximately)
        val distanceMeters = currentSpeed * 111000  // rough conversion from degrees to meters

        val destination = currentLocation.destinationPoint(distanceMeters.coerceAtLeast(5.0), bearing)
        val waypoints = arrayListOf(currentLocation, destination)

        Thread {
            val road = roadManager.getRoad(waypoints)
            runOnUiThread {
                if (road.mStatus == Road.STATUS_OK && road.mRouteHigh.size > 1) {
                    // Move to the next point on the road
                    currentLocation = road.mRouteHigh[1]
                    playerMarker.position = currentLocation
                    mapView.controller.setCenter(currentLocation)

                    // Update the road overlay
                    roadOverlay?.let { mapView.overlays.remove(it) }
                    roadOverlay = RoadManager.buildRoadOverlay(road)
                    roadOverlay?.outlinePaint?.color = Color.rgb(0, 150, 255)
                    roadOverlay?.outlinePaint?.strokeWidth = 8f
                    mapView.overlays.add(roadOverlay)
                    mapView.invalidate()

                    checkCoinCollection()
                    sendPositionUpdate()
                }
                // If can't move on road in that direction, just don't move (stay on road)
            }
        }.start()
    }

    private fun movePlayer(lonDelta: Double, latDelta: Double) {
        currentLocation.latitude += latDelta
        currentLocation.longitude += lonDelta

        playerMarker.position = currentLocation
        mapView.invalidate()
        mapView.controller.setCenter(currentLocation)

        checkCoinCollection()
        sendPositionUpdate()
    }

    private fun sendPositionUpdate() {
        val x = (currentLocation.longitude * 1e6).toInt()
        val y = (currentLocation.latitude * 1e6).toInt()

        if (::serverConnectionManager.isInitialized && serverConnectionManager.isConnected()) {
            serverConnectionManager.sendUpdateMessage(playerId, Pair(x, y), "global")
        }
    }

    private fun checkCoinCollection() {
        val collectedCoins = mutableListOf<Marker>()
        val collectionDistance = when {
            currentVehicle != null -> 10.0
            isOnRoad -> 8.0  // Slightly larger radius when on road (moving fast)
            else -> 5.0
        }

        for (coin in coinMarkers) {
            if (currentLocation.distanceToAsDouble(coin.position) < collectionDistance) {
                collectedCoins.add(coin)
            }
        }

        if (collectedCoins.isNotEmpty()) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(50)

            for (coin in collectedCoins) {
                mapView.overlays.remove(coin)
                coinMarkers.remove(coin)
                score += 10
            }
            updateScoreUI()
            mapView.invalidate()
        }
    }

    private fun updateScoreUI() {
        tvScore.text = "Puntos: $score"
    }

    // ---------------------------------------------------------
    // CONFIGURACIÓN INICIAL MARCADORES
    // ---------------------------------------------------------

    private fun initPlayerMarker() {
        if (::playerMarker.isInitialized) {
            try {
                playerMarker.icon = ContextCompat.getDrawable(this, R.drawable.pasajero)
                playerMarker.icon?.clearColorFilter()
            } catch (e: Exception) {
                playerMarker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)
            }
            return
        }

        playerMarker = Marker(mapView)
        playerMarker.position = currentLocation
        playerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        playerMarker.title = "Tú"

        try {
            playerMarker.icon = ContextCompat.getDrawable(this, R.drawable.pasajero)
        } catch (e: Exception) {
            playerMarker.icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)
        }

        mapView.overlays.add(playerMarker)
        mapView.invalidate()
    }

    private fun initReturnPointMarker() {
        returnMarker = Marker(mapView)
        returnMarker.position = entryPoint
        returnMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        returnMarker.title = "Salida"
        val returnIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_directions)
        returnIcon?.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP)
        returnMarker.icon = returnIcon
        mapView.overlays.add(returnMarker)
    }

    private fun returnToGameplayActivity() {
        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerId)
            putExtra("IS_SERVER", isServer)
            putExtra("INITIAL_POSITION", previousPosition)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (::compassOverlay.isInitialized) compassOverlay.enableCompass()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopMovementLoop()
        if (::compassOverlay.isInitialized) compassOverlay.disableCompass()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMovementLoop()
    }

    companion object {
        private const val TAG = "GlobalMapActivity"
    }
}