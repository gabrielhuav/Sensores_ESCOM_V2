package ovh.gabrielhuav.sensores_escom_v2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

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

    private var playerColor: Int = Color.RED // Color inicial del jugador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Verificar permisos
        if (!hasPermissions()) {
            requestPermissions()
        }

        // Botón para el modo de un jugador
        val singlePlayerButton = findViewById<Button>(R.id.btnSinglePlayer)
        singlePlayerButton.setOnClickListener {
            val intent = Intent(this, SinglePlayerActivity::class.java)
            intent.putExtra("PLAYER_COLOR", playerColor) // Pasar el color al modo un jugador
            startActivity(intent)
        }

        // Botón para el modo Bluetooth
        val bluetoothButton = findViewById<Button>(R.id.btnBluetooth)
        bluetoothButton.setOnClickListener {
            if (hasPermissions()) {
                val intent = Intent(this, BluetoothActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Se requieren permisos para usar Bluetooth.", Toast.LENGTH_SHORT).show()
                requestPermissions()
            }
        }

        // Botón para cambiar color
        val changeColorButton = findViewById<Button>(R.id.btnChangeColor)
        changeColorButton.setOnClickListener {
            showColorPickerDialog()
        }
    }

    // Mostrar el diálogo de selección de color
    private fun showColorPickerDialog() {
        val colors = arrayOf("Rojo", "Azul", "Verde", "Amarillo", "Morado")
        val colorValues = arrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Elige un color")
        builder.setItems(colors) { _, which ->
            // Cambiar el color del jugador al seleccionar una opción
            playerColor = colorValues[which]
            Toast.makeText(this, "Color cambiado a ${colors[which]}", Toast.LENGTH_SHORT).show()
        }
        builder.create().show()
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS_CODE)
    }

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

    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 101
    }
}
