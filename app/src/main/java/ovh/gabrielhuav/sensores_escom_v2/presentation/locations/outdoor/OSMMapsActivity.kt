package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration as OSMConfiguration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothWebSocketBridge
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import kotlin.math.abs

/**
 * Activity that provides a full OpenStreetMap view with player movement,
 * multiplayer support, and integration with the existing game system
 */
class OSMMapActivity : AppCompatActivity(),
    BluetoothManager.BluetoothManagerCallback,
    BluetoothGameManager.ConnectionListener,
    OnlineServerManager.WebSocketListener {

    companion object {
        private const val TAG = "OSMMapActivity"

        // Default map configuration
        private const val DEFAULT_ZOOM = 18.0
        private const val MIN_ZOOM = 12.0
        private const val MAX_ZOOM = 20.0

        // Movement configuration
        private const val MOVEMENT_SPEED = 0.00001 // Degrees per movement tick
        private const val MOVEMENT_INTERVAL = 50L // Milliseconds between movements

        // Key locations (IPN Zacatenco area)
        private val ESCOM_CENTER = GeoPoint(19.504633, -99.146744)
        private val ZACATENCO_ENTRANCE = GeoPoint(19.505500, -99.135000)
        private val LINDAVISTA_EXIT = GeoPoint(19.514000, -99.134000)

        // Transition points
        private val TRANSITION_POINTS = mapOf(
            "escom_entrance" to TransitionPoint(
                GeoPoint(19.504633, -99.146744),
                0.0001,
                "main"
            ),
            "zacatenco_main" to TransitionPoint(
                GeoPoint(19.505500, -99.135000),
                0.0001,
                "zacatenco"
            ),
            "lindavista" to TransitionPoint(
                GeoPoint(19.514000, -99.134000),
                0.0001,
                "lindavista"
            )
        )
    }

    data class TransitionPoint(
        val location: GeoPoint,
        val radius: Double,
        val targetActivity: String
    )

    // Map components
    private lateinit var osmMapView: MapView
    private lateinit var mapController: IMapController
    private lateinit var mapContainer: FrameLayout

    // Player tracking
    private var playerMarker: Marker? = null
    private var currentPlayerPosition = ESCOM_CENTER
    private val remotePlayerMarkers = mutableMapOf<String, Marker>()

    // Movement handling
    private val movementHandler = Handler(Looper.getMainLooper())
    private var activeMovementRunnable: Runnable? = null
    private var currentDirection: Direction? = null

    // Overlays
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var compassOverlay: CompassOverlay? = null
    private var scaleBarOverlay: ScaleBarOverlay? = null
    private var rotationGestureOverlay: RotationGestureOverlay? = null
    private var playerPathOverlay: Polyline? = null

    // Path tracking
    private val pathPoints = mutableListOf<GeoPoint>()

    // Networking components
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothBridge: BluetoothWebSocketBridge
    private lateinit var serverConnectionManager: ServerConnectionManager

    // Game state
    private var playerName: String = ""
    private var isServer = false
    private var isConnected = false
    private var isAttachedToMap = true

    // UI components
    private lateinit var btnNorth: Button
    private lateinit var btnSouth: Button
    private lateinit var btnEast: Button
    private lateinit var btnWest: Button
    private lateinit var btnAction: Button
    private lateinit var btnBack: Button
    private lateinit var btnZoomIn: Button
    private lateinit var btnZoomOut: Button
    private lateinit var btnCenterMap: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvCoordinates: TextView

    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            enableLocationOverlay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_osm_map)

        // Get intent extras
        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Player"
        isServer = intent.getBooleanExtra("IS_SERVER", false)
        val initialLat = intent.getDoubleExtra("INITIAL_LAT", ESCOM_CENTER.latitude)
        val initialLon = intent.getDoubleExtra("INITIAL_LON", ESCOM_CENTER.longitude)
        currentPlayerPosition = GeoPoint(initialLat, initialLon)

        // Configure OSM
        OSMConfiguration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = filesDir
            osmdroidTileCache = cacheDir
        }

        initializeViews()
        initializeMap()
        initializeNetworking()
        setupButtonListeners()

        // Request location permissions
        requestLocationPermissions()
    }

    private fun initializeViews() {
        // Map container
        mapContainer = findViewById(R.id.map_container)

        // Create map view
        osmMapView = MapView(this).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = MIN_ZOOM
            maxZoomLevel = MAX_ZOOM
        }
        mapContainer.addView(osmMapView)

        // Movement buttons
        btnNorth = findViewById(R.id.button_north)
        btnSouth = findViewById(R.id.button_south)
        btnEast = findViewById(R.id.button_east)
        btnWest = findViewById(R.id.button_west)

        // Control buttons
        btnAction = findViewById(R.id.button_a)
        btnBack = findViewById(R.id.button_back)
        btnZoomIn = findViewById(R.id.button_zoom_in)
        btnZoomOut = findViewById(R.id.button_zoom_out)
        btnCenterMap = findViewById(R.id.button_center)

        // Status views
        tvStatus = findViewById(R.id.tv_status)
        tvCoordinates = findViewById(R.id.tv_coordinates)

        updateCoordinatesDisplay()
    }

    private fun initializeMap() {
        mapController = osmMapView.controller
        mapController.setZoom(DEFAULT_ZOOM)
        mapController.setCenter(currentPlayerPosition)

        // Add map event overlay for tap detection
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                Log.d(TAG, "Tapped at: ${p.latitude}, ${p.longitude}")
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                addCustomMarker(p, "Custom Point", "Long pressed location")
                return true
            }
        }
        val eventsOverlay = MapEventsOverlay(mapEventsReceiver)
        osmMapView.overlays.add(eventsOverlay)

        // Add compass
        compassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), osmMapView).apply {
            enableCompass()
        }
        osmMapView.overlays.add(compassOverlay)

        // Add scale bar
        scaleBarOverlay = ScaleBarOverlay(osmMapView).apply {
            setAlignBottom(true)
            setAlignRight(true)
        }
        osmMapView.overlays.add(scaleBarOverlay)

        // Add rotation gestures
        rotationGestureOverlay = RotationGestureOverlay(osmMapView).apply {
            isEnabled = true
        }
        osmMapView.overlays.add(rotationGestureOverlay)

        // Setup player marker
        setupPlayerMarker()

        // Setup path tracking
        setupPathTracking()

        // Add important location markers
        addLocationMarkers()
    }

    private fun setupPlayerMarker() {
        playerMarker = Marker(osmMapView).apply {
            position = currentPlayerPosition
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = playerName
            snippet = "Tu posición"

            // Custom icon (blue circle for local player)
            icon = ContextCompat.getDrawable(this@OSMMapActivity, android.R.drawable.presence_online)?.apply {
                setTint(Color.BLUE)
            }
        }
        osmMapView.overlays.add(playerMarker)
    }

    private fun setupPathTracking() {
        playerPathOverlay = Polyline().apply {
            outlinePaint.apply {
                color = Color.BLUE
                strokeWidth = 5f
                alpha = 128
            }
        }
        osmMapView.overlays.add(playerPathOverlay)
        pathPoints.add(currentPlayerPosition)
    }

    private fun addLocationMarkers() {
        // ESCOM marker
        addCustomMarker(
            ESCOM_CENTER,
            "ESCOM",
            "Escuela Superior de Cómputo",
            Color.RED
        )

        // Zacatenco entrance
        addCustomMarker(
            ZACATENCO_ENTRANCE,
            "Entrada Principal",
            "IPN Zacatenco",
            Color.GREEN
        )

        // Add transition zone markers
        TRANSITION_POINTS.forEach { (name, point) ->
            addCustomMarker(
                point.location,
                name.replace("_", " ").capitalize(),
                "Zona de transición",
                Color.YELLOW
            )
        }
    }

    private fun addCustomMarker(position: GeoPoint, title: String, snippet: String, color: Int = Color.RED): Marker {
        val marker = Marker(osmMapView).apply {
            this.position = position
            this.title = title
            this.snippet = snippet
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            icon = ContextCompat.getDrawable(this@OSMMapActivity, android.R.drawable.ic_menu_mylocation)?.apply {
                setTint(color)
            }
        }
        osmMapView.overlays.add(marker)
        return marker
    }

    private fun initializeNetworking() {
        // Initialize Bluetooth manager
        bluetoothManager = BluetoothManager.getInstance(this, tvStatus).apply {
            setCallback(this@OSMMapActivity)
        }

        // Initialize WebSocket bridge
        bluetoothBridge = BluetoothWebSocketBridge.getInstance()

        // Initialize online server manager
        val onlineServerManager = OnlineServerManager.getInstance(this).apply {
            setListener(this@OSMMapActivity)
        }

        // Initialize ServerConnectionManager
        serverConnectionManager = ServerConnectionManager(
            context = this,
            onlineServerManager = onlineServerManager
        )

        // Connect to server
        connectToOnlineServer()
    }

    private fun connectToOnlineServer() {
        serverConnectionManager.connectToServer { success ->
            if (success) {
                isConnected = true
                serverConnectionManager.onlineServerManager.sendJoinMessage(playerName)
                runOnUiThread {
                    tvStatus.text = "Conectado al servidor"
                }
            } else {
                runOnUiThread {
                    tvStatus.text = "Sin conexión al servidor"
                }
            }
        }
    }

    private fun setupButtonListeners() {
        // Movement buttons
        btnNorth.setOnTouchListener { _, event ->
            handleMovementButton(event, Direction.NORTH)
            true
        }

        btnSouth.setOnTouchListener { _, event ->
            handleMovementButton(event, Direction.SOUTH)
            true
        }

        btnEast.setOnTouchListener { _, event ->
            handleMovementButton(event, Direction.EAST)
            true
        }

        btnWest.setOnTouchListener { _, event ->
            handleMovementButton(event, Direction.WEST)
            true
        }

        // Action button
        btnAction.setOnClickListener {
            checkForTransition()
        }

        // Back button
        btnBack.setOnClickListener {
            returnToPreviousActivity()
        }

        // Zoom controls
        btnZoomIn.setOnClickListener {
            mapController.zoomIn()
        }

        btnZoomOut.setOnClickListener {
            mapController.zoomOut()
        }

        // Center map button
        btnCenterMap.setOnClickListener {
            centerMapOnPlayer()
        }
    }

    private fun handleMovementButton(event: MotionEvent, direction: Direction) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> startMovement(direction)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> stopMovement()
        }
    }

    private fun startMovement(direction: Direction) {
        stopMovement()
        currentDirection = direction

        activeMovementRunnable = object : Runnable {
            override fun run() {
                movePlayer(direction)
                movementHandler.postDelayed(this, MOVEMENT_INTERVAL)
            }
        }
        movementHandler.post(activeMovementRunnable!!)
    }

    private fun stopMovement() {
        activeMovementRunnable?.let {
            movementHandler.removeCallbacks(it)
        }
        activeMovementRunnable = null
        currentDirection = null
    }

    private fun movePlayer(direction: Direction) {
        val lat = currentPlayerPosition.latitude
        val lon = currentPlayerPosition.longitude

        val newPosition = when (direction) {
            Direction.NORTH -> GeoPoint(lat + MOVEMENT_SPEED, lon)
            Direction.SOUTH -> GeoPoint(lat - MOVEMENT_SPEED, lon)
            Direction.EAST -> GeoPoint(lat, lon + MOVEMENT_SPEED)
            Direction.WEST -> GeoPoint(lat, lon - MOVEMENT_SPEED)
        }

        updatePlayerPosition(newPosition)
        checkForTransitionZones(newPosition)
    }

    private fun updatePlayerPosition(newPosition: GeoPoint) {
        currentPlayerPosition = newPosition

        // Update marker
        playerMarker?.position = newPosition

        // Add to path
        pathPoints.add(newPosition)
        playerPathOverlay?.setPoints(pathPoints)

        // Update display
        updateCoordinatesDisplay()

        // Center map if player is near edge
        val mapCenter = osmMapView.mapCenter as GeoPoint
        val latDiff = abs(mapCenter.latitude - newPosition.latitude)
        val lonDiff = abs(mapCenter.longitude - newPosition.longitude)

        if (latDiff > 0.0003 || lonDiff > 0.0003) {
            mapController.animateTo(newPosition)
        }

        // Send update to server
        if (isConnected) {
            sendPositionUpdate()
        }

        // Refresh map
        osmMapView.invalidate()
    }

    private fun updateCoordinatesDisplay() {
        tvCoordinates.text = String.format(
            "Lat: %.6f, Lon: %.6f",
            currentPlayerPosition.latitude,
            currentPlayerPosition.longitude
        )
    }

    private fun checkForTransitionZones(position: GeoPoint) {
        TRANSITION_POINTS.forEach { (name, point) ->
            val distance = position.distanceToAsDouble(point.location)

            // Convert to degrees (rough approximation)
            val distanceInDegrees = distance / 111000.0

            if (distanceInDegrees < point.radius) {
                Toast.makeText(this, "Presiona A para ir a ${name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkForTransition() {
        TRANSITION_POINTS.forEach { (name, point) ->
            val distance = currentPlayerPosition.distanceToAsDouble(point.location)
            val distanceInDegrees = distance / 111000.0

            if (distanceInDegrees < point.radius) {
                performTransition(point.targetActivity)
                return
            }
        }

        Toast.makeText(this, "No hay transición disponible aquí", Toast.LENGTH_SHORT).show()
    }

    private fun performTransition(targetActivity: String) {
        val intent = when (targetActivity) {
            "main" -> Intent(this, GameplayActivity::class.java)
            "zacatenco" -> Intent(this, Zacatenco::class.java)
            "lindavista" -> Intent(this, Lindavista::class.java)
            else -> null
        }

        intent?.apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            putExtra("FROM_OSM", true)
            putExtra("RETURN_LAT", currentPlayerPosition.latitude)
            putExtra("RETURN_LON", currentPlayerPosition.longitude)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(this)
            finish()
        }
    }

    private fun returnToPreviousActivity() {
        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerName)
            putExtra("IS_SERVER", isServer)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun centerMapOnPlayer() {
        mapController.animateTo(currentPlayerPosition)
    }

    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            locationPermissionLauncher.launch(permissions)
        } else {
            enableLocationOverlay()
        }
    }

    private fun enableLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), osmMapView).apply {
            enableMyLocation()
            enableFollowLocation()

            runOnFirstFix {
                runOnUiThread {
                    mapController.animateTo(myLocation)
                    Toast.makeText(this@OSMMapActivity, "GPS ubicado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        osmMapView.overlays.add(myLocationOverlay)
    }

    private fun sendPositionUpdate() {
        // Use the specialized OSM position update method
        serverConnectionManager.sendOSMPositionUpdate(
            playerId = playerName,
            latitude = currentPlayerPosition.latitude,
            longitude = currentPlayerPosition.longitude
        )
    }

    // Bluetooth callbacks
    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {
        tvStatus.text = "BT: ${device.name}"
    }

    override fun onBluetoothConnectionFailed(error: String) {
        tvStatus.text = "BT Error: $error"
    }

    override fun onConnectionComplete() {
        tvStatus.text = "Conexión completa"
    }

    override fun onConnectionFailed(message: String) {
        tvStatus.text = "Fallo: $message"
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        // Handle device connected
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        // Convert grid position to lat/lon if needed
    }

    // WebSocket listener
    override fun onMessageReceived(message: String) {
        runOnUiThread {
            try {
                val json = JSONObject(message)
                when (json.getString("type")) {
                    "update" -> {
                        val playerId = json.getString("id")
                        if (playerId != playerName) {
                            val lat = json.getDouble("x")
                            val lon = json.getDouble("y")
                            updateRemotePlayer(playerId, GeoPoint(lat, lon))
                        }
                    }
                    "disconnect" -> {
                        val playerId = json.getString("id")
                        removeRemotePlayer(playerId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message: ${e.message}")
            }
        }
    }

    private fun updateRemotePlayer(playerId: String, position: GeoPoint) {
        var marker = remotePlayerMarkers[playerId]

        if (marker == null) {
            marker = Marker(osmMapView).apply {
                this.position = position
                title = playerId
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                icon = ContextCompat.getDrawable(this@OSMMapActivity, android.R.drawable.presence_online)?.apply {
                    setTint(Color.RED)
                }
            }
            osmMapView.overlays.add(marker)
            remotePlayerMarkers[playerId] = marker
        } else {
            marker.position = position
        }

        osmMapView.invalidate()
    }

    private fun removeRemotePlayer(playerId: String) {
        remotePlayerMarkers[playerId]?.let {
            osmMapView.overlays.remove(it)
            remotePlayerMarkers.remove(playerId)
            osmMapView.invalidate()
        }
    }

    // Lifecycle methods
    override fun onResume() {
        super.onResume()
        osmMapView.onResume()
        myLocationOverlay?.enableMyLocation()
        compassOverlay?.enableCompass()
    }

    override fun onPause() {
        super.onPause()
        osmMapView.onPause()
        myLocationOverlay?.disableMyLocation()
        compassOverlay?.disableCompass()
        stopMovement()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMovement()
        serverConnectionManager.disconnect()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes if needed
    }

    enum class Direction {
        NORTH, SOUTH, EAST, WEST
    }
}