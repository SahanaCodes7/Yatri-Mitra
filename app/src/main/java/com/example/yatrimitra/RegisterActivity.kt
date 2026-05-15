package com.example.yatrimitra

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaType
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etName     = findViewById<EditText>(R.id.etRegName)
        val etEmail    = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val ivEye      = findViewById<ImageView>(R.id.ivRegPasswordEye)
        val btnReg     = findViewById<LinearLayout>(R.id.btnDoRegister)
        val tvBack     = findViewById<TextView>(R.id.tvBackToLogin)
        val tvError    = findViewById<TextView>(R.id.tvRegError)
        val progress   = findViewById<ProgressBar>(R.id.regProgress)

        // Eye toggle
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

        btnReg.setOnClickListener {
            val name  = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass  = etPassword.text.toString().trim()
            if (name.isEmpty() || email.isEmpty() || pass.length < 6) {
                tvError.text = "All fields required, password min 6 chars"
                tvError.visibility = View.VISIBLE; return@setOnClickListener
            }
            tvError.visibility = View.GONE
            progress.visibility = View.VISIBLE

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    val firebaseUser = auth.currentUser
                    // Set display name first
                    val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name).build()
                    firebaseUser?.updateProfile(profileUpdate)?.addOnCompleteListener {
                        // Send Firebase verification email (works out-of-the-box, no setup needed)
                        val actionSettings = com.google.firebase.auth.ActionCodeSettings.newBuilder()
                            .setHandleCodeInApp(false)
                            .build()
                        firebaseUser.sendEmailVerification()
                        // Also send custom branded welcome via EmailJS (add your keys to enable)
                        sendWelcomeEmail(name, email)
                        saveRoleAndGoHome()
                    }
                }
                .addOnFailureListener { e ->
                    progress.visibility = View.GONE
                    tvError.text = e.message ?: "Registration failed"
                    tvError.visibility = View.VISIBLE
                }
        }

        tvBack.setOnClickListener { finish() }
    }

    private fun sendWelcomeEmail(name: String, email: String) {
        val joinDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        // Backup record in Firebase
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            com.google.firebase.database.FirebaseDatabase.getInstance().reference
                .child("welcome_emails").child(uid)
                .setValue(hashMapOf(
                    "to" to email, "name" to name,
                    "joinDate" to joinDate, "sentAt" to System.currentTimeMillis()
                ))
        } catch (_: Exception) {}

        // Send via EmailJS — free tier (200 emails/month), no backend needed
        Thread {
            try {
                val json = org.json.JSONObject().apply {
                    put("service_id",  "service_yatrimitra")      // ← your EmailJS Service ID
                    put("template_id", "template_welcome")        // ← your EmailJS Template ID
                    put("user_id",     "YOUR_EMAILJS_PUBLIC_KEY") // ← your EmailJS Public Key
                    put("template_params", org.json.JSONObject().apply {
                        put("to_name",   name)
                        put("to_email",  email)
                        put("join_date", joinDate)
                        put("app_name",  "YatriMitra")
                    })
                }
                val body = okhttp3.RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                    json.toString()
                )
                val request = okhttp3.Request.Builder()
                    .url("https://api.emailjs.com/api/v1.0/email/send")
                    .post(body)
                    .addHeader("origin", "https://yatrimitra.app")
                    .build()
                val response = okhttp3.OkHttpClient().newCall(request).execute()
                Log.d("WelcomeEmail", if (response.isSuccessful) "Sent ✓" else "Failed: ${response.code}")
            } catch (e: Exception) {
                Log.e("WelcomeEmail", "Error sending welcome email", e)
            }
        }.start()
    }

    private fun saveRoleAndGoHome() {
        val prefs = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(YatriMitraApp.KEY_USER_ROLE, "passenger").apply()
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
