package com.suraksha.shaurya

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val preferenceHelper = PreferenceHelper(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (preferenceHelper.getUserName() != "") {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                startActivity(
                    Intent(this, ProfileActivity::class.java)
                        .putExtra("btn_text", "NEXT")
                )
                finish()
            }
        }, 5000)
    }
}