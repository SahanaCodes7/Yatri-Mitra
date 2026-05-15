package com.example.yatrimitra

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var selectedRole = "passenger"
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) { goHome(); return }

        val btnPassenger = findViewById<LinearLayout>(R.id.btnRolePassenger)
        val btnDriver    = findViewById<LinearLayout>(R.id.btnRoleDriver)
        val etEmail      = findViewById<EditText>(R.id.etEmail)
        val etPassword   = findViewById<EditText>(R.id.etPassword)
        val ivEye        = findViewById<ImageView>(R.id.ivPasswordEye)
        val btnLogin     = findViewById<LinearLayout>(R.id.btnLogin)
        val btnRegister  = findViewById<LinearLayout>(R.id.btnRegister)
        val tvError      = findViewById<TextView>(R.id.tvLoginError)
        val progress     = findViewById<ProgressBar>(R.id.loginProgress)

        btnPassenger.setOnClickListener {
            selectedRole = "passenger"
            btnPassenger.setBackgroundResource(R.drawable.bg_role_btn_active)
            btnDriver.setBackgroundResource(R.drawable.bg_role_btn_inactive)
        }
        btnDriver.setOnClickListener {
            selectedRole = "driver"
            btnDriver.setBackgroundResource(R.drawable.bg_role_btn_active)
            btnPassenger.setBackgroundResource(R.drawable.bg_role_btn_inactive)
        }

        // Password eye toggle
        ivEye.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etPassword.setSelection(etPassword.text.length)
            ivEye.setImageResource(
                if (isPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass  = etPassword.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                tvError.text = "Please fill in email and password"
                tvError.visibility = View.VISIBLE; return@setOnClickListener
            }
            tvError.visibility = View.GONE
            progress.visibility = View.VISIBLE
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { saveRoleAndGoHome(selectedRole) }
                .addOnFailureListener { e ->
                    progress.visibility = View.GONE
                    tvError.text = e.message ?: "Login failed"
                    tvError.visibility = View.VISIBLE
                }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun saveRoleAndGoHome(role: String) {
        val prefs = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(YatriMitraApp.KEY_USER_ROLE, role)
            .putBoolean("driver_mode_enabled", role == "driver")
            .apply()
        goHome()
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
