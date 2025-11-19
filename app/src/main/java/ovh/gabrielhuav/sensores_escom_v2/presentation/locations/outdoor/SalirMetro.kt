package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.R
import android.widget.TextView
import android.widget.ImageView
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.util.Log
class SalirMetro : AppCompatActivity() {
        private lateinit var handler: Handler
        private var latitude: Double = 0.0
        private var longitude: Double = 0.0
        private var playerName: String = ""

        // Views para animación
        private lateinit var metroTrain: ImageView
        private lateinit var pasajero: ImageView
        private lateinit var tvMessage: TextView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_salir_metro)

            window.setBackgroundDrawable(ColorDrawable(Color.BLACK))

            latitude = intent.getDoubleExtra("LATITUDE", 0.0)
            longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
            playerName = intent.getStringExtra("PLAYER_NAME") ?: "Jugador"

            initializeViews()
            startMetroAnimation()
        }

        private fun initializeViews() {
            metroTrain = findViewById(R.id.metroTrain)
            pasajero = findViewById(R.id.pasajero)
            tvMessage = findViewById(R.id.tvTransitionMessage)

            // Inicialmente ocultar los elementos
            metroTrain.alpha = 0f
            pasajero.alpha = 0f
            tvMessage.text = "Llegando a su destino..."
        }

        private fun startMetroAnimation() {
            handler = Handler(Looper.getMainLooper())

            // Secuencia de animaciones
            handler.postDelayed({
                animateMetroArrival()
            }, 1000)
        }

        private fun animateMetroArrival() {
            tvMessage.text = "¡Llego a su destino!"

            // Animación de entrada del metro desde la derecha
            val metroEnterAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 1.0f,  // Desde fuera de la pantalla a la derecha
                Animation.RELATIVE_TO_PARENT, 0.3f,  // Hasta el centro
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f
            ).apply {
                duration = 1500
                interpolator = DecelerateInterpolator()
                fillAfter = true
            }

            metroTrain.alpha = 1f
            metroTrain.startAnimation(metroEnterAnimation)

            // Cuando termina la entrada del metro se muestra punto
            handler.postDelayed({
                animatePassengerDescent()
            }, 1600)
        }

        private fun animatePassengerDescent() {
            tvMessage.text = "Bajando del metro..."

            // Primero hacer visible el puntito en la posición del metro
            pasajero.alpha = 1f

            // Animación de descenso del puntito
            val descentAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,  // Desde la posición del metro
                Animation.RELATIVE_TO_PARENT, 0.8f   // Hacia abajo (fuera de pantalla)
            ).apply {
                duration = 2000
                interpolator = AccelerateInterpolator()
                fillAfter = true
            }

            // Escuchar cuando termine la animación de descenso
            descentAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    tvMessage.text = "¡Listo!"
                    // Esperar y cambiar de actividad
                    handler.postDelayed({
                        openEstacionesMetro()
                    }, 500)
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })

            pasajero.startAnimation(descentAnimation)
        }

        private fun openEstacionesMetro() {
            try {
                val intent = Intent(this, EstacionesMetro::class.java).apply {
                    putExtra("LATITUDE", latitude)
                    putExtra("LONGITUDE", longitude)
                    putExtra("PLAYER_NAME", playerName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e("MetroTransition", "Error al abrir EstacionesMetro: ${e.message}")
                finish()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacksAndMessages(null)
        }
    }