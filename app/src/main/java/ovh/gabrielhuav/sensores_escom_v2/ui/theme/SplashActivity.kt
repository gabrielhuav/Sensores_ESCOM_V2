package ovh.gabrielhuav.sensores_escom_v2.ui.theme

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.MainActivity
import ovh.gabrielhuav.sensores_escom_v2.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Find the logo and apply the animation
        val logo = findViewById<ImageView>(R.id.logo)
        val fadeInScaleAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale)
        logo.startAnimation(fadeInScaleAnimation)

        // Wait for a while before starting the next activity
        Handler(Looper.getMainLooper()).postDelayed({
            // Start the MainActivity after the animation
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Finish SplashActivity so it can't be returned to
        }, 2000) // 2000 ms = 2 seconds
    }
}