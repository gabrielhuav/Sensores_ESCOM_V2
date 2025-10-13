package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.base.GameplayActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import java.util.UUID

class GlobalMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var playerMarker: Marker
    private lateinit var returnMarker: Marker
    private lateinit var currentLocation: GeoPoint
    private lateinit var serverConnectionManager: ServerConnectionManager

    private lateinit var playerId: String
    private var isServer: Boolean = false
    private var previousPosition: Pair<Int, Int>? = null
    private lateinit var entryPoint: GeoPoint

    private val MOVEMENT_STEP = 0.0001
    private val RETURN_DISTANCE_THRESHOLD = 10.0 // Meters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_map)

        // Get player ID from Intent
        playerId = intent.getStringExtra("PLAYER_NAME") ?: "Player_${UUID.randomUUID()}"
        if (playerId.isBlank()) {
            Toast.makeText(this, "Invalid player name, using temporary ID.", Toast.LENGTH_SHORT).show()
            playerId = "Player_${UUID.randomUUID()}"
        }

        isServer = intent.getBooleanExtra("IS_SERVER", false)

        // Get the position from the previous map to know where to return
        @Suppress("UNCHECKED_CAST")
        previousPosition = intent.getSerializableExtra("PREVIOUS_POSITION") as? Pair<Int, Int>

        // Get the initial position for this map, this is our entry/exit point
        @Suppress("UNCHECKED_CAST")
        val initialPosPair = intent.getSerializableExtra("INITIAL_POSITION") as? Pair<Int, Int>

        val startPoint = if (initialPosPair != null) {
            GeoPoint(initialPosPair.second / 1e6, initialPosPair.first / 1e6)
        } else {
            // Fallback to a default location if no position is provided
            GeoPoint(19.5049291, -99.1466950)
        }
        entryPoint = startPoint.clone()
        currentLocation = startPoint.clone()


        // osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Center map and set zoom
        val mapController = mapView.controller
        mapController.setZoom(19.5)
        mapController.setCenter(startPoint)

        initReturnPointMarker()  // Inicializar primero el punto de retorno
        initPlayerMarker()        // Luego el jugador (para que quede encima)
        setupMovementButtons()
        setupInteractionButton()

        // Initialize and connect to the server
        val onlineServerManager = OnlineServerManager.getInstance(this)
        serverConnectionManager = ServerConnectionManager(this, onlineServerManager)
        serverConnectionManager.connectToServer { success ->
            runOnUiThread {
                if (success) {
                    serverConnectionManager.onlineServerManager.apply {
                        sendJoinMessage(playerId)
                        // Solicitar posiciones actuales
                        requestPositionsUpdate()
                    }
                    Toast.makeText(this, "Connected to server.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to connect to server.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initPlayerMarker() {
        playerMarker = Marker(mapView)
        playerMarker.position = currentLocation
        playerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        playerMarker.title = "You"

        // Color VERDE BRILLANTE para el jugador
        playerMarker.icon?.setColorFilter(Color.rgb(0, 255, 0), PorterDuff.Mode.SRC_ATOP)

        mapView.overlays.add(playerMarker)
        mapView.invalidate()
    }

    private fun initReturnPointMarker() {
        returnMarker = Marker(mapView)
        returnMarker.position = entryPoint
        returnMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        returnMarker.title = "Return Point"

        // Color ROJO para el punto de retorno (más visible que azul)
        returnMarker.icon?.setColorFilter(Color.rgb(255, 0, 0), PorterDuff.Mode.SRC_ATOP)

        mapView.overlays.add(returnMarker)
    }

    private fun setupMovementButtons() {
        findViewById<Button>(R.id.button_north).setOnClickListener { movePlayer(0.0, MOVEMENT_STEP) }
        findViewById<Button>(R.id.button_south).setOnClickListener { movePlayer(0.0, -MOVEMENT_STEP) }
        findViewById<Button>(R.id.button_east).setOnClickListener { movePlayer(MOVEMENT_STEP, 0.0) }
        findViewById<Button>(R.id.button_west).setOnClickListener { movePlayer(-MOVEMENT_STEP, 0.0) }
    }

    private fun setupInteractionButton() {
        findViewById<Button>(R.id.button_a).setOnClickListener {
            handleInteraction()
        }
    }

    private fun handleInteraction() {
        // Check if the player is at the entry point to go back
        if (currentLocation.distanceToAsDouble(entryPoint) < RETURN_DISTANCE_THRESHOLD) {
            if (previousPosition != null) {
                returnToGameplayActivity()
            } else {
                Toast.makeText(this, "No previous map to return to.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleJoinMessage(jsonObject: JSONObject) {
        val newPlayerId = jsonObject.getString("id")
        Log.d(TAG, "Player joined: $newPlayerId")
        serverConnectionManager.onlineServerManager.requestPositionsUpdate()
    }
    private fun returnToGameplayActivity() {
        val intent = Intent(this, GameplayActivity::class.java).apply {
            putExtra("PLAYER_NAME", playerId)
            putExtra("IS_SERVER", isServer)
            putExtra("INITIAL_POSITION", previousPosition) // Use the position from the previous map
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }


    private fun movePlayer(lonDelta: Double, latDelta: Double) {
        currentLocation.latitude += latDelta
        currentLocation.longitude += lonDelta

        playerMarker.position = currentLocation
        mapView.invalidate()
        mapView.controller.animateTo(currentLocation)

        val x = (currentLocation.longitude * 1e6).toInt()
        val y = (currentLocation.latitude * 1e6).toInt()

        if (::serverConnectionManager.isInitialized && serverConnectionManager.isConnected()) {
            serverConnectionManager.sendUpdateMessage(
                playerId,
                Pair(x, y),
                "global"
            )
        }

        checkReturnPossibility()
    }

    private fun checkReturnPossibility() {
        if (currentLocation.distanceToAsDouble(entryPoint) < RETURN_DISTANCE_THRESHOLD) {
            if (previousPosition != null) {
                Toast.makeText(this, "Press A to return to previous map", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        private const val TAG = "GameplayActivity"
    }
}