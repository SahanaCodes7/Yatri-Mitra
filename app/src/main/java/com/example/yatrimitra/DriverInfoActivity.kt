package com.example.yatrimitra

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class DriverInfoActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvDriverEta: TextView
    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverRating: TextView
    private lateinit var tvPlateNumber: TextView
    private lateinit var tvOtp: TextView

    private var pickupPoint: GeoPoint? = null
    private var destPoint: GeoPoint? = null
    private var vehicle    = "Auto"
    private var fare       = 0
    private var tipAmount  = 0
    private var destName   = ""
    private var pickupName = ""
    private var rideId     = ""
    private var distKm     = 0.0

    private var driverMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var destMarker: Marker? = null
    private var routeLine: Polyline? = null

    private val simHandler   = Handler(Looper.getMainLooper())
    private var simRunnable: Runnable? = null
    private val simPath      = mutableListOf<GeoPoint>()
    private var simIndex     = 0
    private var simFraction  = 0.0
    private val SIM_INTERVAL = 120L
    private val SIM_SPEED    = 28.0

    private val http = OkHttpClient()
    private var phaseDriverToPickup = true
    private var tripStartTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        setContentView(R.layout.activity_driver_info)

        pickupPoint = GeoPoint(intent.getDoubleExtra("pickup_lat", 12.97), intent.getDoubleExtra("pickup_lon", 77.59))
        destPoint   = GeoPoint(intent.getDoubleExtra("dest_lat",  12.98), intent.getDoubleExtra("dest_lon",  77.60))
        vehicle    = intent.getStringExtra("vehicle")    ?: "Auto"
        fare       = intent.getIntExtra("fare", 50)
        tipAmount  = intent.getIntExtra("tip_amount", 0)
        destName   = intent.getStringExtra("dest_name")   ?: "Destination"
        pickupName = intent.getStringExtra("pickup_name") ?: "Your Location"
        rideId     = intent.getStringExtra("ride_id")   ?: ""
        distKm     = intent.getDoubleExtra("dist_km", 2.4)

        bindViews(); setupMap(); placeStaticMarkers(); startPhase1()
    }

    private fun bindViews() {
        mapView        = findViewById(R.id.driverMapView)
        tvDriverEta    = findViewById(R.id.tvDriverEta)
        tvDriverName   = findViewById(R.id.tvDriverName)
        tvDriverRating = findViewById(R.id.tvDriverRating)
        tvPlateNumber  = findViewById(R.id.tvPlateNumber)
        tvOtp          = findViewById(R.id.tvOtp)

        tvDriverName.text   = intent.getStringExtra("driver_name")   ?: "Ramesh Kumar"
        tvDriverRating.text = "⭐ ${(intent.getStringExtra("driver_rating") ?: "4.3")}  · Your Driver"
        tvPlateNumber.text  = intent.getStringExtra("driver_plate")  ?: "KA 01 AB 1234"
        tvOtp.text          = (1000..9999).random().toString()

        val driverPhone = intent.getStringExtra("driver_phone") ?: ""

        findViewById<LinearLayout>(R.id.btnCallDriver).setOnClickListener {
            startActivity(Intent(this, InAppCallActivity::class.java).apply {
                putExtra("driver_name",    tvDriverName.text.toString())
                putExtra("driver_vehicle", vehicle)
                putExtra("driver_initial", tvDriverName.text.toString().firstOrNull()?.toString() ?: "D")
                putExtra("driver_phone",   driverPhone)
            })
        }
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(pickupPoint ?: GeoPoint(12.97, 77.59))
    }

    private fun placeStaticMarkers() {
        pickupMarker = Marker(mapView).apply {
            position = pickupPoint!!; icon = drawableToIcon(R.drawable.ic_map_user); title = "Pickup"
        }
        mapView.overlays.add(pickupMarker)
        destMarker = Marker(mapView).apply {
            position = destPoint!!; icon = drawableToIcon(R.drawable.ic_map_destination); title = destName
        }
        mapView.overlays.add(destMarker)
        mapView.invalidate()
    }

    private fun startPhase1() {
        phaseDriverToPickup = true
        val pu = pickupPoint ?: return
        val driverStart = GeoPoint(pu.latitude + 0.005, pu.longitude - 0.004)
        fetchRoute(driverStart, pu) { pts ->
            routeLine?.let { mapView.overlays.remove(it) }
            routeLine = dashedPolyline(pts); mapView.overlays.add(routeLine)
            buildAndStartSim(pts, ::onDriverArrivedAtPickup)
        }
    }

    private fun startPhase2() {
        phaseDriverToPickup = false; tripStartTime = System.currentTimeMillis()
        fetchRoute(pickupPoint!!, destPoint!!) { pts ->
            routeLine?.let { mapView.overlays.remove(it) }
            routeLine = dashedPolyline(pts); mapView.overlays.add(routeLine)
            buildAndStartSim(pts, ::onTripComplete)
        }
    }

    private fun onDriverArrivedAtPickup() {
        tvDriverEta.text = "Driver has arrived! Share OTP to start"
        simHandler.postDelayed({ startPhase2() }, 2500)
    }

    private fun onTripComplete() {
        SoundManager.playDestinationArrived(this)
        tvDriverEta.text = "You have arrived!"
        val elapsed = ((System.currentTimeMillis() - tripStartTime) / 60000).coerceAtLeast(1)
        simHandler.postDelayed({
            startActivity(Intent(this, ArrivalActivity::class.java).apply {
                putExtra("dest_name",    destName)
                putExtra("pickup_name",  pickupName)
                putExtra("vehicle",       vehicle)
                putExtra("driver_plate",  tvPlateNumber.text.toString())
                putExtra("driver_rating", tvDriverRating.text.toString())
                putExtra("fare",         fare)
                putExtra("tip_amount",   tipAmount)
                putExtra("dist_km",      distKm)
                putExtra("duration_min", elapsed.toInt())
                putExtra("driver_name",  tvDriverName.text.toString())
                putExtra("ride_id",      rideId)
                putExtra("driver_phone", intent.getStringExtra("driver_phone") ?: "")
            })
            finish()
        }, 2000)
    }

    private fun buildAndStartSim(pts: List<GeoPoint>, onDone: () -> Unit) {
        stopSim(); simPath.clear(); simPath.addAll(pts); simIndex = 0; simFraction = 0.0
        if (driverMarker == null) {
            driverMarker = Marker(mapView).apply {
                icon = drawableToIcon(R.drawable.ic_map_auto)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                mapView.overlays.add(this)
            }
        }
        driverMarker!!.position = simPath[0]; mapView.invalidate()
        val r = object : Runnable {
            override fun run() {
                if (simPath.size < 2) { onDone(); return }
                val mPerTick = SIM_SPEED / 3.6 * (SIM_INTERVAL / 1000.0); var rem = mPerTick
                while (simIndex < simPath.size - 1 && rem > 0) {
                    val segLen = distM(simPath[simIndex], simPath[simIndex + 1])
                    if (segLen == 0.0) { simIndex++; simFraction = 0.0; continue }
                    val left = segLen - simFraction * segLen
                    if (rem < left) { simFraction += rem / segLen; rem = 0.0 }
                    else { rem -= left; simIndex++; simFraction = 0.0 }
                }
                if (simIndex >= simPath.size - 1) { driverMarker!!.position = simPath.last(); mapView.invalidate(); onDone(); return }
                val pos = interpolate(simPath[simIndex], simPath[simIndex + 1], simFraction)
                driverMarker!!.position = pos; mapView.controller.animateTo(pos); mapView.invalidate()
                val distLeft = totalDist(simPath.drop(simIndex + 1).toMutableList().also { it.add(0, pos) })
                val etaMins  = ((distLeft / 1000.0) / SIM_SPEED * 60).toInt().coerceAtLeast(1)
                tvDriverEta.text = if (phaseDriverToPickup) "$vehicle arriving in $etaMins min" else "Reaching destination in $etaMins min"
                simHandler.postDelayed(this, SIM_INTERVAL)
            }
        }
        simRunnable = r; simHandler.post(r)
    }

    private fun stopSim() { simRunnable?.let { simHandler.removeCallbacks(it) }; simRunnable = null }

    private fun fetchRoute(from: GeoPoint, to: GeoPoint, cb: (List<GeoPoint>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url  = "https://router.project-osrm.org/route/v1/driving/${from.longitude},${from.latitude};${to.longitude},${to.latitude}?overview=full&geometries=geojson"
                val body = http.newCall(Request.Builder().url(url).header("User-Agent", "YatriMitra/1.0").build()).execute().body?.string() ?: ""
                val coords = JSONObject(body).getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                val pts = mutableListOf<GeoPoint>()
                for (i in 0 until coords.length()) { val c = coords.getJSONArray(i); pts.add(GeoPoint(c.getDouble(1), c.getDouble(0))) }
                withContext(Dispatchers.Main) { cb(pts) }
            } catch (_: Exception) { withContext(Dispatchers.Main) { cb(listOf(from, to)) } }
        }
    }

    private fun dashedPolyline(pts: List<GeoPoint>) = Polyline(mapView).apply {
        setPoints(pts); outlinePaint.color = 0xFF1D9E75.toInt(); outlinePaint.strokeWidth = 6f
        outlinePaint.strokeCap = Paint.Cap.ROUND; outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 12f), 0f)
    }

    private fun drawableToIcon(resId: Int): BitmapDrawable {
        val d = ContextCompat.getDrawable(this, resId)!!
        val w = d.intrinsicWidth.takeIf { it > 0 } ?: 80; val h = d.intrinsicHeight.takeIf { it > 0 } ?: 80
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        d.setBounds(0, 0, w, h); d.draw(c); return BitmapDrawable(resources, bmp)
    }

    private fun interpolate(a: GeoPoint, b: GeoPoint, t: Double) = GeoPoint(a.latitude + (b.latitude - a.latitude) * t, a.longitude + (b.longitude - a.longitude) * t)
    private fun distM(a: GeoPoint, b: GeoPoint): Double { val r = FloatArray(1); Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r); return r[0].toDouble() }
    private fun totalDist(pts: List<GeoPoint>): Double { var d = 0.0; for (i in 0 until pts.size - 1) d += distM(pts[i], pts[i + 1]); return d }

    override fun onResume()  { super.onResume();  mapView.onResume() }
    override fun onPause()   { super.onPause();   mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); stopSim(); mapView.onDetach() }
}
