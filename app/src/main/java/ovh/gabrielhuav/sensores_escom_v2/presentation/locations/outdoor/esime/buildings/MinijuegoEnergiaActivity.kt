package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.esime.buildings

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import ovh.gabrielhuav.sensores_escom_v2.R

class MinijuegoEnergiaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Conectamos con el diseño que acabamos de hacer
        setContentView(R.layout.activity_minijuego_energia)

        val etPin = findViewById<EditText>(R.id.etPin)
        val btnRestaurar = findViewById<MaterialButton>(R.id.btnRestaurar)
        val btnCancelar = findViewById<MaterialButton>(R.id.btnCancelar)
        val tvPista = findViewById<TextView>(R.id.tvPista)

        // Recibimos el dato para saber si el jugador ya leyó el pizarrón en el Aula 7
        val conoceContrasena = intent.getBooleanExtra("CONOCE_CONTRASENA", false)
        if (conoceContrasena) {
            tvPista.text = "Ingresa el PIN de 4 dígitos (Pista: 1936):"
        }

        btnRestaurar.setOnClickListener {
            val pinIngresado = etPin.text.toString()

            if (pinIngresado == "1936") {
                Toast.makeText(this, "✅ ¡ENERGÍA RESTAURADA CON ÉXITO!", Toast.LENGTH_LONG).show()
                // Aquí podrías enviar un resultado de vuelta a Edificio3Activity si quisieras
                // Por ahora, solo cerramos y regresamos al mapa
                finish()
            } else {
                Toast.makeText(this, "❌ PIN INCORRECTO. ACCESO DENEGADO.", Toast.LENGTH_SHORT).show()
                etPin.text.clear() // Borra el texto para que intente de nuevo
            }
        }

        btnCancelar.setOnClickListener {
            Toast.makeText(this, "Operación cancelada", Toast.LENGTH_SHORT).show()
            finish() // Regresa al mapa sin hacer nada
        }
    }
}