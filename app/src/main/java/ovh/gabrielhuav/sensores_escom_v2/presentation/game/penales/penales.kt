package ovh.gabrielhuav.sensores_escom_v2.presentation.game.penales

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.R
import kotlin.random.Random

class PenalesActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var txtResultado: TextView
    private lateinit var imgPortero: ImageView
    private lateinit var imgBalon: ImageView

    private var goles = 0
    private var tiros = 0
    private val maxTiros = 7

    private var balonInicialX = 0f
    private var balonInicialY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.penales)

        txtResultado = findViewById(R.id.txtResultado)
        imgPortero = findViewById(R.id.imgPortero)
        imgBalon = findViewById(R.id.imgBalon)

        imgBalon.post {
            balonInicialX = imgBalon.x
            balonInicialY = imgBalon.y
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {

                // 🚫 Evitar tiros extra
                if (tiros >= maxTiros) return true

                val diffX = e2.x - (e1?.x ?: 0f)

                val direccionJugador = when {
                    diffX > 100 -> 2
                    diffX < -100 -> 0
                    else -> 1
                }

                tirarPenal(direccionJugador)
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event!!)
        return super.onTouchEvent(event)
    }

    private fun tirarPenal(direccionJugador: Int) {

        val direccionPortero = Random.nextInt(3)

        tiros++ // 👈 contar tiro

        val destinoPorteroX = when (direccionPortero) {
            0 -> 100f
            1 -> 400f
            else -> 700f
        }

        imgPortero.animate().x(destinoPorteroX).setDuration(300).start()

        val destinoX = when (direccionJugador) {
            0 -> 100f
            1 -> 400f
            else -> 700f
        }

        val destinoY = 200f

        imgBalon.animate()
            .x(destinoX)
            .y(destinoY)
            .rotationBy(360f)
            .setDuration(500)
            .withEndAction {

                if (direccionJugador == direccionPortero) {
                    txtResultado.text = "¡Atajó! 🧤 ($tiros/$maxTiros)"
                } else {
                    goles++
                    txtResultado.text = "¡Gol! ⚽ ($tiros/$maxTiros)"
                }

                // Regresar balón
                imgBalon.animate()
                    .x(balonInicialX)
                    .y(balonInicialY)
                    .rotation(0f)
                    .setDuration(300)
                    .start()

                // 🏁 Terminar juego
                if (tiros >= maxTiros) {
                    terminarJuego()
                }
            }
            .start()
    }

    private fun terminarJuego() {

        val mensajeFinal = if (goles >= 4) {
            "¡Ganaste! 🎉 ($goles/$maxTiros)"
        } else {
            "Perdiste 😢 ($goles/$maxTiros)"
        }

        txtResultado.text = mensajeFinal

        // Espera 2 segundos y cierra
        txtResultado.postDelayed({

            val resultIntent = Intent()
            resultIntent.putExtra("RESULTADO", mensajeFinal)

            setResult(Activity.RESULT_OK, resultIntent)
            finish()

        }, 2000)
    }
}