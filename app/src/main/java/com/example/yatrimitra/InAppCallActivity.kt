package com.example.yatrimitra

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class InAppCallActivity : AppCompatActivity() {

    private enum class CallPhase { CONNECTING, CONNECTED, ENDED }
    private var phase = CallPhase.CONNECTING
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var elapsedSeconds = 0
    private var pulseAnim: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_app_call)

        val driverName    = intent.getStringExtra("driver_name")    ?: "Driver"
        val driverInitial = intent.getStringExtra("driver_initial") ?: "D"

        findViewById<TextView>(R.id.tvCallerName).text    = driverName
        findViewById<TextView>(R.id.tvCallerInitial).text = driverInitial

        startPulse()
        startConnecting()

        findViewById<View>(R.id.btnEndCall).setOnClickListener {
            if (phase == CallPhase.CONNECTED) endCall()
            else finish()
        }
        findViewById<View>(R.id.btnCallAgain).setOnClickListener { restartCall() }
        findViewById<View>(R.id.btnBackToDriver).setOnClickListener { finish() }
        findViewById<View>(R.id.btnMute).setOnClickListener { /* mock toggle */ }
        findViewById<View>(R.id.btnSpeaker).setOnClickListener { /* mock toggle */ }
    }

    private fun startConnecting() {
        phase = CallPhase.CONNECTING
        findViewById<TextView>(R.id.tvCallStatus).text = "Connecting..."
        findViewById<View>(R.id.tvCallTimer).visibility         = View.GONE
        findViewById<View>(R.id.callProgressBar).visibility     = View.GONE
        setControlsEnabled(false)
        handler.postDelayed({ startConnected() }, 2000)
    }

    private fun startConnected() {
        phase = CallPhase.CONNECTED
        pulseAnim?.cancel()
        findViewById<TextView>(R.id.tvCallStatus).apply { text = "Connected"; setTextColor(0xFF1D9E75.toInt()) }
        findViewById<View>(R.id.tvCallTimer).visibility         = View.VISIBLE
        findViewById<View>(R.id.callProgressBar).visibility     = View.VISIBLE
        setControlsEnabled(true)
        elapsedSeconds = 0
        timerRunnable = object : Runnable {
            override fun run() {
                elapsedSeconds++
                val m = elapsedSeconds / 60; val s = elapsedSeconds % 60
                findViewById<TextView>(R.id.tvCallTimer).text = "%02d:%02d".format(m, s)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun endCall() {
        phase = CallPhase.ENDED
        timerRunnable?.let { handler.removeCallbacks(it) }
        findViewById<TextView>(R.id.tvCallStatus).apply { text = "Call Ended"; setTextColor(0xFF7FA8C0.toInt()) }
        findViewById<View>(R.id.callControls).visibility      = View.GONE
        findViewById<View>(R.id.callProgressBar).visibility   = View.GONE
        findViewById<View>(R.id.callEndedActions).visibility  = View.VISIBLE
        handler.postDelayed({ finish() }, 4000)
    }

    private fun restartCall() {
        findViewById<View>(R.id.callEndedActions).visibility = View.GONE
        startPulse(); startConnecting()
    }

    private fun startPulse() {
        val ring = findViewById<View>(R.id.callPulseRing)
        pulseAnim = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.3f).apply {
            duration = 1000; repeatCount = ObjectAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
        ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.3f).apply {
            duration = 1000; repeatCount = ObjectAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.4f
        findViewById<View>(R.id.btnMute).alpha    = alpha
        findViewById<View>(R.id.btnSpeaker).alpha = alpha
    }

    override fun onBackPressed() {
        if (phase == CallPhase.CONNECTED) endCall()
        else if (phase == CallPhase.ENDED) finish()
        // Phase CONNECTING: do nothing (prevent back)
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnim?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}
