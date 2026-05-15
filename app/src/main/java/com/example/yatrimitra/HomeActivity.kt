package com.example.yatrimitra

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class HomeActivity : AppCompatActivity() {

    private var passengerMapView: MapView? = null
    private var driverMapView: MapView? = null
    private var rideRequestHandler: Handler? = null
    private var rideRequestRunnable: Runnable? = null
    private var countDownTimer: CountDownTimer? = null
    private var isDriverOnline = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_home)

        val prefs    = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
        val isDriver = prefs.getBoolean("driver_mode_enabled", false)

        if (isDriver) showDriverHome() else showPassengerHome()
        setupNav()
    }

    // ── Passenger Home ─────────────────────────────────────────────
    private fun showPassengerHome() {
        findViewById<View>(R.id.layoutPassengerHome).visibility = View.VISIBLE
        findViewById<View>(R.id.layoutDriverHome).visibility    = View.GONE

        passengerMapView = findViewById<MapView>(R.id.homeMapView).also {
            it.setTileSource(TileSourceFactory.MAPNIK)
            it.setMultiTouchControls(true)
            it.controller.setZoom(14.0)
            it.controller.setCenter(GeoPoint(12.9716, 77.5946))
        }

        val goBook = { _: View -> startActivity(Intent(this, MainActivity::class.java)) }
        findViewById<View>(R.id.btnHomeSearch).setOnClickListener(goBook)
        findViewById<View>(R.id.btnQuickBookContainer).setOnClickListener(goBook)
        findViewById<View>(R.id.btnShortcutHome).setOnClickListener(goBook)
        findViewById<View>(R.id.btnShortcutWork).setOnClickListener(goBook)
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    // ── Driver Home ────────────────────────────────────────────────
    private fun showDriverHome() {
        findViewById<View>(R.id.layoutPassengerHome).visibility = View.GONE
        findViewById<View>(R.id.layoutDriverHome).visibility    = View.VISIBLE

        driverMapView = findViewById<MapView>(R.id.driverHomeMapView).also {
            it.setTileSource(TileSourceFactory.MAPNIK)
            it.setMultiTouchControls(true)
            it.controller.setZoom(14.0)
            it.controller.setCenter(GeoPoint(12.9716, 77.5946))
        }

        val switchOnline       = findViewById<Switch>(R.id.switchDriverOnline)
        val tvStatus           = findViewById<TextView>(R.id.tvDriverOnlineStatus)
        val tvSubtitle         = findViewById<TextView>(R.id.tvDriverOnlineSubtitle)
        val statusCard         = findViewById<View>(R.id.driverStatusCard)
        val rideSheet          = findViewById<View>(R.id.rideRequestSheet)
        val btnAccept          = findViewById<Button>(R.id.btnAcceptRide)
        val btnDecline         = findViewById<Button>(R.id.btnDeclineRide)
        val timerBar           = findViewById<ProgressBar>(R.id.rideRequestTimer)
        val tvCountdown        = findViewById<TextView>(R.id.tvCountdown)

        switchOnline.setOnCheckedChangeListener { _, checked ->
            isDriverOnline = checked
            if (checked) {
                tvStatus.text    = "You are Online"
                tvSubtitle.text  = "Waiting for ride requests..."
                statusCard.setBackgroundResource(R.drawable.bg_driver_home_online)
                scheduleRideRequest(rideSheet, timerBar, tvCountdown, btnAccept, btnDecline)
            } else {
                tvStatus.text    = "You are Offline"
                tvSubtitle.text  = "Toggle to start accepting rides"
                statusCard.setBackgroundResource(R.drawable.bg_driver_home_offline)
                cancelRideRequest(rideSheet)
            }
        }

        btnAccept.setOnClickListener {
            SoundManager.playDriverAccepted(this)
            countDownTimer?.cancel()
            rideSheet.visibility = View.GONE
            Toast.makeText(this, "Ride Accepted! Navigate to pickup.", Toast.LENGTH_SHORT).show()
            try {
                val mapIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=12.9716,77.5946"))
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            } catch (_: Exception) {
                Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show()
            }
            rideRequestHandler?.postDelayed({
                if (isDriverOnline) {
                    scheduleRideRequest(rideSheet, timerBar, tvCountdown, btnAccept, btnDecline)
                }
            }, 20000)
        }

        btnDecline.setOnClickListener {
            countDownTimer?.cancel()
            rideSheet.visibility = View.GONE
            if (isDriverOnline) {
                rideRequestHandler?.postDelayed({
                    scheduleRideRequest(rideSheet, timerBar, tvCountdown, btnAccept, btnDecline)
                }, 20000)
            }
        }
    }

    private fun scheduleRideRequest(
        sheet: View, timerBar: ProgressBar, tvCountdown: TextView,
        btnAccept: Button, btnDecline: Button
    ) {
        rideRequestHandler = Handler(Looper.getMainLooper())
        rideRequestRunnable = Runnable {
            sheet.visibility = View.VISIBLE
            timerBar.progress = 15
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(15000, 1000) {
                override fun onTick(msLeft: Long) {
                    val s = (msLeft / 1000).toInt()
                    tvCountdown.text = "${s}s"
                    timerBar.progress = s
                }
                override fun onFinish() {
                    sheet.visibility = View.GONE
                    if (isDriverOnline) {
                        rideRequestHandler?.postDelayed({
                            scheduleRideRequest(sheet, timerBar, tvCountdown, btnAccept, btnDecline)
                        }, 20000)
                    }
                }
            }.start()
        }
        rideRequestHandler?.postDelayed(rideRequestRunnable!!, 10000)
    }

    private fun cancelRideRequest(sheet: View) {
        countDownTimer?.cancel()
        rideRequestRunnable?.let { rideRequestHandler?.removeCallbacks(it) }
        sheet.visibility = View.GONE
    }

    private fun setupNav() {
        findViewById<View>(R.id.navTrack).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<View>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }
        findViewById<View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume()  { super.onResume();  passengerMapView?.onResume();  driverMapView?.onResume() }
    override fun onPause()   { super.onPause();   passengerMapView?.onPause();   driverMapView?.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        rideRequestRunnable?.let { rideRequestHandler?.removeCallbacks(it) }
        passengerMapView?.onDetach()
        driverMapView?.onDetach()
    }
}
