package ovh.gabrielhuav.sensores_escom_v2

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class SinglePlayerActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_player)

        // Initialize UI components
        val mapContainer: FrameLayout = findViewById(R.id.mapContainer)
        val btnZoomIn: Button = findViewById(R.id.btnZoomIn)
        val btnZoomOut: Button = findViewById(R.id.btnZoomOut)
        val btnNorth: Button = findViewById(R.id.btnNorth)
        val btnSouth: Button = findViewById(R.id.btnSouth)
        val btnEast: Button = findViewById(R.id.btnEast)
        val btnWest: Button = findViewById(R.id.btnWest)
        val tvPlayerPosition: TextView = findViewById(R.id.tvPlayerPosition)

        // Create MapView and add to container
        mapView = MapView(this)
        mapContainer.addView(mapView)

        // Observe player position changes
        gameViewModel.playerPosition.observe(this) { position ->
            // Safely update player position and TextView
            position?.let {
                mapView.updatePlayerPosition(it)
                tvPlayerPosition.text = "Posición: (${it.first}, ${it.second})"
            }
        }

        // Configure movement buttons
        setupMovementButtons(btnNorth, btnSouth, btnEast, btnWest)

        // Configure zoom buttons
        setupZoomButtons(btnZoomIn, btnZoomOut)
    }

    private fun setupMovementButtons(
        btnNorth: Button,
        btnSouth: Button,
        btnEast: Button,
        btnWest: Button
    ) {
        btnNorth.setOnClickListener { gameViewModel.moveNorth() }
        btnSouth.setOnClickListener { gameViewModel.moveSouth() }
        btnEast.setOnClickListener { gameViewModel.moveEast() }
        btnWest.setOnClickListener { gameViewModel.moveWest() }
    }

    private fun setupZoomButtons(btnZoomIn: Button, btnZoomOut: Button) {
        btnZoomIn.setOnClickListener {
            mapView.updateScaleFactor(mapView.scaleFactor + 0.2f)
        }
        btnZoomOut.setOnClickListener {
            mapView.updateScaleFactor(mapView.scaleFactor - 0.2f)
        }
    }
}