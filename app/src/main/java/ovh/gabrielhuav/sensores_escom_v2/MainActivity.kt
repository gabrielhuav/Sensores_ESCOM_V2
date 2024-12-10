
package ovh.gabrielhuav.sensores_escom_v2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), MapView.OnMapTouchListener {

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA, // Permiso para la cámara
        Manifest.permission.BLUETOOTH, // Permiso para Bluetooth
        Manifest.permission.BLUETOOTH_ADMIN, // Permiso para administrar Bluetooth
        Manifest.permission.BLUETOOTH_SCAN, // Permiso para escanear dispositivos Bluetooth
        Manifest.permission.BLUETOOTH_CONNECT, // Permiso para conectar con dispositivos Bluetooth
        Manifest.permission.ACCESS_FINE_LOCATION // Permiso para ubicación (necesario para Bluetooth en algunas versiones de Android)
    )

    private val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Permisos otorgados.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Debe otorgar los permisos para continuar.", Toast.LENGTH_SHORT).show()
        }
    }
    // Registro para iniciar el escáner QR
    private val startQRCodeScannerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val scannedResult = data?.getStringExtra("SCAN_RESULT")
            scannedResult?.let {
                // Mostrar la imagen del QR escaneado
                Toast.makeText(this, "Código QR escaneado: $it", Toast.LENGTH_LONG).show()
                // Aquí podrías agregar lógica para mostrar la imagen
                displayQRCodeImage(it)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
// Verificar y solicitar permisos
        if (hasPermissions()) {
            Toast.makeText(this, "Permisos ya otorgados.", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions() // Solicitar permisos si no los tiene
        }

        // Botón para el modo de un jugador
        val singlePlayerButton = findViewById<Button>(R.id.btnSinglePlayer)
        singlePlayerButton.setOnClickListener {
            val intent = Intent(this, SinglePlayerActivity::class.java)
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
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_PERMISSIONS_CODE)
        requestPermissionsLauncher.launch(requiredPermissions)

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
    private fun startQRCodeScanner() {
        val scannerIntent = Intent(this, QRCodeScannerActivity::class.java)
        startQRCodeScannerLauncher.launch(scannerIntent)
    }

    // Listener de MapView para cuando se toque el mapa y se quiera iniciar el escáner QR
    override fun onMapTouched() {
        launchQRCodeScanner()
    }

    // Lanza el escáner QR al tocar el mapa
    private fun launchQRCodeScanner() {
        val scannerIntent = Intent(this, QRCodeScannerActivity::class.java)
        startQRCodeScannerLauncher.launch(scannerIntent)
    }
    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 101
    }
        // Mostrar la imagen obtenida del código QR en un ImageView
        private fun displayQRCodeImage(imageUrl: String) {
            // Asegúrate de tener un ImageView adecuado en tu layout para mostrar la imagen
            val imageView: ImageView = findViewById(R.id.qrImageView) // Asegúrate de usar un id correcto en tu layout XML
            Picasso.get().load(imageUrl).into(imageView)

    }
}
