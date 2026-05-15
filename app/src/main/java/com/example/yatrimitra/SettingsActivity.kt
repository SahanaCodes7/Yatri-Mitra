package com.example.yatrimitra

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)

        // Back
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        // Driver mode
        val switchDriver = findViewById<Switch>(R.id.switchDriverMode)
        switchDriver.isChecked = prefs.getBoolean("driver_mode_enabled", false)
        switchDriver.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("driver_mode_enabled", checked).apply()
            Toast.makeText(this, if (checked) "Driver mode ON — restart to apply" else "Passenger mode ON", Toast.LENGTH_SHORT).show()
        }

        // Arrival sounds
        val switchSounds = findViewById<Switch>(R.id.switchArrivalSounds)
        switchSounds.isChecked = prefs.getBoolean("arrival_sounds_enabled", true)
        switchSounds.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("arrival_sounds_enabled", checked).apply()
        }

        // Dark mode — use unified key, apply immediately via AppCompatDelegate
        val switchDark = findViewById<Switch>(R.id.switchDarkMode)
        switchDark.isChecked = prefs.getBoolean(YatriMitraApp.KEY_DARK_MODE, true)
        switchDark.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(YatriMitraApp.KEY_DARK_MODE, checked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Favourite stop — tap to set via dialog, long-press to clear
        val tvFav = findViewById<TextView>(R.id.tvFavStopValue)
        val fav   = prefs.getString(YatriMitraApp.KEY_FAVORITE_STOP, null)
        tvFav.text = if (fav.isNullOrEmpty()) "None set" else fav

        findViewById<LinearLayout>(R.id.rowFavStop).setOnClickListener {
            val et = EditText(this).apply {
                hint = "e.g. MG Road, Indiranagar"
                setText(prefs.getString(YatriMitraApp.KEY_FAVORITE_STOP, ""))
                setPadding(40, 20, 40, 20)
            }
            AlertDialog.Builder(this)
                .setTitle("Set Favourite Stop")
                .setView(et)
                .setPositiveButton("Save") { _, _ ->
                    val entered = et.text.toString().trim()
                    if (entered.isEmpty()) {
                        prefs.edit().remove(YatriMitraApp.KEY_FAVORITE_STOP).apply()
                        tvFav.text = "None set"
                    } else {
                        prefs.edit().putString(YatriMitraApp.KEY_FAVORITE_STOP, entered).apply()
                        tvFav.text = entered
                        Toast.makeText(this, "Favourite stop saved", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Clear") { _, _ ->
                    prefs.edit().remove(YatriMitraApp.KEY_FAVORITE_STOP).apply()
                    tvFav.text = "None set"
                    Toast.makeText(this, "Favourite stop cleared", Toast.LENGTH_SHORT).show()
                }
                .setNeutralButton("Cancel", null)
                .show()
        }

        // Simulation speed
        val tvSpeed = findViewById<TextView>(R.id.tvSpeedValue)
        val seekBar = findViewById<SeekBar>(R.id.seekSimSpeed)
        val speedMult = prefs.getInt("sim_speed_mult", 1)
        tvSpeed.text = "×$speedMult"; seekBar.progress = speedMult - 1
        findViewByIdOrNull<LinearLayout>(R.id.rowSimSpeed)?.setOnClickListener {
            seekBar.visibility = if (seekBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val mult = progress + 1; tvSpeed.text = "×$mult"
                prefs.edit().putInt("sim_speed_mult", mult).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Location permissions
        findViewByIdOrNull<LinearLayout>(R.id.rowLocationPerms)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }

        // Clear cache
        findViewByIdOrNull<LinearLayout>(R.id.rowClearCache)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Cached Data")
                .setMessage("This will clear search history and cached stops.")
                .setPositiveButton("Clear") { _, _ ->
                    prefs.edit()
                        .remove(YatriMitraApp.KEY_FAVORITE_STOP)
                        .remove("last_search")
                        .apply()
                    tvFav.text = "None set"
                    Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null).show()
        }

        // Reset app
        findViewByIdOrNull<LinearLayout>(R.id.rowResetApp)?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset App Data")
                .setMessage("This will clear ALL app data and preferences. Cannot be undone.")
                .setPositiveButton("Reset") { _, _ ->
                    prefs.edit().clear().apply()
                    Toast.makeText(this, "App data reset", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null).show()
        }

        setupNav()
    }

    private fun <T : View> findViewByIdOrNull(id: Int): T? = try { findViewById(id) } catch (e: Exception) { null }

    private fun setupNav() {
        findViewByIdOrNull<LinearLayout>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }); finish()
        }
        findViewByIdOrNull<LinearLayout>(R.id.navTrack)?.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        findViewByIdOrNull<LinearLayout>(R.id.navHistory)?.setOnClickListener { startActivity(Intent(this, TripHistoryActivity::class.java)) }
        findViewByIdOrNull<LinearLayout>(R.id.navProfile)?.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)); finish() }
    }
}
