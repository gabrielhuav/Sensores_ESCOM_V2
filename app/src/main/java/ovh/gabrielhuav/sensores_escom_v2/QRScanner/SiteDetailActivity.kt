package ovh.gabrielhuav.sensores_escom_v2.QRScanner

import android.Manifest // Import correcto para los permisos
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ovh.gabrielhuav.sensores_escom_v2.R

class SiteDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_detail)

        // Obtener los datos pasados desde la actividad principal
        val imageResId = intent.getIntExtra("IMAGE_RES_ID", R.drawable.logoescom)

        // Configurar la imagen
        val imageView = findViewById<ImageView>(R.id.imageView_site)
        imageView.setImageResource(imageResId)

        // Configurar el botón para abrir el escáner QR
        val buttonOpenScanner = findViewById<ImageButton>(R.id.button_open_scanner)
        buttonOpenScanner.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {

                openScanner()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Muestra una explicación al usuario antes de solicitar el permiso
                Toast.makeText(this, "Se necesita acceso a la cámara para escanear códigos QR", Toast.LENGTH_LONG).show()
                requestCameraPermission()
            }
            else -> {
                // Solicitar el permiso directamente
                requestCameraPermission()
            }
        }
    }

    // Lanzador de permisos
    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permiso concedido, abre el escáner
            openScanner()
        } else {
            // Permiso denegado, muestra un mensaje
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Solicitar permiso de la cámara
    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Abre la actividad del escáner QR
    private fun openScanner() {
        val intent = Intent(this, QRCodeScannerActivity::class.java)
        startActivity(intent)
        Log.d("SiteDetail", "Iniciando cámara...")

    }
}
