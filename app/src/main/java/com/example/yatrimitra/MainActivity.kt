package com.example.yatrimitra

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : AppCompatActivity() {

    private enum class Step { SEARCH, PICKUP, SELECT_VEHICLE }
    private var step = Step.SEARCH
    private var selectedVehicle = "Auto"

    private var userLocation: GeoPoint? = null
    private var destinationPoint: GeoPoint? = null
    private var pickupPoint: GeoPoint? = null
    private var destName = ""
    private var pickupName = ""
    private var distanceKm = 0.0

    private lateinit var mapView: MapView
    private lateinit var llSearchHeader: View
    private lateinit var llPickupContainer: View
    private lateinit var llVehiclePanel: View
    private lateinit var tvStepTitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var llSuggestions: LinearLayout
    private lateinit var etPickup: EditText
    private lateinit var tvFromLabel: TextView
    private lateinit var tvToLabel: TextView
    private lateinit var tvDistanceLabel: TextView
    private lateinit var tvAutoPrice: TextView
    private lateinit var tvCabPrice: TextView
    private lateinit var tvBikePrice: TextView
    private lateinit var btnBookRide: Button
    private lateinit var arrivalBanner: View
    private lateinit var tvArrivalAlert: TextView
    private lateinit var btnSelectAuto: LinearLayout
    private lateinit var btnSelectCab: LinearLayout
    private lateinit var btnSelectBike: LinearLayout

    private var destMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var myLocMarker: Marker? = null
    private var routeLine: Polyline? = null

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCb: LocationCallback
    private val http = OkHttpClient()
    private lateinit var db: DatabaseReference
    private val suggestHandler = Handler(Looper.getMainLooper())
    private var suggestRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        setContentView(R.layout.activity_main)
        db = FirebaseDatabase.getInstance().reference
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        bindViews(); setupMap(); setupListeners(); startLocationTracking(); showStep(Step.SEARCH)
    }

    private fun bindViews() {
        mapView           = findViewById(R.id.mapView)
        llSearchHeader    = findViewById(R.id.llSearchHeader)
        llPickupContainer = findViewById(R.id.llPickupContainer)
        llVehiclePanel    = findViewById(R.id.llVehiclePanel)
        tvStepTitle       = findViewById(R.id.tvStepTitle)
        etSearch          = findViewById(R.id.etSearchLocation)
        llSuggestions     = findViewById(R.id.llSuggestions)
        etPickup          = findViewById(R.id.etPickupLocation)
        tvFromLabel       = findViewById(R.id.tvFromLabel)
        tvToLabel         = findViewById(R.id.tvToLabel)
        tvDistanceLabel   = findViewById(R.id.tvDistanceLabel)
        tvAutoPrice       = findViewById(R.id.tvAutoPrice)
        tvCabPrice        = findViewById(R.id.tvCabPrice)
        tvBikePrice       = findViewById(R.id.tvBikePrice)
        btnBookRide       = findViewById(R.id.btnBookRide)
        arrivalBanner     = findViewById(R.id.arrivalBanner)
        tvArrivalAlert    = findViewById(R.id.tvArrivalAlert)
        btnSelectAuto     = findViewById(R.id.btnSelectAuto)
        btnSelectCab      = findViewById(R.id.btnSelectCab)
        btnSelectBike     = findViewById(R.id.btnSelectBike)
        findViewById<View>(R.id.btnBack).setOnClickListener  { onBackPressed() }
        findViewById<View>(R.id.btnBack2).setOnClickListener { showStep(Step.SEARCH) }
        findViewById<View>(R.id.btnReset).setOnClickListener { resetAll() }
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(12.9716, 77.5946))
    }

    private fun showStep(s: Step) {
        step = s
        llSearchHeader.visibility    = if (s == Step.SEARCH)         View.VISIBLE else View.GONE
        llPickupContainer.visibility = if (s == Step.PICKUP)         View.VISIBLE else View.GONE
        llVehiclePanel.visibility    = if (s == Step.SELECT_VEHICLE) View.VISIBLE else View.GONE
        arrivalBanner.visibility     = View.GONE
        when (s) {
            Step.SEARCH         -> tvStepTitle.text = "Where to?"
            Step.PICKUP         -> {}
            Step.SELECT_VEHICLE -> { tvToLabel.text = destName.take(22); tvFromLabel.text = pickupName.take(22); calculateAndShowPrices(); highlightVehicle("Auto") }
        }
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                suggestRunnable?.let { suggestHandler.removeCallbacks(it) }
                val q = s.toString().trim()
                if (q.length < 2) { llSuggestions.visibility = View.GONE; return }
                suggestRunnable = Runnable { fetchSuggestions(q) }
                suggestHandler.postDelayed(suggestRunnable!!, 400)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        findViewById<View>(R.id.btnSearch).setOnClickListener {
            val q = etSearch.text.toString().trim()
            if (q.isNotEmpty()) geocodeAndSet(q, true)
        }
        etSearch.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) { geocodeAndSet(etSearch.text.toString().trim(), true); true } else false
        }
        findViewById<View>(R.id.btnPickupCurrent).setOnClickListener {
            if (userLocation != null) {
                setPickup(userLocation!!, "Your Location")
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Please grant location permission", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val btn = it
                btn.isEnabled = false
                Toast.makeText(this, "Getting your location…", Toast.LENGTH_SHORT).show()
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        userLocation = GeoPoint(loc.latitude, loc.longitude)
                        btn.isEnabled = true
                        setPickup(userLocation!!, "Your Location")
                    } else {
                        // Request a single fresh GPS fix
                        val oneShot = object : LocationCallback() {
                            override fun onLocationResult(r: LocationResult) {
                                fusedClient.removeLocationUpdates(this)
                                r.lastLocation?.let { fresh ->
                                    userLocation = GeoPoint(fresh.latitude, fresh.longitude)
                                    runOnUiThread {
                                        btn.isEnabled = true
                                        setPickup(userLocation!!, "Your Location")
                                    }
                                }
                            }
                        }
                        fusedClient.requestLocationUpdates(
                            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                                .setMaxUpdates(1).build(),
                            oneShot, mainLooper
                        )
                        // Safety timeout — re-enable after 10 s if GPS never responds
                        Handler(Looper.getMainLooper()).postDelayed({
                            btn.isEnabled = true
                            if (userLocation == null)
                                Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                        }, 10_000)
                    }
                }
            }
        }
        findViewById<View>(R.id.btnConfirmPickup).setOnClickListener {
            val q = etPickup.text.toString().trim()
            if (q.isNotEmpty()) geocodeAndSet(q, false)
            else Toast.makeText(this, "Enter a pickup address", Toast.LENGTH_SHORT).show()
        }
        btnSelectAuto.setOnClickListener  { highlightVehicle("Auto") }
        btnSelectCab.setOnClickListener   { highlightVehicle("Cab") }
        btnSelectBike.setOnClickListener  { highlightVehicle("Bike") }
        btnBookRide.setOnClickListener    { launchDriverSearch() }
    }

    private fun fetchSuggestions(q: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url  = "https://nominatim.openstreetmap.org/search?q=${q.replace(" ", "+")}&format=json&limit=4&countrycodes=in"
                val body = http.newCall(Request.Builder().url(url).header("User-Agent", "YatriMitra/1.0").build()).execute().body?.string() ?: return@launch
                val arr  = JSONArray(body)
                withContext(Dispatchers.Main) {
                    llSuggestions.removeAllViews()
                    if (arr.length() == 0) { llSuggestions.visibility = View.GONE; return@withContext }
                    llSuggestions.visibility = View.VISIBLE
                    for (i in 0 until arr.length()) {
                        val obj       = arr.getJSONObject(i)
                        val fullName  = obj.getString("display_name")
                        val shortName = fullName.split(",")[0]
                        val lat       = obj.getDouble("lat"); val lon = obj.getDouble("lon")
                        val row       = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity     = android.view.Gravity.CENTER_VERTICAL
                            setPadding(dp(12), dp(12), dp(12), dp(12))
                            isClickable = true; isFocusable = true
                        }
                        val icon = ImageView(this@MainActivity).apply {
                            setImageResource(R.drawable.ic_place)
                            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { marginEnd = dp(10) }
                        }
                        val col = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
                        col.addView(TextView(this@MainActivity).apply { text = shortName; textSize = 12f; setTextColor(0xFFE8F1FF.toInt()) })
                        col.addView(TextView(this@MainActivity).apply { text = fullName.take(50); textSize = 10f; setTextColor(0xFF7FA8C0.toInt()) })
                        row.addView(icon); row.addView(col)
                        if (i < arr.length() - 1) {
                            val div = View(this@MainActivity).apply {
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)); setBackgroundColor(0x221A3550)
                            }
                            row.setOnClickListener { hideKeyboard(); llSuggestions.visibility = View.GONE; etSearch.setText(shortName); setDestination(GeoPoint(lat, lon), shortName) }
                            llSuggestions.addView(row); llSuggestions.addView(div)
                        } else {
                            row.setOnClickListener { hideKeyboard(); llSuggestions.visibility = View.GONE; etSearch.setText(shortName); setDestination(GeoPoint(lat, lon), shortName) }
                            llSuggestions.addView(row)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun geocodeAndSet(query: String, isDestination: Boolean) {
        if (query.isEmpty()) return
        hideKeyboard()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url  = "https://nominatim.openstreetmap.org/search?q=${query.replace(" ", "+")}&format=json&limit=1&countrycodes=in"
                val body = http.newCall(Request.Builder().url(url).header("User-Agent", "YatriMitra/1.0").build()).execute().body?.string() ?: return@launch
                val arr  = JSONArray(body)
                if (arr.length() > 0) {
                    val obj  = arr.getJSONObject(0)
                    val pt   = GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"))
                    val name = obj.getString("display_name").split(",")[0]
                    withContext(Dispatchers.Main) { if (isDestination) setDestination(pt, name) else setPickup(pt, name) }
                } else withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Location not found", Toast.LENGTH_SHORT).show() }
            } catch (_: Exception) {}
        }
    }

    private fun setDestination(point: GeoPoint, name: String) {
        destinationPoint = point; destName = name
        llSuggestions.visibility = View.GONE
        destMarker?.let { mapView.overlays.remove(it) }
        destMarker = Marker(mapView).apply {
            position = point; title = name
            icon = drawableToBitmapDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_map_destination)!!)
        }
        mapView.overlays.add(destMarker)
        mapView.controller.animateTo(point); mapView.controller.setZoom(15.0); mapView.invalidate()
        showStep(Step.PICKUP)
    }

    private fun setPickup(point: GeoPoint, name: String) {
        pickupPoint = point; pickupName = name
        pickupMarker?.let { mapView.overlays.remove(it) }
        pickupMarker = Marker(mapView).apply {
            position = point; title = name
            icon = drawableToBitmapDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_map_user)!!)
        }
        mapView.overlays.add(pickupMarker)
        fetchRouteAndDraw(point, destinationPoint!!) { pts, km ->
            distanceKm = km; drawRouteLine(pts)
            val midLat = (point.latitude + destinationPoint!!.latitude) / 2
            val midLon = (point.longitude + destinationPoint!!.longitude) / 2
            mapView.controller.animateTo(GeoPoint(midLat, midLon)); mapView.controller.setZoom(13.0); mapView.invalidate()
            showStep(Step.SELECT_VEHICLE)
        }
    }

    private fun fetchRouteAndDraw(from: GeoPoint, to: GeoPoint, onResult: (List<GeoPoint>, Double) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url  = "https://router.project-osrm.org/route/v1/driving/${from.longitude},${from.latitude};${to.longitude},${to.latitude}?overview=full&geometries=geojson"
                val body = http.newCall(Request.Builder().url(url).header("User-Agent", "YatriMitra/1.0").build()).execute().body?.string() ?: ""
                val json   = JSONObject(body)
                val route  = json.getJSONArray("routes").getJSONObject(0)
                val distM  = route.getDouble("distance")
                val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
                val pts    = mutableListOf<GeoPoint>()
                for (i in 0 until coords.length()) { val c = coords.getJSONArray(i); pts.add(GeoPoint(c.getDouble(1), c.getDouble(0))) }
                withContext(Dispatchers.Main) { onResult(pts, distM / 1000.0) }
            } catch (_: Exception) {
                val d = FloatArray(1); Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, d)
                withContext(Dispatchers.Main) { onResult(listOf(from, to), d[0] / 1000.0) }
            }
        }
    }

    private fun drawRouteLine(pts: List<GeoPoint>) {
        routeLine?.let { mapView.overlays.remove(it) }
        routeLine = Polyline(mapView).apply {
            setPoints(pts); outlinePaint.color = 0xFF1D9E75.toInt()
            outlinePaint.strokeWidth = 7f; outlinePaint.strokeCap = Paint.Cap.ROUND; outlinePaint.strokeJoin = Paint.Join.ROUND
        }
        mapView.overlays.add(0, routeLine); mapView.invalidate()
    }

    private fun calculateAndShowPrices() {
        val km       = distanceKm.coerceAtLeast(1.0)
        val bikeFare = (km * 14).toInt()   // ₹14/km — cheapest, single rider
        val autoFare = (km * 20).toInt()   // ₹20/km — standard
        val cabFare  = (km * 28).toInt()   // ₹28/km — AC cab, most comfort
        tvBikePrice.text = "₹$bikeFare"
        tvAutoPrice.text = "₹$autoFare"
        tvCabPrice.text  = "₹$cabFare"
        tvDistanceLabel.text = "%.1f km".format(distanceKm)
        btnBookRide.text = "Book Auto — ₹$autoFare"
    }

    private fun fareForVehicle(type: String): Int {
        val km = distanceKm.coerceAtLeast(1.0)
        return when (type) {
            "Bike" -> (km * 14).toInt()
            "Cab"  -> (km * 28).toInt()
            else   -> (km * 20).toInt()
        }
    }

    private fun highlightVehicle(type: String) {
        selectedVehicle = type
        btnSelectAuto.setBackgroundResource(if (type == "Auto") R.drawable.bg_ride_card_selected else R.drawable.bg_ride_card_normal)
        btnSelectCab.setBackgroundResource( if (type == "Cab")  R.drawable.bg_ride_card_selected else R.drawable.bg_ride_card_normal)
        btnSelectBike.setBackgroundResource(if (type == "Bike") R.drawable.bg_ride_card_selected else R.drawable.bg_ride_card_normal)
        btnBookRide.text = "Book $type — ₹${fareForVehicle(type)}"
        val green = 0xFF1D9E75.toInt(); val grey = 0xFF7FA8C0.toInt()
        tvAutoPrice.setTextColor(if (type == "Auto") green else grey)
        tvCabPrice.setTextColor( if (type == "Cab")  green else grey)
        tvBikePrice.setTextColor(if (type == "Bike") green else grey)
    }

    private fun launchDriverSearch() {
        val pu = pickupPoint ?: run { Toast.makeText(this, "Set pickup first", Toast.LENGTH_SHORT).show(); return }
        val dp = destinationPoint ?: run { Toast.makeText(this, "Set destination first", Toast.LENGTH_SHORT).show(); return }
        val fare = fareForVehicle(selectedVehicle)
        val id   = db.child("ride_requests").push().key ?: "local"
        db.child("ride_requests").child(id).setValue(mapOf("pickup_lat" to pu.latitude, "pickup_lon" to pu.longitude, "dest_lat" to dp.latitude, "dest_lon" to dp.longitude, "vehicle" to selectedVehicle, "fare" to fare, "status" to "searching"))
        startActivity(Intent(this, DriverSearchActivity::class.java).apply {
            putExtra("pickup_lat",  pu.latitude); putExtra("pickup_lon", pu.longitude)
            putExtra("dest_lat",    dp.latitude); putExtra("dest_lon",   dp.longitude)
            putExtra("vehicle",     selectedVehicle); putExtra("fare", fare)
            putExtra("dest_name",   destName); putExtra("pickup_name", pickupName)
            putExtra("ride_id",     id); putExtra("dist_km", distanceKm)
        })
    }

    private fun resetAll() {
        listOf(destMarker, pickupMarker).forEach { it?.let { m -> mapView.overlays.remove(m) } }
        routeLine?.let { mapView.overlays.remove(it) }
        destMarker = null; pickupMarker = null; routeLine = null
        destinationPoint = null; pickupPoint = null; distanceKm = 0.0
        etSearch.setText(""); etPickup.setText("")
        showStep(Step.SEARCH); mapView.invalidate()
    }

    // ── Real icon helpers ────────────────────────────────────────
    private fun drawableToBitmapDrawable(drawable: Drawable): BitmapDrawable {
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: dp(40)
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: dp(40)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c   = Canvas(bmp); drawable.setBounds(0, 0, w, h); drawable.draw(c)
        return BitmapDrawable(resources, bmp)
    }

    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1); return
        }
        // Fast-path: use last known location immediately so "Use current location" works right away
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                userLocation = GeoPoint(loc.latitude, loc.longitude)
                updateMyLocationMarker(loc.latitude, loc.longitude)
            }
        }
        // Live updates for ongoing accuracy
        locationCb = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) {
                r.lastLocation?.let { loc ->
                    userLocation = GeoPoint(loc.latitude, loc.longitude)
                    updateMyLocationMarker(loc.latitude, loc.longitude)
                }
            }
        }
        fusedClient.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build(),
            locationCb, mainLooper
        )
    }

    private fun updateMyLocationMarker(lat: Double, lon: Double) {
        myLocMarker?.let { mapView.overlays.remove(it) }
        myLocMarker = Marker(mapView).apply {
            position = GeoPoint(lat, lon)
            icon = drawableToBitmapDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_map_user)!!)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        mapView.overlays.add(myLocMarker); mapView.invalidate()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationTracking() // retry now that permission is granted
        }
    }

    private fun hideKeyboard() { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(currentFocus?.windowToken, 0) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onResume()  { super.onResume();  mapView.onResume() }
    override fun onPause()   { super.onPause();   mapView.onPause() }
    override fun onDestroy() { super.onDestroy(); if (::fusedClient.isInitialized && ::locationCb.isInitialized) fusedClient.removeLocationUpdates(locationCb); mapView.onDetach() }
}
