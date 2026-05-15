package com.example.yatrimitra

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        val prefs   = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
        val isFirst = prefs.getBoolean(YatriMitraApp.KEY_FIRST_LAUNCH, true)

        findViewById<View>(R.id.btnGetStarted).setOnClickListener {
            when {
                isFirst -> {
                    prefs.edit().putBoolean(YatriMitraApp.KEY_FIRST_LAUNCH, false).apply()
                    startActivity(Intent(this, OnboardingActivity::class.java))
                }
                FirebaseAuth.getInstance().currentUser == null -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                else -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                }
            }
            finish()
        }
    }
}
