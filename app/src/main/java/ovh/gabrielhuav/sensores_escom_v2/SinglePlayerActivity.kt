package ovh.gabrielhuav.sensores_escom_v2

import android.content.Intent
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

        // Inicializar componentes UI
        val mapContainer: FrameLayout = findViewById(R.id.mapContainer)
        val btnZoomIn: Button = findViewById(R.id.btnZoomIn)
        val btnZoomOut: Button = findViewById(R.id.btnZoomOut)
        val btnNorth: Button = findViewById(R.id.btnNorth)
        val btnSouth: Button = findViewById(R.id.btnSouth)
        val btnEast: Button = findViewById(R.id.btnEast)
        val btnWest: Button = findViewById(R.id.btnWest)
        val tvPlayerPosition: TextView = findViewById(R.id.tvPlayerPosition)
        val btnBackToMenu: Button = findViewById(R.id.btnBackToMenu)

        // Crear MapView y agregarlo al contenedor
        mapView = MapView(this)
        mapContainer.addView(mapView)

        // Observar los cambios en la posición del jugador
        gameViewModel.playerPosition.observe(this) { position ->
            position?.let {
                mapView.updateLocalPlayerPosition(it)
                tvPlayerPosition.text = "Posición: (${it.first}, ${it.second})"
            }
        }

        // Configurar botones de movimiento
        setupMovementButtons(btnNorth, btnSouth, btnEast, btnWest)

        // Configurar botones de zoom
        setupZoomButtons(btnZoomIn, btnZoomOut)

        // Configurar el botón de regresar al menú
        btnBackToMenu.setOnClickListener {
            navigateToMainMenu()
        }
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

    private fun navigateToMainMenu() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Finalizar la actividad actual para evitar volver con el botón "Atrás"
    }
}
