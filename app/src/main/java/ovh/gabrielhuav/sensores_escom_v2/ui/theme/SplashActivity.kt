package ovh.gabrielhuav.sensores_escom_v2.ui.theme

import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.MainActivity
import ovh.gabrielhuav.sensores_escom_v2.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onResume() {
        super.onResume()
        val logo = findViewById<ImageView>(R.id.logo)
        val fadeInScale = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale)
        fadeInScale.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                // No action needed here
            }

            override fun onAnimationEnd(animation: Animation?) {
                // Start MainActivity when the animation ends
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {
                // No action needed here
            }
        })
        logo.startAnimation(fadeInScale)
    }
}