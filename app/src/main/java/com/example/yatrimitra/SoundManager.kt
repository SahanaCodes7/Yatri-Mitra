package com.example.yatrimitra

import android.content.Context
import android.media.RingtoneManager
import android.media.MediaPlayer

object SoundManager {
    fun init(context: Context) { /* no-op */ }
    fun playClick(context: Context) { /* no-op — clicks silent */ }
    fun playArrivingSoon(context: Context) { /* no-op */ }
    fun playArrived(context: Context) { /* no-op */ }

    /** Play when driver accepts a ride */
    fun playDriverAccepted(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mp = MediaPlayer.create(context, uri) ?: return
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (_: Exception) {}
    }

    /** Play when auto arrives at destination */
    fun playDestinationArrived(context: Context) {
        val prefs = context.getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("arrival_sounds_enabled", true)) return
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mp = MediaPlayer.create(context, uri) ?: return
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (_: Exception) {}
    }

    fun release() { /* no-op */ }
}
