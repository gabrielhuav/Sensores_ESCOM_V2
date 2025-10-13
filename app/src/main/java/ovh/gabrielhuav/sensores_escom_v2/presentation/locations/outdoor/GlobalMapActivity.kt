package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.data.map.OnlineServer.OnlineServerManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.ServerConnectionManager
import java.util.UUID

class GlobalMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var playerMarker: Marker
    private lateinit var currentLocation: GeoPoint
    private lateinit var serverConnectionManager: ServerConnectionManager

    private lateinit var playerId: String

    private val MOVEMENT_STEP = 0.0001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_map)


        // Se obtiene el ID (nombre) del Intent.
        // Si no se pasa, se usa uno aleatorio como respaldo seguro.
        playerId = intent.getStringExtra("PLAYER_NAME") ?: "Player_${UUID.randomUUID()}"
        if (playerId.isBlank()) {
            Toast.makeText(this, "Nombre de jugador no válido, usando ID temporal.", Toast.LENGTH_SHORT).show()
            playerId = "Player_${UUID.randomUUID()}"
        }

        // osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Set initial location
        val startPoint = GeoPoint(19.5049291, -99.1466950)
        currentLocation = startPoint.clone()

        // Center map and set zoom
        val mapController = mapView.controller
        mapController.setZoom(19.5)
        mapController.setCenter(startPoint)

        initPlayerMarker()
        setupMovementButtons()

        // Initialize and connect to the server using ServerConnectionManager
        val onlineServerManager = OnlineServerManager.getInstance(this)
        serverConnectionManager = ServerConnectionManager(this, onlineServerManager)
        serverConnectionManager.connectToServer { success ->
            runOnUiThread {
                if (success) {
                    serverConnectionManager.onlineServerManager.sendJoinMessage(playerId)
                    Toast.makeText(this, "Conectado al servidor.", Toast.LENGTH_SHORT).show()
                    Toast.makeText(this, "Conectado al servidor.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Fallo al conectar con el servidor.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initPlayerMarker() {
        playerMarker = Marker(mapView)
        playerMarker.position = currentLocation
        playerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(playerMarker)
        mapView.invalidate()
    }

    private fun setupMovementButtons() {
        findViewById<Button>(R.id.button_north).setOnClickListener { movePlayer(0.0, MOVEMENT_STEP) }
        findViewById<Button>(R.id.button_south).setOnClickListener { movePlayer(0.0, -MOVEMENT_STEP) }
        findViewById<Button>(R.id.button_east).setOnClickListener { movePlayer(MOVEMENT_STEP, 0.0) }
        findViewById<Button>(R.id.button_west).setOnClickListener { movePlayer(-MOVEMENT_STEP, 0.0) }
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
        if (::serverConnectionManager.isInitialized) {
            serverConnectionManager.disconnect()
        }
    }
}
