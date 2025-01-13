
package ovh.gabrielhuav.sensores_escom_v2.QRScanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.squareup.picasso.Picasso
import ovh.gabrielhuav.sensores_escom_v2.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRCodeScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView  // Usando PreviewView ahora
    private lateinit var cameraExecutor: ExecutorService
    private val barcodeScanner = BarcodeScanning.getClient()

    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true) {
                startCameraPreview()
            } else {
                Toast.makeText(this, "Se requieren permisos para usar la cámara", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_scanner)

        previewView = findViewById(R.id.scannerView)

        if (hasPermissions()) {
            startCameraPreview()
        } else {
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        requestPermissionsLauncher.launch(requiredPermissions)
    }

    private fun startCameraPreview() {
        Log.d("QRCodeScannerActivity", "Iniciando cámara...")

        if (!hasPermissions()) return

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetRotation(previewView.display.rotation)  // Usa la rotación de la vista
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, { imageProxy ->
                processImage(imageProxy)
            })

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)

                // Usa el surfaceProvider de PreviewView para CameraX
                preview.setSurfaceProvider(previewView.surfaceProvider)
            } catch (e: Exception) {
                Log.e("QRCodeScanner", "Error starting camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val barcode = barcodes.first()
                        val barcodeValue = barcode.displayValue
                        Log.d("QRCodeScanner", "Código QR detectado: $barcodeValue") // Mostrar en Log

                        Toast.makeText(this, "Código QR detectado: $barcodeValue", Toast.LENGTH_SHORT).show()
                        handleScanResult(barcodeValue ?: "")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QRCodeScanner", "Barcode scanning failed", e)
                    Toast.makeText(this, "Escaneo fallido, intenta de nuevo", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    Log.d("QRCodeScanner", "Escaneo completado")

                    imageProxy.close()
                }

        } else {
            imageProxy.close()
        }
    }


    private fun handleScanResult(result: String) {
        if (isValidUrl(result)) {
            // Si es una URL válida, abre en el navegador
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
            startActivity(intent)
        }  else if (isValidImageUrl(result)) {
            // Si es una URL de imagen, carga la imagen en un ImageView
            displayQRCodeImage(result)
        } else {
            // Si no es una URL de imagen, muestra el resultado o realiza otra acción
            Toast.makeText(this, "Código QR detectado: $result", Toast.LENGTH_SHORT).show()
        }
        val resultIntent = Intent()
        resultIntent.putExtra("scannedData", result)  // Puedes personalizar los datos
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun isValidImageUrl(url: String): Boolean {
        // Verifica si el enlace termina en una extensión común de imagen (jpg, png, etc.)
        return url.endsWith(".jpg", true) || url.endsWith(".png", true) || url.endsWith(".jpeg", true)
    }

    private fun isValidUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches() || url.matches("^(https?|ftp)://[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(:[0-9]{1,5})?(/.*)?$".toRegex())

    }

    private fun displayQRCodeImage(imageUrl: String) {
        val imageView: ImageView = findViewById(R.id.qrImageView)
        imageView.visibility = View.VISIBLE
        Picasso.get().load(imageUrl).into(imageView)
    }

    override fun onResume() {
        super.onResume()
        if (hasPermissions()) {
            if (!::cameraExecutor.isInitialized) {
                startCameraPreview()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cameraExecutor.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }



}