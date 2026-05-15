package com.example.yatrimitra

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ArrivalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arrival)

        val destName    = intent.getStringExtra("dest_name")   ?: "Destination"
        val pickupName  = intent.getStringExtra("pickup_name") ?: "Pickup"
        val fare        = intent.getIntExtra("fare", 50)
        val tipAmount   = intent.getIntExtra("tip_amount", 0)
        val distKm      = intent.getDoubleExtra("dist_km", 2.4)
        val durationMin = intent.getIntExtra("duration_min", 12)
        val driverName  = intent.getStringExtra("driver_name")   ?: "Driver"
        val driverPlate = intent.getStringExtra("driver_plate")  ?: ""
        val driverRating= intent.getStringExtra("driver_rating") ?: ""
        val driverPhone = intent.getStringExtra("driver_phone")  ?: ""
        val vehicle     = intent.getStringExtra("vehicle")       ?: "Auto"
        val rideId      = intent.getStringExtra("ride_id")       ?: ""

        findViewById<TextView>(R.id.tvArrivalDest).text     = destName
        findViewById<TextView>(R.id.tvArrivalDistance).text = "%.1f km".format(distKm)
        findViewById<TextView>(R.id.tvArrivalDuration).text = "$durationMin min"
        findViewById<TextView>(R.id.tvArrivalFare).text     = "₹$fare"

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, PaymentActivity::class.java).apply {
                putExtra("fare",          fare)
                putExtra("tip_amount",    tipAmount)
                putExtra("dist_km",       distKm)
                putExtra("driver_name",   driverName)
                putExtra("driver_plate",  driverPlate)
                putExtra("driver_rating", driverRating)
                putExtra("driver_phone",  driverPhone)
                putExtra("vehicle",       vehicle)
                putExtra("dest_name",     destName)
                putExtra("pickup_name",   pickupName)
                putExtra("ride_id",       rideId)
            })
            finish()
        }, 3000)
    }
}
