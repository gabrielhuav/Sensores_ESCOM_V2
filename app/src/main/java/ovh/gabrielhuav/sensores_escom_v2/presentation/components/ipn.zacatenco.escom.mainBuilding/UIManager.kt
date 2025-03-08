package ovh.gabrielhuav.sensores_escom_v2.presentation.components.mapview

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import ovh.gabrielhuav.sensores_escom_v2.R

// UIManager.kt
class UIManager(
    private val rootView: View,
    private val mapView: MapView
) {
    // Botones y vistas
    val btnStartServer: Button = rootView.findViewById(R.id.button_small_1)
    val btnConnectDevice: Button = rootView.findViewById(R.id.button_small_2)
    val btnNorth: Button = rootView.findViewById(R.id.button_north)
    val btnSouth: Button = rootView.findViewById(R.id.button_south)
    val btnEast: Button = rootView.findViewById(R.id.button_east)
    val btnWest: Button = rootView.findViewById(R.id.button_west)
    val tvBluetoothStatus: TextView = rootView.findViewById(R.id.tvBluetoothStatus)
    val btnOnlineServer: Button = rootView.findViewById(R.id.button_serverOnline)
    val buttonA: Button = rootView.findViewById(R.id.button_a)

    // Handler para asegurar las actualizaciones de UI en el hilo principal
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initializeViews() {
        mapView.setBluetoothServerMode(false)
    }

    fun setupMovementButtons(movementManager: MovementManager) {
        btnNorth.setOnTouchListener { _, event ->
            movementManager.handleMovement(event, 0, -1)
            true
        }
        btnSouth.setOnTouchListener { _, event ->
            movementManager.handleMovement(event, 0, 1)
            true
        }
        btnEast.setOnTouchListener { _, event ->
            movementManager.handleMovement(event, 1, 0)
            true
        }
        btnWest.setOnTouchListener { _, event ->
            movementManager.handleMovement(event, -1, 0)
            true
        }
    }

    // Método para cuando el usuario activa manualmente el Bluetooth después
    fun onBluetoothEnabled() {
        mainHandler.post {
            btnStartServer.text = "B1"
            btnStartServer.alpha = 1.0f
            btnStartServer.isEnabled = true

            tvBluetoothStatus.text = "Bluetooth activado - Listo para usar"
        }
    }

    fun setupInteractionButton(
        playerPosition: () -> Pair<Int, Int>,
        onInteract: (Int, Int) -> Unit
    ) {
        buttonA.setOnClickListener {
            val (x, y) = playerPosition()
            if (mapView.isInteractivePosition(x, y)) {
                onInteract(x, y)
            } else {
                showToast("No puedes interactuar aquí")
            }
        }
    }

    fun configureForServerMode() {
        mainHandler.post {
            btnConnectDevice.visibility = View.GONE
            btnStartServer.isEnabled = true
        }
    }

    fun configureForClientMode() {
        mainHandler.post {
            btnConnectDevice.visibility = View.GONE
            btnStartServer.visibility = View.GONE
        }
    }

    fun configureForOnlineOnlyMode() {
        mainHandler.post {
            // Hacer que el botón de servidor indique que se puede activar Bluetooth
            btnStartServer.text = "BT Off"
            btnStartServer.alpha = 0.5f

            // Ocultar botón de conectar dispositivo si no es relevante
            btnConnectDevice.visibility = View.GONE

            // Actualizar el texto de estado
            tvBluetoothStatus.text = "Modo online - Bluetooth desactivado"
        }
    }

    fun showToast(message: String) {
        // Los Toast ya se manejan internamente en el hilo principal
        Toast.makeText(rootView.context, message, Toast.LENGTH_SHORT).show()
    }

    fun updateBluetoothStatus(status: String, isBluetoothEnabled: Boolean = true) {
        // Asegurarse de que esta actualización de UI siempre ocurra en el hilo principal
        mainHandler.post {
            tvBluetoothStatus.text = status

            // Actualizar la apariencia del botón de servidor según el estado del Bluetooth
            if (!isBluetoothEnabled) {
                btnStartServer.alpha = 0.5f  // Hacer el botón semitransparente si Bluetooth está apagado
                btnStartServer.isEnabled = false
                btnStartServer.text = "BT Off"
            } else {
                btnStartServer.alpha = 1.0f  // Botón normal si Bluetooth está encendido
                btnStartServer.isEnabled = true
                btnStartServer.text = "B1"
            }
        }
    }
}