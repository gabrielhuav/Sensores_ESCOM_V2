package ovh.gabrielhuav.sensores_escom_v2

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*
import android.Manifest

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

        // Botón para cambiar idioma
        val languageButton = findViewById<Button>(R.id.btnChangeLanguage)
        languageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }
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

    private fun showLanguageSelectionDialog() {
        val languages = resources.getStringArray(R.array.language_names) // Obtiene los nombres localizados
        val locales = arrayOf("es", "en", "ru") // Mapeo de idiomas

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_title_select_language)) // Título localizado
            .setItems(languages) { _, which ->
                setLocale(locales[which]) // Cambia el idioma según la selección
            }
            .create()
        dialog.show()
    }



    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Reinicia la actividad para aplicar el idioma
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Finaliza la actividad actual
    }


    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 101
    }
}
