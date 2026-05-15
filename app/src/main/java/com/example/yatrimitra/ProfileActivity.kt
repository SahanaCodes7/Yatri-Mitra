package com.example.yatrimitra

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import org.json.JSONArray

class ProfileActivity : AppCompatActivity() {

    companion object { private const val REQ_PICK_IMAGE = 1001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val auth  = FirebaseAuth.getInstance()
        val user  = auth.currentUser
        val prefs = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)

        val name  = user?.displayName?.takeIf { it.isNotEmpty() } ?: "YatriMitra User"
        val email = user?.email ?: "Not signed in"
        val uid   = user?.uid?.take(12) ?: "—"

        val tvInitial = findViewById<TextView>(R.id.tvProfileInitial)
        val tvName    = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail   = findViewById<TextView>(R.id.tvProfileEmail)
        val tvUid     = findViewById<TextView>(R.id.tvUidChip)
        val tvRole    = findViewById<TextView>(R.id.tvRoleText)
        val tvSafety  = findViewById<TextView>(R.id.tvSafetyCount)

        tvInitial.text = name.first().uppercaseChar().toString()
        tvName.text    = name
        tvEmail.text   = email
        tvUid.text     = "UID: $uid..."

        // Restore previously saved profile photo
        val savedPhotoUri = prefs.getString("profile_photo_uri", null)
        if (!savedPhotoUri.isNullOrEmpty()) {
            try {
                val ivAvatar  = findViewById<android.widget.ImageView>(R.id.ivProfileAvatar)
                val tvInitial2 = findViewById<TextView>(R.id.tvProfileInitial)
                val file = java.io.File(filesDir, "profile_photo.jpg")
                if (file.exists()) {
                    ivAvatar.setImageURI(null)   // clear any cached URI
                    ivAvatar.setImageURI(android.net.Uri.fromFile(file))
                    ivAvatar.visibility  = android.view.View.VISIBLE
                    tvInitial2.visibility = android.view.View.INVISIBLE
                }
            } catch (_: Exception) {}
        }

        val isDriver = prefs.getBoolean("driver_mode_enabled", false)
        tvRole.text = if (isDriver) "Driver" else "Passenger"

        val contacts = try {
            val raw = prefs.getString("safety_contacts", null)
            if (raw != null) JSONArray(raw).length() else 0
        } catch (_: Exception) { 0 }
        tvSafety.text = "$contacts contact${if (contacts == 1) "" else "s"} saved"

        // Camera badge — open gallery to pick profile pic
        findViewById<LinearLayout>(R.id.btnCameraBadge).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, REQ_PICK_IMAGE)
        }

        // Edit Profile — shows real dialog to update display name
        findViewById<LinearLayout>(R.id.rowEditProfile).setOnClickListener {
            val etName = EditText(this).apply {
                hint = "Display name"
                setText(user?.displayName ?: "")
                setPadding(40, 20, 40, 20)
            }
            AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(etName)
                .setPositiveButton("Save") { _, _ ->
                    val newName = etName.text.toString().trim()
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val req = UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()
                    user?.updateProfile(req)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            tvName.text    = newName
                            tvInitial.text = newName.first().uppercaseChar().toString()
                            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Change Password — re-auth then update
        findViewById<LinearLayout>(R.id.rowChangePassword).setOnClickListener {
            if (user == null) { Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 10, 40, 10)
            }
            val etCurrent = EditText(this).apply {
                hint = "Current password"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                setPadding(0, 10, 0, 10)
            }
            val etNew = EditText(this).apply {
                hint = "New password (min 6 chars)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                setPadding(0, 10, 0, 10)
            }
            layout.addView(etCurrent)
            layout.addView(etNew)

            AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update") { _, _ ->
                    val currentPass = etCurrent.text.toString()
                    val newPass     = etNew.text.toString()
                    if (currentPass.isEmpty() || newPass.length < 6) {
                        Toast.makeText(this, "Enter current password and new password (min 6 chars)", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    val email2 = user.email ?: return@setPositiveButton
                    val credential = EmailAuthProvider.getCredential(email2, currentPass)
                    user.reauthenticate(credential).addOnCompleteListener { reauth ->
                        if (!reauth.isSuccessful) {
                            Toast.makeText(this, "Current password incorrect", Toast.LENGTH_SHORT).show(); return@addOnCompleteListener
                        }
                        user.updatePassword(newPass).addOnCompleteListener { update ->
                            if (update.isSuccessful) {
                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed: ${update.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Safety contacts
        findViewById<LinearLayout>(R.id.rowSafetyContacts).setOnClickListener {
            startActivity(Intent(this, SafetyContactsActivity::class.java))
        }

        // Sign out
        findViewById<LinearLayout>(R.id.rowSignOut).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        setupNav()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_IMAGE && resultCode == Activity.RESULT_OK && data?.data != null) {
            val sourceUri: Uri = data.data!!
            try {
                // Copy image into app's private storage so it persists across sessions
                val destFile = java.io.File(filesDir, "profile_photo.jpg")
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                val permanentUri = android.net.Uri.fromFile(destFile)

                val ivAvatar  = findViewById<android.widget.ImageView>(R.id.ivProfileAvatar)
                val tvInitial = findViewById<TextView>(R.id.tvProfileInitial)
                ivAvatar.setImageURI(null)           // clear cache
                ivAvatar.setImageURI(permanentUri)
                ivAvatar.visibility  = android.view.View.VISIBLE
                tvInitial.visibility = android.view.View.INVISIBLE

                // Save permanent file path (not the transient gallery URI)
                getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
                    .edit().putString("profile_photo_uri", permanentUri.toString()).apply()

                Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("Profile", "Could not save photo", e)
                Toast.makeText(this, "Could not save photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNav() {
        try { findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }); finish()
        }} catch (_: Exception) {}
        try { findViewById<LinearLayout>(R.id.navTrack).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }} catch (_: Exception) {}
        try { findViewById<LinearLayout>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }} catch (_: Exception) {}
    }
}
