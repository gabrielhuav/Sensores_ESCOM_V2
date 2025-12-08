package ovh.gabrielhuav.sensores_escom_v2.presentation.game.basketball

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import ovh.gabrielhuav.sensores_escom_v2.R
import kotlin.math.abs

class BasketballGame(context: Context) : Dialog(context) {

    private var score = 0
    private var gameState = 0 // 0: Idle, 1: Angle, 2: Power
    private val handler = Handler(Looper.getMainLooper())

    // Variables lógicas
    private var angleDirection = 1
    private var powerDirection = 1
    private var currentAngle = 50
    private var currentPower = 0

    // --- ESTAS SON LAS VARIABLES QUE TE FALTABAN ---
    // Componentes Visuales
    private lateinit var imgBall: ImageView
    private lateinit var imgHoop: ImageView
    private lateinit var imgPlayer: ImageView
    private lateinit var tvScore: TextView
    private lateinit var tvStatus: TextView        // <--- Esta
    private lateinit var progressAngle: ProgressBar // <--- Esta
    private lateinit var progressPower: ProgressBar // <--- Esta
    private lateinit var btnAction: Button
    private lateinit var btnExit: Button
    // -----------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_basketball)

        // Ajustar ancho del diálogo
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = lp

        setCancelable(false)

        // Vincular Vistas (Asegúrate de que estos IDs existan en tu XML)
        imgBall = findViewById(R.id.imgBall)
        imgHoop = findViewById(R.id.imgHoop)
        imgPlayer = findViewById(R.id.imgPlayer)
        tvScore = findViewById(R.id.tvScore)
        tvStatus = findViewById(R.id.tvStatus)
        progressAngle = findViewById(R.id.progressAngle)
        progressPower = findViewById(R.id.progressPower)
        btnAction = findViewById(R.id.btnAction)
        btnExit = findViewById(R.id.btnExit)

        btnAction.setOnClickListener { nextState() }
        btnExit.setOnClickListener {
            stopLoops()
            dismiss()
        }

        resetShot()
    }

    private fun nextState() {
        when (gameState) {
            0 -> { // Iniciar Ángulo
                gameState = 1
                tvStatus.text = "1. Fija el ÁNGULO (Altura)"
                btnAction.text = "FIJAR"
                startAngleLoop()
            }
            1 -> { // Iniciar Potencia
                gameState = 2
                stopLoops()
                tvStatus.text = "2. Fija la POTENCIA (Distancia)"
                btnAction.text = "LANZAR"
                startPowerLoop()
            }
            2 -> { // Disparar
                gameState = 3 // Estado de animación
                stopLoops()
                btnAction.isEnabled = false // Desactivar botón durante vuelo
                animateShot()
            }
        }
    }

    // --- BUCLES DE LAS BARRAS ---
    private val angleRunnable = object : Runnable {
        override fun run() {
            currentAngle += (3 * angleDirection)
            if (currentAngle >= 100 || currentAngle <= 0) angleDirection *= -1
            progressAngle.progress = currentAngle
            handler.postDelayed(this, 20)
        }
    }

    private val powerRunnable = object : Runnable {
        override fun run() {
            currentPower += (4 * powerDirection)
            if (currentPower >= 100 || currentPower <= 0) powerDirection *= -1
            progressPower.progress = currentPower
            handler.postDelayed(this, 20)
        }
    }

    private fun startAngleLoop() { handler.post(angleRunnable) }
    private fun startPowerLoop() { handler.post(powerRunnable) }

    private fun stopLoops() {
        handler.removeCallbacks(angleRunnable)
        handler.removeCallbacks(powerRunnable)
    }

    // --- LÓGICA DE ANIMACIÓN Y FÍSICA VISUAL ---
    private fun animateShot() {
        // 1. Calcular si es canasta
        // Ángulo ideal: 50 (Centro). Potencia ideal: 90 (Casi lleno)
        val isAngleGood = abs(currentAngle - 50) <= 15
        val isPowerGood = currentPower in 80..100
        val isGoal = isAngleGood && isPowerGood

        // 2. Coordenadas de inicio (Manos del jugador)
        val startX = imgBall.x
        val startY = imgBall.y

        // 3. Coordenadas del Aro (Destino ideal)
        val hoopX = imgHoop.x + (imgHoop.width / 2) - 20
        val hoopY = imgHoop.y + (imgHoop.height / 3)

        // 4. Calcular destino real basado en fallo o acierto
        var endX = hoopX
        var endY = hoopY

        if (!isGoal) {
            if (currentPower < 80) {
                // Tiro corto: cae al suelo antes
                endX = startX + (hoopX - startX) * (currentPower / 100f)
                endY = startY + 200 // Cae al suelo
            } else if (currentPower > 95) {
                // Tiro largo: golpea tablero y se va
                endX = hoopX + 50
                endY = hoopY - 50
            }

            if (!isAngleGood) {
                // Si el ángulo es malo, ajustamos altura final
                endY += 100
            }
        }

        // 5. Calcular Altura del arco (Control Point para curva Bézier)
        val arcHeight = 500f + (currentAngle * 5)
        val controlX = (startX + endX) / 2
        val controlY = startY - arcHeight

        // 6. Crear el camino (Path)
        val path = Path().apply {
            moveTo(startX, startY)
            quadTo(controlX, controlY, endX, endY) // Curva cuadrática
        }

        // 7. Ejecutar Animación
        val animator = ObjectAnimator.ofFloat(imgBall, View.X, View.Y, path).apply {
            duration = 1500 // 1.5 segundos de vuelo
            start()
        }

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                resolveShotResult(isGoal)
            }
        })
    }

    private fun resolveShotResult(isGoal: Boolean) {
        if (isGoal) {
            score += 2
            tvStatus.text = "¡CANASTA! ¡EXCELENTE TIRO!"
            tvScore.text = "Puntos: $score"
            tvScore.setTextColor(context.resources.getColor(android.R.color.holo_green_light))
        } else {
            var reason = ""
            if (abs(currentAngle - 50) > 15) reason += "Mal Ángulo. "
            if (currentPower < 80) reason += "Falta Fuerza. "
            if (currentPower > 95) reason += "Demasiada Fuerza. "

            tvStatus.text = "FALLASTE: $reason"
            tvScore.setTextColor(context.resources.getColor(android.R.color.holo_red_light))
        }

        btnAction.text = "OTRA VEZ"
        btnAction.isEnabled = true

        // Regresar balón a las manos después de 2 segundos
        handler.postDelayed({
            resetShot()
            tvStatus.text = "Toca INICIAR para lanzar"
            tvScore.setTextColor(context.resources.getColor(android.R.color.white))
        }, 2000)
    }

    private fun resetBallPosition() {
        // Resetear posición visual del balón
        imgBall.translationX = 0f
        imgBall.translationY = 0f
    }

    private fun resetShot() {
        resetBallPosition()
        currentAngle = 50
        currentPower = 0
        angleDirection = 1
        powerDirection = 1
        progressAngle.progress = 50
        progressPower.progress = 0
        gameState = 0
        btnAction.text = "INICIAR"
        btnAction.isEnabled = true
    }

    override fun onStop() {
        super.onStop()
        stopLoops()
    }
}