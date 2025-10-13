package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.util.Log
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.databinding.ActivityPlazaTorresN1Binding // <-- ¡IMPORTANTE!
import ovh.gabrielhuav.sensores_escom_v2.domain.bluetooth.BluetoothManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.components.UIManager

class PlazaTorresN1 : AppCompatActivity(), BluetoothManager.BluetoothManagerCallback {

    // 1. GESTORES PRINCIPALES
    private lateinit var binding: ActivityPlazaTorresN1Binding // Para acceder a los botones del XML
    private lateinit var bluetoothManager: BluetoothManager      // Para manejar la conexión Bluetooth
    private lateinit var uiManager: UIManager                    // Para manejar la UI (opcional pero bueno)

    // para saber si estamos conectados al robot
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 2. ENLAZAR EL XML CON EL CÓDIGO KOTLIN
        binding = ActivityPlazaTorresN1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. INICIALIZAR COMPONENTES
        initializeComponents()

        // 4. CONFIGURAR LOS BOTONES
        setupButtonListeners()
    }

    private fun initializeComponents() {
        // Inicializa el gestor de UI pasándole las vistas que necesita
        uiManager = UIManager(binding.root, binding.tvBluetoothStatus).apply {
            initializeViews()
        }

        // Obtiene la instancia única del BluetoothManager
        bluetoothManager = BluetoothManager.getInstance(this, binding.tvBluetoothStatus)
        bluetoothManager.setCallback(this) // Nos registramos para recibir eventos (conectado, desconectado, etc.)

        // Obtenemos el dispositivo Bluetooth al que nos conectamos en la pantalla anterior
        val selectedDevice = intent.getParcelableExtra<BluetoothDevice>("SELECTED_DEVICE")

        if (selectedDevice != null) {
            // Si hay un dispositivo, intentamos conectarnos a él
            bluetoothManager.connectToDevice(selectedDevice)
        } else {
            // Si no, mostramos un error
            uiManager.updateBluetoothStatus("Error: No se recibió dispositivo")
            Toast.makeText(this, "No se seleccionó un dispositivo Bluetooth.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtonListeners() {
        // Asignamos una acción a cada botón
        // Cuando se toca un botón, se llama a bluetoothManager.sendMessage()

        // --- BOTONES DE MOVIMIENTO ---
        // Usamos setOnTouchListener para poder enviar comandos mientras se mantiene presionado (opcional, pero mejor)
        binding.buttonNorth.setOnClickListener { sendMessage("N") }
        binding.buttonSouth.setOnClickListener { sendMessage("S") }
        binding.buttonEast.setOnClickListener { sendMessage("E") }
        binding.buttonWest.setOnClickListener { sendMessage("W") }

        // --- BOTONES DE ACCIÓN ---
        binding.buttonA.setOnClickListener { sendMessage("A") } // Comando para acción 'A'
        binding.buttonSmall1.setOnClickListener { sendMessage("B1") } // Comando para 'B1'
        binding.buttonSmall2.setOnClickListener { sendMessage("BCK") } // Comando para 'BCK'

        // El botón de Home debe regresar a la pantalla anterior
        binding.buttonBackToHome.setOnClickListener {
            finish() // Cierra esta Activity y regresa a la anterior
        }


        binding.buttonServerOnline.visibility = View.GONE

    // Función centralizada para enviar mensajes al robot
    private fun sendMessage(message: String) {
        if (isConnected) {
            bluetoothManager.sendMessage(message)
            Log.d("PlazaTorresN1", "Enviando comando: $message")
        } else {
            Toast.makeText(this, "No estás conectado a un dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    // --- IMPLEMENTACIÓN DE LOS MÉTODOS DEL BLUETOOTH MANAGER ---
    // Estas funciones son llamadas automáticamente por el BluetoothManager cuando algo sucede

    override fun onBluetoothDeviceConnected(device: BluetoothDevice) {
        isConnected = true
        // Chequeo de permisos (Android lo requiere)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            uiManager.updateBluetoothStatus("Conectado a: ${device.name}")
        }
    }

    override fun onBluetoothConnectionFailed(error: String) {
        isConnected = false
        uiManager.updateBluetoothStatus("Error de conexión: $error")
        Toast.makeText(this, "Fallo en la conexión: $error", Toast.LENGTH_SHORT).show()
    }

    override fun onPositionReceived(device: BluetoothDevice, x: Int, y: Int) {
        // Esta función se llama si el robot envía su posición.
        // Como tu compañero se encargará de la matriz, puedes dejarla vacía por ahora.
        Log.d("PlazaTorresN1", "Posición recibida: ($x, $y)")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Es MUY importante limpiar la conexión al cerrar la pantalla para no dejarla abierta
        bluetoothManager.cleanup()
    }

    companion object {
        private const val TAG = "PlazaTorresN1"
    }
}
