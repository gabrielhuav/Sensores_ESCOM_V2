package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import ovh.gabrielhuav.sensores_escom_v2.R

class GlobalMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var playerMarker: Marker
    private lateinit var currentLocation: GeoPoint

    // Define how much the player moves with each button press
    private val MOVEMENT_STEP = 0.0001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_map)

        // osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        mapView = findViewById(R.id.map) // Corrected ID from previous step
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
    }

    private fun initPlayerMarker() {
        playerMarker = Marker(mapView)
        playerMarker.position = currentLocation
        playerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        // You can customize the marker icon later, e.g.:
        // playerMarker.icon = resources.getDrawable(R.drawable.ic_player_marker, theme)
        mapView.overlays.add(playerMarker)
        mapView.invalidate() // Refresh the map
    }

    private fun setupMovementButtons() {
        findViewById<Button>(R.id.button_north).setOnClickListener { movePlayer(0.0, MOVEMENT_STEP) }
        findViewById<Button>(R.id.button_south).setOnClickListener { movePlayer(0.0, -MOVEMENT_STEP) }
        findViewById<Button>(R.id.button_east).setOnClickListener { movePlayer(MOVEMENT_STEP, 0.0) }
        findViewById<Button>(R.id.button_west).setOnClickListener { movePlayer(-MOVEMENT_STEP, 0.0) }
    }

    private fun movePlayer(lonDelta: Double, latDelta: Double) {
        // Update current location
        currentLocation.latitude += latDelta
        currentLocation.longitude += lonDelta

        // Update marker position on the map
        playerMarker.position = currentLocation
        mapView.invalidate() // Redraw the map to show the new marker position

        // Animate camera to the new position to keep the player centered
        mapView.controller.animateTo(currentLocation)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}