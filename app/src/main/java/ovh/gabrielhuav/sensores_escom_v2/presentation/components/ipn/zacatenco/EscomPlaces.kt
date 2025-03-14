package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ovh.gabrielhuav.sensores_escom_v2.R

class EscomPlaces : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_escom_places)

        val place: String = intent.extras?.getString("PLACE").orEmpty()

        // Encuentra el ConstraintLayout en el layout
        val constraintLayout = findViewById<ConstraintLayout>(R.id.main)

        // Cambia el fondo dependiendo del valor de "PLACE"
        when (place) {
            "queso" -> constraintLayout.setBackgroundResource(R.drawable.queso)
            "planetario" -> constraintLayout.setBackgroundResource(R.drawable.planetario)
            // Añade más casos según sea necesario
            else -> constraintLayout.setBackgroundResource(R.drawable.queso)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}