package ovh.gabrielhuav.sensores_escom_v2

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ovh.gabrielhuav.sensores_escom_v2.data.map.Bluetooth.BluetoothGameManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.DeviceListActivity
import ovh.gabrielhuav.sensores_escom_v2.presentation.components.GameplayActivity

class MainActivity : AppCompatActivity() {

    // Lista de permisos necesarios
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // Modificamos el método onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Verificar permisos, pero no forzar Bluetooth
        if (!hasPermissions()) {
            requestPermissions()
        }

        // Referencias a los elementos de la interfaz
        val btnStartGame = findViewById<Button>(R.id.btnStartGame)
        val btnBluetoothConnect = findViewById<Button>(R.id.btnBluetoothConnect)
        val etPlayerName = findViewById<EditText>(R.id.etPlayerName)

        // Botón para iniciar el juego como servidor
        btnStartGame.setOnClickListener {
            val playerName = etPlayerName.text.toString()
            if (playerName.isNotEmpty()) {
                // Inicia `GameplayActivity` como servidor
                val intent = Intent(this, GameplayActivity::class.java).apply {
                    putExtra("PLAYER_NAME", playerName)
                    putExtra("IS_SERVER", true) // Especifica que actuará como servidor
                    // Podríamos añadir un flag opcional para indicar que no queremos forzar Bluetooth
                    putExtra("FORCE_BLUETOOTH", false)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Por favor, ingrese su nombre.", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón para conectarse a un dispositivo Bluetooth
        // Este botón SÍ requerirá Bluetooth, ya que es explícito
        btnBluetoothConnect.setOnClickListener {
            val playerName = etPlayerName.text.toString()
            if (playerName.isNotEmpty()) {
                // Verificar si Bluetooth está disponible
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    Toast.makeText(this, "Este dispositivo no soporta Bluetooth.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Verificar si Bluetooth está activado
                if (!bluetoothAdapter.isEnabled) {
                    // Intento de activar Bluetooth
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    // Abrir la lista de dispositivos Bluetooth
                    val intent = Intent(this, DeviceListActivity::class.java).apply {
                        putExtra("PLAYER_NAME", playerName) // Envía el nombre del jugador
                    }
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Por favor, ingrese su nombre antes de conectarse por Bluetooth.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Verifica si los permisos ya han sido otorgados.
     */
    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Solicita los permisos necesarios al usuario.
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS_CODE)
    }

    /**
     * Maneja los resultados de la solicitud de permisos.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(this, "Debe otorgar los permisos para continuar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Constante para el request code
    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 101
        private const val REQUEST_ENABLE_BT = 102
    }

    // Manejar el resultado de la solicitud de activación Bluetooth
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth activado, abrir la lista de dispositivos
                val playerName = findViewById<EditText>(R.id.etPlayerName).text.toString()
                val intent = Intent(this, DeviceListActivity::class.java).apply {
                    putExtra("PLAYER_NAME", playerName)
                }
                startActivity(intent)
            } else {
                // El usuario no activó Bluetooth
                Toast.makeText(this, "Se requiere Bluetooth para conectarse a otros dispositivos.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

}
