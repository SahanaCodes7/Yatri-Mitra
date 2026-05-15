package com.example.yatrimitra

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class DriverSearchActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var pulseAnimator1: ObjectAnimator? = null
    private var pulseAnimator2: ObjectAnimator? = null
    private var pulseAnimator3: ObjectAnimator? = null
    private lateinit var mapView: MapView

    private var pickupLat  = 0.0; private var pickupLon  = 0.0
    private var destLat    = 0.0; private var destLon    = 0.0
    private var vehicle    = "Auto"; private var fare     = 50
    private var destName   = ""; private var pickupName  = ""
    private var rideId     = ""; private var distKm      = 2.4
    private var selectedTip = 0

    // Mock driver pool
    private val driverNames  = listOf("Rajan Kumar", "Suresh Rao", "Mahesh Gowda", "Arjun Shetty", "Pradeep Nair")
    private val driverPlates = listOf("KA 01 AB 1234", "KA 05 MN 7890", "KA 03 CD 5678", "KA 09 PQ 2345", "KA 02 XY 9012")
    private val driverRatings = listOf("4.8", "4.7", "4.9", "4.6", "4.8")
    private val driverPhones = listOf("+91 98450 11234", "+91 97312 45678", "+91 96200 56789", "+91 99001 23456", "+91 95678 34567")
    private var chosenDriverIdx = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_driver_search)

        pickupLat  = intent.getDoubleExtra("pickup_lat", 12.97)
        pickupLon  = intent.getDoubleExtra("pickup_lon", 77.59)
        destLat    = intent.getDoubleExtra("dest_lat",  12.98)
        destLon    = intent.getDoubleExtra("dest_lon",  77.60)
        vehicle    = intent.getStringExtra("vehicle")    ?: "Auto"
        fare       = intent.getIntExtra("fare", 50)
        destName   = intent.getStringExtra("dest_name")  ?: "Destination"
        pickupName = intent.getStringExtra("pickup_name") ?: "Pickup"
        rideId     = intent.getStringExtra("ride_id")    ?: ""
        distKm     = intent.getDoubleExtra("dist_km", 2.4)
        chosenDriverIdx = (System.currentTimeMillis() % driverNames.size).toInt()

        mapView = findViewById(R.id.searchMapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(GeoPoint(pickupLat, pickupLon))

        startPulseAnimation()
        showPhase1()
        handler.postDelayed({ showPhase2() }, 5000)

        findViewById<View>(R.id.btnCancelSearch).setOnClickListener { finish() }
    }

    private fun startPulseAnimation() {
        val ring = findViewById<View>(R.id.pulseRing)
        pulseAnimator1 = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 1.8f).apply {
            duration = 1200; repeatCount = ObjectAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
        pulseAnimator2 = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 1.8f).apply {
            duration = 1200; repeatCount = ObjectAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
        pulseAnimator3 = ObjectAnimator.ofFloat(ring, "alpha", 0.6f, 0f).apply {
            duration = 1200; repeatCount = ObjectAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator1?.cancel(); pulseAnimator2?.cancel(); pulseAnimator3?.cancel()
        val ring = findViewById<View>(R.id.pulseRing)
        ring.scaleX = 1f; ring.scaleY = 1f; ring.alpha = 0f
    }

    private fun showPhase1() {
        findViewById<View>(R.id.phaseSearching).visibility   = View.VISIBLE
        findViewById<View>(R.id.phaseDriverFound).visibility = View.GONE
        findViewById<View>(R.id.phaseNoDriver).visibility    = View.GONE
    }

    private fun showPhase2() {
        stopPulseAnimation()
        playAcceptedSound()

        findViewById<View>(R.id.phaseSearching).visibility   = View.GONE
        findViewById<View>(R.id.phaseDriverFound).visibility = View.VISIBLE
        findViewById<View>(R.id.phaseNoDriver).visibility    = View.GONE

        // Show real driver info in phase 2
        val name   = driverNames[chosenDriverIdx]
        val plate  = driverPlates[chosenDriverIdx]
        val rating = driverRatings[chosenDriverIdx]
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

        // Fill driver found card fields (may not exist in old layout; use null-safe)
        findTV(R.id.tvFoundDriverName)?.text   = name
        findTV(R.id.tvFoundDriverPlate)?.text  = plate
        findTV(R.id.tvFoundDriverRating)?.text = "⭐ $rating"
        findTV(R.id.tvFoundDriverInitial)?.text = initials
        findTV(R.id.tvFoundDriverVehicle)?.text = "$vehicle · Arriving in ~3 min"

        // Tip pills
        val tipIds  = listOf(R.id.tip0, R.id.tip20, R.id.tip30, R.id.tip50)
        val tipVals = listOf(0, 20, 30, 50)
        tipIds.forEachIndexed { i, id ->
            val tv = findTV(id) ?: return@forEachIndexed
            tv.setOnClickListener {
                selectedTip = tipVals[i]
                tipIds.forEach { tid ->
                    findTV(tid)?.setBackgroundResource(R.drawable.bg_tip_pill_normal)
                    findTV(tid)?.setTextColor(0xFF7FA8C0.toInt())
                }
                tv.setBackgroundResource(R.drawable.bg_tip_pill_selected)
                tv.setTextColor(0xFF1D9E75.toInt())
            }
        }

        findViewById<android.widget.Button>(R.id.btnConfirmTrip).setOnClickListener {
            startActivity(Intent(this, DriverInfoActivity::class.java).apply {
                putExtra("pickup_lat",   pickupLat);  putExtra("pickup_lon",  pickupLon)
                putExtra("dest_lat",     destLat);    putExtra("dest_lon",    destLon)
                putExtra("vehicle",      vehicle);    putExtra("fare",        fare)
                putExtra("tip_amount",   selectedTip)
                putExtra("dest_name",    destName);   putExtra("pickup_name", pickupName)
                putExtra("ride_id",      rideId);     putExtra("dist_km",     distKm)
                putExtra("driver_name",  name)
                putExtra("driver_plate", plate)
                putExtra("driver_rating",rating)
                putExtra("driver_phone", driverPhones[chosenDriverIdx])
            })
            finish()
        }

        findView(R.id.btnTryAgain)?.setOnClickListener {
            startPulseAnimation()
            showPhase1()
            handler.postDelayed({ showPhase2() }, 5000)
        }
    }

    private fun playAcceptedSound() {
        try {
            val prefs = getSharedPreferences(YatriMitraApp.PREFS, MODE_PRIVATE)
            if (!prefs.getBoolean("arrival_sounds_enabled", true)) return
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri)?.play()
        } catch (_: Exception) {}
    }

    private fun findTV(id: Int): TextView? = try { findViewById(id) } catch (_: Exception) { null }
    private fun findView(id: Int): View?   = try { findViewById(id) } catch (_: Exception) { null }

    override fun onResume()  { super.onResume();  mapView.onResume() }
    override fun onPause()   { super.onPause();   mapView.onPause() }
    override fun onDestroy() {
        super.onDestroy()
        stopPulseAnimation()
        handler.removeCallbacksAndMessages(null)
        mapView.onDetach()
    }
}
