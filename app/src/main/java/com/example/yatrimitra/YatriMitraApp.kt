package com.example.yatrimitra

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class YatriMitraApp : Application(), ViewModelStoreOwner {
    private val appViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = appViewModelStore

    val simulationViewModel: SimulationViewModel by lazy {
        ViewModelProvider(this)[SimulationViewModel::class.java]
    }

    companion object {
        const val PREFS             = "yatri_prefs"
        const val KEY_FIRST_LAUNCH  = "first_launch"
        const val KEY_FAVORITE_STOP = "fav_stop"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_DEFAULT_SPEED = "default_speed"
        const val KEY_TRIP_COUNT    = "trip_count"
        const val KEY_USER_ROLE     = "user_role"
        // Dark mode key — single source of truth used by both YatriMitraApp and SettingsActivity
        const val KEY_DARK_MODE     = "dark_mode_enabled"
    }

    override fun onCreate() {
        super.onCreate()
        val prefs  = getSharedPreferences(PREFS, MODE_PRIVATE)
        // Default to dark theme
        val isDark = prefs.getBoolean(KEY_DARK_MODE, true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
