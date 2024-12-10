package ovh.gabrielhuav.sensores_escom_v2

import ovh.gabrielhuav.sensores_escom_v2.MapView
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
    private val qrCells = listOf(Pair(16,4), Pair(11, 9), Pair(17, 17),Pair(2,17),Pair(5,9))

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
        val btnQrScanner: Button = findViewById(R.id.btnQrScanner)
        // Crear MapView y agregarlo al contenedor
        mapView = MapView(this)
        mapContainer.addView(mapView)
        mapView.setOnMapTouchListener(object : MapView.OnMapTouchListener {
            override fun onMapTouched() {
                // Activar el escáner QR
                val intent = Intent(this@SinglePlayerActivity, QRCodeScannerActivity::class.java)
                startActivityForResult(intent, QR_CODE_REQUEST)
            }
        })
        // Observar los cambios en la posición del jugador
        gameViewModel.playerPosition.observe(this) { position ->
            position?.let {
                mapView.updateLocalPlayerPosition(it)
                tvPlayerPosition.text = "Posición: (${it.first}, ${it.second})"
                toggleQrScannerButton(it)
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

        //Boton para abrir el scanner
        btnQrScanner.setOnClickListener {
            val intent = Intent(this, QRCodeScannerActivity::class.java)
            startActivityForResult(intent, QR_CODE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == QR_CODE_REQUEST && resultCode == RESULT_OK) {
            val scannedData = data?.getStringExtra("scannedData")
            scannedData?.let {
                // Aquí puedes actualizar la posición del jugador con la data escaneada
                // Suponiendo que el código QR contiene una posición o datos relevantes
                val newPosition = parseQRData(it)
                mapView.updateLocalPlayerPosition(newPosition)
            }
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
    private fun toggleQrScannerButton(playerPosition: Pair<Int, Int>) {
        val btnQrScanner: Button = findViewById(R.id.btnQrScanner)

        // Verificar si la posición del jugador está en las coordenadas válidas para activar el escáner QR
        if (qrCells.contains(playerPosition)) {
            btnQrScanner.isEnabled = true // Habilitar el botón
        } else {
            btnQrScanner.isEnabled = false // Deshabilitar el botón
        }
    }

    private fun parseQRData(data: String): Pair<Int, Int> {
        val coordinates = data.split(",")
        val x = coordinates.getOrNull(0)?.toIntOrNull() ?: 0
        val y = coordinates.getOrNull(1)?.toIntOrNull() ?: 0
        return Pair(x, y)
    }
    companion object {
        private const val QR_CODE_REQUEST = 1
    }


}
