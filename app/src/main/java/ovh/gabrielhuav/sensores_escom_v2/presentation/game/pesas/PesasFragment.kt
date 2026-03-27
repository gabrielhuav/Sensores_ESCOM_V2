package ovh.gabrielhuav.sensores_escom_v2.presentation.game.pesas


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import ovh.gabrielhuav.sensores_escom_v2.R
import kotlin.random.Random

class PesasFragment : Fragment() {

    private lateinit var indicator: View
    private lateinit var target: View
    private lateinit var track: View
    private lateinit var weight: ImageView
    private lateinit var btnAction: Button
    private lateinit var heart1: ImageView
    private lateinit var heart2: ImageView
    private lateinit var heart3: ImageView

    private var currentLevel = 1
    private var successfulHits = 0
    private var lives = 3
    private var animator: ValueAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pesas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        indicator = view.findViewById(R.id.v_indicator)
        target = view.findViewById(R.id.v_target)
        track = view.findViewById(R.id.v_track)
        weight = view.findViewById(R.id.iv_weight)
        btnAction = view.findViewById(R.id.btn_action)
        heart1 = view.findViewById(R.id.iv_heart1)
        heart2 = view.findViewById(R.id.iv_heart2)
        heart3 = view.findViewById(R.id.iv_heart3)

        // Asumiendo que el recurso se llama ic_pesa
        weight.setImageResource(R.drawable.ic_pesa)

        startMovingBar()

        btnAction.setOnClickListener {
            if (lives > 0) checkHit() else restartGame()
        }
    }

    private fun startMovingBar() {
        track.post {
            val maxTranslation = (track.width - indicator.width).toFloat()
            if (maxTranslation <= 0f) return@post

            val (durationMs, targetWidthDp) = when(currentLevel) {
                1 -> Pair(1500L, 70f)
                2 -> Pair(1000L, 45f)
                3 -> Pair(600L, 30f)
                else -> Pair(1500L, 70f)
            }

            val params = target.layoutParams
            params.width = dpToPx(targetWidthDp).toInt()
            target.layoutParams = params

            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, maxTranslation).apply {
                duration = durationMs
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    indicator.translationX = animation.animatedValue as Float
                }
                start()
            }
        }
    }

    private fun checkHit() {
        val iLeft = indicator.x
        val iRight = iLeft + indicator.width
        val tLeft = target.x
        val tRight = tLeft + target.width
        val buffer = if (currentLevel == 3) 10f else 25f

        if (iLeft < tRight + buffer && iRight > tLeft - buffer) {
            onSuccess()
        } else {
            onFailure()
        }
    }

    private fun onSuccess() {
        successfulHits++
        ObjectAnimator.ofFloat(weight, "translationY", 0f, -300f).apply {
            duration = 200
            repeatCount = 1
            repeatMode = ValueAnimator.REVERSE
            start()
        }

        if (successfulHits >= 5) {
            if (currentLevel < 3) {
                currentLevel++
                successfulHits = 0
                startMovingBar()
            } else {
                animator?.cancel()
                showFinishDialog(true)
                return
            }
        }

        target.post {
            val safety = 60f
            val maxPos = (track.width - target.width).toFloat() - safety
            if (maxPos > safety) {
                target.translationX = Random.nextDouble(safety.toDouble(), maxPos.toDouble()).toFloat()
            }
        }
    }

    private fun onFailure() {
        lives--
        when(lives) {
            2 -> animateHeartLoss(heart3)
            1 -> animateHeartLoss(heart2)
            0 -> animateHeartLoss(heart1)
        }

        if (lives > 0) {
            startMovingBar()
        } else {
            animator?.cancel()
            showFinishDialog(false)
        }
    }

    private fun animateHeartLoss(heart: ImageView) {
        val fadeOut = ObjectAnimator.ofFloat(heart, View.ALPHA, 1f, 0f)
        val dropDown = ObjectAnimator.ofFloat(heart, View.TRANSLATION_Y, 0f, 150f)
        AnimatorSet().apply {
            playTogether(fadeOut, dropDown)
            duration = 500
            interpolator = AccelerateInterpolator()
            start()
        }
    }

    private fun showFinishDialog(isWin: Boolean) {
        val title = if (isWin) "¡Felicidades!" else "Game Over"
        val message = if (isWin) "¡ESTÁS MAMADÍSIMO! 💪🔥" else "¡A darle más al gym! 🏋️‍♂️"

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(if (isWin) "Seguir" else "Reintentar") { _, _ ->
                restartGame()
            }
            .setNegativeButton("Salir") { _, _ ->
                exitGame()
            }
            .show()
    }

    private fun exitGame() {
        // Cierra el fragmento y vuelve al mapa de Canchasgestion
        parentFragmentManager.popBackStack()
    }

    private fun restartGame() {
        currentLevel = 1
        successfulHits = 0
        lives = 3
        listOf(heart1, heart2, heart3).forEach {
            it.alpha = 1f
            it.translationY = 0f
        }
        startMovingBar()
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        animator?.cancel()
    }
}
