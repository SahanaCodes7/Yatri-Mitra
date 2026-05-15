package com.example.yatrimitra

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

class SafetyContactsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private val contacts = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_contacts)

        container = findViewById(R.id.contactsContainer)
        loadContacts()
        renderContacts()

        findViewById<LinearLayout>(R.id.btnAddContact).setOnClickListener { showAddDialog() }
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        setupNav()
    }

    private fun showAddDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        val etName = EditText(this).apply {
            hint = "Contact Name (e.g. Mom)"
            setTextColor(Color.parseColor("#E8F1FF"))
            setHintTextColor(Color.parseColor("#3A5270"))
            setBackgroundColor(Color.parseColor("#122030"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val etPhone = EditText(this).apply {
            hint = "Phone Number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setTextColor(Color.parseColor("#E8F1FF"))
            setHintTextColor(Color.parseColor("#3A5270"))
            setBackgroundColor(Color.parseColor("#122030"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(etName); layout.addView(etPhone)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Safety Contact")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val name  = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    contacts.add(JSONObject().apply { put("name", name); put("phone", phone) })
                    saveContacts(); renderContacts()
                } else Toast.makeText(this, "Fill in both fields", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dark_card)
    }

    private fun renderContacts() {
        container.removeAllViews()
        if (contacts.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No emergency contacts yet.\nTap Add Contact below."
                textSize = 13f; setTextColor(Color.parseColor("#3A5270"))
                gravity = Gravity.CENTER
                setPadding(0, dp(24), 0, dp(24))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }); return
        }

        contacts.forEachIndexed { i, contact ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#1A2E40"))
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
            }

            // Avatar circle initial
            val initial = contact.optString("name", "?").firstOrNull()?.uppercaseChar() ?: '?'
            card.addView(TextView(this).apply {
                text = initial.toString()
                textSize = 16f; setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setBackgroundResource(R.drawable.bg_circle_green)
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) }
            })

            // Name + phone col
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(TextView(this).apply {
                text = contact.optString("name"); textSize = 14f
                setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#E8F1FF"))
            })
            col.addView(TextView(this).apply {
                text = contact.optString("phone"); textSize = 12f
                setTextColor(Color.parseColor("#7FA8C0"))
            })
            card.addView(col)

            // Call button (material icon)
            val callBtn = ImageView(this).apply {
                setImageResource(R.drawable.ic_phone)
                setColorFilter(Color.parseColor("#1D9E75"))
                setPadding(dp(8), dp(4), dp(8), dp(4))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
                setOnClickListener {
                    val phone = contact.optString("phone")
                    startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone")))
                }
            }
            card.addView(callBtn)

            // Delete button (material icon)
            val delBtn = ImageView(this).apply {
                setImageDrawable(ContextCompat.getDrawable(this@SafetyContactsActivity, R.drawable.ic_report))
                setColorFilter(Color.parseColor("#B04020"))
                setPadding(dp(6), dp(4), 0, dp(4))
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(40))
                setOnClickListener {
                    contacts.removeAt(i); saveContacts(); renderContacts()
                }
            }
            card.addView(delBtn)
            container.addView(card)
        }
    }

    private fun saveContacts() {
        val arr = JSONArray(); contacts.forEach { arr.put(it) }
        getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
            .edit().putString("safety_contacts", arr.toString()).apply()
    }

    private fun loadContacts() {
        val raw = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
            .getString("safety_contacts", null) ?: return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) contacts.add(arr.getJSONObject(i))
        } catch (e: Exception) { /* ignore */ }
    }

    private fun setupNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP })
        }
        findViewById<LinearLayout>(R.id.navTrack).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
