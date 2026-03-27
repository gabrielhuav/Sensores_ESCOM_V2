package ovh.gabrielhuav.sensores_escom_v2.presentation.game.basketball

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import ovh.gabrielhuav.sensores_escom_v2.R
import kotlin.math.abs

class BasketballGame(context: Context) : Dialog(context) {

    private var score = 0
    private var gameState = 0 // 0: Inicio, 1: Angulo, 2: Potencia, 3: Animando, 4: Resultado

    private val handler = Handler(Looper.getMainLooper())

    private var angleDirection = 1
    private var powerDirection = 1
    private var currentAngle = 50
    private var currentPower = 0

    private lateinit var imgBall: ImageView
    private lateinit var imgHoop: ImageView
    private lateinit var tvScore: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressAngle: ProgressBar
    private lateinit var progressPower: ProgressBar
    private lateinit var btnAction: Button
    private lateinit var trajectoryView: TrajectoryView

    private var initialBallX = 0f
    private var initialBallY = 0f

    private var isInitialized = false
    private var originalOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    private fun getBaseActivity(): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_basketball)

        configWindow()

        imgBall = findViewById(R.id.imgBall)
        imgHoop = findViewById(R.id.imgHoop)
        tvScore = findViewById(R.id.tvScore)
        tvStatus = findViewById(R.id.tvStatus)
        progressAngle = findViewById(R.id.progressAngle)
        progressPower = findViewById(R.id.progressPower)
        btnAction = findViewById(R.id.btnAction)
        trajectoryView = findViewById(R.id.trajectoryView)

        findViewById<Button>(R.id.btnExit).setOnClickListener {
            stopLoops()
            dismiss()
        }

        btnAction.setOnClickListener {
            if (isInitialized) handleAction()
        }

        btnAction.isEnabled = false

        imgBall.post {
            captureInitialPositions()
        }
    }

    override fun onStart() {
        super.onStart()
        getBaseActivity()?.let { activity ->
            originalOrientation = activity.requestedOrientation
            val currentOrientation = context.resources.configuration.orientation

            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    override fun onStop() {
        getBaseActivity()?.requestedOrientation = originalOrientation
        super.onStop()
        stopLoops()
        imgBall.animate().cancel()
    }

    private fun captureInitialPositions() {
        if (imgHoop.width == 0 || imgBall.width == 0) {
            imgBall.postDelayed({ captureInitialPositions() }, 32)
            return
        }

        initialBallX = imgBall.x
        initialBallY = imgBall.y
        isInitialized = true
        btnAction.isEnabled = true
        resetShot()
    }

    private fun configWindow() {
        val displayMetrics = context.resources.displayMetrics

        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (isLandscape) {
            (displayMetrics.heightPixels * 1.25).toInt().coerceAtMost((displayMetrics.widthPixels * 0.95).toInt())
        } else {
            (displayMetrics.widthPixels * 0.98).toInt()
        }

        window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
        setCancelable(false)
    }

    private fun handleAction() {
        when (gameState) {
            0 -> {
                gameState = 1
                tvStatus.text = "FIJA ALTURA"
                btnAction.text = "FIJAR"
                startAngleLoop()
            }
            1 -> {
                gameState = 2
                handler.removeCallbacks(angleRunnable)
                tvStatus.text = "MIDE FUERZA"
                btnAction.text = "LANZAR"
                startPowerLoop()
            }
            2 -> {
                gameState = 3
                handler.removeCallbacks(powerRunnable)
                trajectoryView.hide()
                btnAction.isEnabled = false
                tvStatus.text = "¡LANZANDO!"
                animateShot()
            }
            4 -> resetShot()
        }
    }

    private val angleRunnable = object : Runnable {
        override fun run() {
            // Ajuste de velocidad: MUCHO MÁS RÁPIDO (10ms delay + incremento de 2)
            currentAngle += 2 * angleDirection
            if (currentAngle >= 100) { currentAngle = 100; angleDirection = -1 }
            else if (currentAngle <= 0) { currentAngle = 0; angleDirection = 1 }
            progressAngle.progress = currentAngle
            updateTrajectoryPreview()
            handler.postDelayed(this, 10) 
        }
    }

    private val powerRunnable = object : Runnable {
        override fun run() {
            // Ajuste de velocidad: MUCHO MÁS RÁPIDO (10ms delay + incremento de 3)
            currentPower += 3 * powerDirection
            if (currentPower >= 100) { currentPower = 100; powerDirection = -1 }
            else if (currentPower <= 0) { currentPower = 0; powerDirection = 1 }
            progressPower.progress = currentPower
            updateTrajectoryPreview()
            handler.postDelayed(this, 10)
        }
    }

    private fun startAngleLoop() = handler.post(angleRunnable)
    private fun startPowerLoop() = handler.post(powerRunnable)

    private fun stopLoops() {
        handler.removeCallbacks(angleRunnable)
        handler.removeCallbacks(powerRunnable)
    }

    private fun calculateTargetParams(): Triple<Float, Float, Float> {
        val hoopCenterX = imgHoop.x + (imgHoop.width / 2)
        val hoopTargetY = imgHoop.y + (imgHoop.height * 0.3f)
        val distToHoop = hoopCenterX - initialBallX

        val targetX = initialBallX + distToHoop * (currentPower / 80f)
        val targetY = hoopTargetY

        val baseArc = distToHoop * 0.35f
        val angleInfluence = currentAngle * 4.8f
        val powerAdjustment = (80 - currentPower) * 2f

        val arcHeight = baseArc + angleInfluence + powerAdjustment

        return Triple(targetX, targetY, arcHeight)
    }

    private fun getShotPath(): Path {
        val (targetX, targetY, arcHeight) = calculateTargetParams()
        return Path().apply {
            moveTo(initialBallX, initialBallY)
            val controlX = (initialBallX + targetX) / 2
            val controlY = initialBallY - arcHeight
            quadTo(controlX, controlY, targetX, targetY)
        }
    }

    private fun updateTrajectoryPreview() {
        if (!isInitialized) return
        val (targetX, _, arcHeight) = calculateTargetParams()
        trajectoryView.updateTrajectory(
            initialBallX, initialBallY,
            (initialBallX + targetX) / 2, initialBallY - arcHeight,
            targetX, initialBallY + 50f
        )
    }

    private fun animateShot() {
        val (targetX, targetY, _) = calculateTargetParams()
        val path = getShotPath()

        val hoopCenterX = imgHoop.x + (imgHoop.width / 2)
        val errorX = abs(targetX - hoopCenterX)

        val isDirectGoal = errorX < 50 && currentPower in 73..87 && currentAngle > 15
        val isBackboardHit = targetX > hoopCenterX + 45 && targetX < hoopCenterX + 125
        val isBankShot = isBackboardHit && currentPower in 85..96 && currentAngle > 18

        val distance = abs(targetX - initialBallX)
        val duration = (700 + distance * 1.0).toLong().coerceIn(700, 1800)

        val rotationAnim = ObjectAnimator.ofFloat(imgBall, View.ROTATION, 0f, 1080f)
        val moveAnim = ObjectAnimator.ofFloat(imgBall, View.X, View.Y, path)

        AnimatorSet().apply {
            playTogether(rotationAnim, moveAnim)
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    when {
                        isDirectGoal -> animateBallThroughNet()
                        isBankShot -> animateBackboardBounce(true)
                        isBackboardHit -> animateBackboardBounce(false)
                        else -> animateMiss(targetX, hoopCenterX)
                    }
                }
            })
            start()
        }
    }

    private fun animateBackboardBounce(goesIn: Boolean) {
        tvStatus.text = "¡TABLERO! 🏀"
        tvStatus.setTextColor(android.graphics.Color.CYAN)

        imgHoop.animate().scaleX(1.1f).scaleY(0.9f).setDuration(60).withEndAction {
            imgHoop.animate().scaleX(1f).scaleY(1f).setDuration(60).start()
        }.start()

        if (goesIn) {
            val hoopCenterX = imgHoop.x + (imgHoop.width / 2) - (imgBall.width / 2)
            val hoopTopY = imgHoop.y + (imgHoop.height * 0.25f)

            imgBall.animate()
                .x(hoopCenterX)
                .y(hoopTopY)
                .setDuration(250)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { animateBallThroughNet() }
                .start()
        } else {
            val bounceX = imgBall.x - 120f
            val bounceY = imgBall.y + 250f
            imgBall.animate()
                .x(bounceX)
                .y(bounceY)
                .setDuration(400)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { resolveShotResult(false) }
                .start()
        }
    }

    private fun animateBallThroughNet() {
        tvStatus.text = "¡SWISH! 🏀✨"
        tvStatus.setTextColor(android.graphics.Color.YELLOW)

        val hoopCenterX = imgHoop.x + (imgHoop.width / 2) - (imgBall.width / 2)
        imgBall.x = hoopCenterX 

        val fallAnim = ObjectAnimator.ofFloat(imgBall, View.TRANSLATION_Y, 0f, 180f)
        val scaleX = ObjectAnimator.ofFloat(imgBall, View.SCALE_X, 1f, 0.4f)
        val scaleY = ObjectAnimator.ofFloat(imgBall, View.SCALE_Y, 1f, 0.4f)
        val alphaAnim = ObjectAnimator.ofFloat(imgBall, View.ALPHA, 1f, 0.2f)

        val hoopRipple = ObjectAnimator.ofFloat(imgHoop, View.SCALE_Y, 1f, 1.1f, 1f).apply {
            duration = 300
            interpolator = OvershootInterpolator()
        }

        AnimatorSet().apply {
            playTogether(fallAnim, scaleX, scaleY, alphaAnim, hoopRipple)
            duration = 500
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    imgBall.scaleX = 1f; imgBall.scaleY = 1f; imgBall.alpha = 1f
                    resolveShotResult(true)
                }
            })
            start()
        }
    }

    private fun animateMiss(ballX: Float, hoopX: Float) {
        val errorX = abs(ballX - hoopX)
        val hitFront = ballX < hoopX

        if (errorX < 115) {
            tvStatus.text = "¡REBOTE! ☄️"
            val bounceX = if (hitFront) -140f else 140f
            val bouncePath = Path().apply {
                moveTo(imgBall.x, imgBall.y)
                quadTo(imgBall.x + bounceX/2, imgBall.y - 120f, imgBall.x + bounceX, imgBall.y + 250f)
            }
            ObjectAnimator.ofFloat(imgBall, View.X, View.Y, bouncePath).apply {
                duration = 500
                interpolator = AccelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) { resolveShotResult(false) }
                })
                start()
            }
        } else {
            tvStatus.text = "¡AIR BALL! 💨"
            imgBall.animate().translationY(450f).alpha(0f).setDuration(500).withEndAction { resolveShotResult(false) }.start()
        }
    }

    private fun resolveShotResult(isGoal: Boolean) {
        if (isGoal) {
            score += 2
            tvScore.text = "SCORE: ${String.format("%02d", score)}"
            tvStatus.text = "¡CANASTA! 🔥"
            tvStatus.setTextColor(android.graphics.Color.GREEN)
        } else {
            tvStatus.setTextColor(android.graphics.Color.RED)
        }

        handler.postDelayed({
            if (isShowing) tvStatus.setTextColor(android.graphics.Color.WHITE)
        }, 1500)

        gameState = 4
        btnAction.isEnabled = true
        btnAction.text = "REINTENTAR"
    }

    private fun resetShot() {
        if (!isInitialized) return
        imgBall.animate().cancel()
        imgHoop.animate().cancel()
        imgBall.alpha = 1f
        imgBall.x = initialBallX
        imgBall.y = initialBallY
        imgBall.rotation = 0f
        imgBall.translationY = 0f
        imgBall.translationX = 0f
        imgBall.scaleX = 1f
        imgBall.scaleY = 1f
        imgHoop.scaleX = 1f
        imgHoop.scaleY = 1f

        currentAngle = 50
        currentPower = 0
        angleDirection = 1
        powerDirection = 1
        progressAngle.progress = 50
        progressPower.progress = 0
        gameState = 0
        btnAction.text = "EMPEZAR"
        btnAction.isEnabled = true
        tvStatus.text = "Apunta al aro"
        trajectoryView.hide()
    }
}
