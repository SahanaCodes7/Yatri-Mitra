package com.example.yatrimitra

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PaymentActivity : AppCompatActivity() {

    private var selectedRating = 0
    private val stars          = mutableListOf<ImageView>()
    private var selectedMethod = "Cash"
    private var fare           = 0
    private var tipAmount      = 0
    private var totalPaid      = 0
    private var distKm         = 0.0
    private var driverName     = ""
    private var driverPlate    = ""
    private var driverRating   = ""
    private var driverPhone    = ""
    private var vehicle        = "Auto"
    private var destName       = ""
    private var pickupName     = ""
    private var rideId         = ""

    private lateinit var phase1: View
    private lateinit var phase2: View
    private lateinit var phase3: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_payment)

        fare       = intent.getIntExtra("fare", 50)
        tipAmount  = intent.getIntExtra("tip_amount", 0)
        totalPaid  = fare + tipAmount
        distKm     = intent.getDoubleExtra("dist_km", 2.4)
        driverName = intent.getStringExtra("driver_name") ?: "Your Driver"
        destName   = intent.getStringExtra("dest_name")   ?: "Destination"
        pickupName = intent.getStringExtra("pickup_name") ?: "Pickup"
        rideId       = intent.getStringExtra("ride_id")       ?: ""
        vehicle      = intent.getStringExtra("vehicle")       ?: "Auto"
        driverPlate  = intent.getStringExtra("driver_plate")  ?: ""
        driverRating = intent.getStringExtra("driver_rating") ?: ""
        driverPhone  = intent.getStringExtra("driver_phone")  ?: ""

        phase1 = findViewById(R.id.phase1)
        phase2 = findViewById(R.id.phase2)
        phase3 = findViewById(R.id.phase3)

        // Fill UI
        findViewById<TextView>(R.id.tvPayFare).text       = "₹$totalPaid"
        findViewById<TextView>(R.id.tvPayDest).text       = destName
        findViewById<TextView>(R.id.tvPayDriver).text     = "Driver: $driverName"
        findViewById<TextView>(R.id.tvBreakdown).text     = "%.1f km × ₹20".format(distKm)
        findViewById<TextView>(R.id.tvTipBreakdown).text  = "₹$tipAmount"
        findViewById<TextView>(R.id.tvTotalFare).text     = "₹$totalPaid"
        findViewById<Button>(R.id.btnPayNow).text         = "Pay ₹$totalPaid →"

        // Stars
        listOf(R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5).forEach { id ->
            val iv = findViewById<ImageView>(id); stars.add(iv)
            iv.setOnClickListener { selectedRating = stars.indexOf(iv) + 1; updateStars() }
        }

        // Payment method toggle
        val btnCash = findViewById<LinearLayout>(R.id.btnPayCash)
        val btnUPI  = findViewById<LinearLayout>(R.id.btnPayUPI)
        val btnCard = findViewById<LinearLayout>(R.id.btnPayCard)
        val etUpi   = findViewById<EditText>(R.id.etUpiId)

        btnCash.setOnClickListener { selectMethod("Cash", etUpi); refreshMethods(btnCash, btnUPI, btnCard) }
        btnUPI.setOnClickListener  { selectMethod("UPI", etUpi);  refreshMethods(btnCash, btnUPI, btnCard) }
        btnCard.setOnClickListener { selectMethod("Card", etUpi); refreshMethods(btnCash, btnUPI, btnCard) }

        // Pay now
        findViewById<Button>(R.id.btnPayNow).setOnClickListener {
            if (selectedRating == 0) {
                Toast.makeText(this, "Please rate your driver first", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            showPhase2()
            Handler(Looper.getMainLooper()).postDelayed({ showPhase3() }, 3000)
        }

        findViewById<Button>(R.id.btnBackHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }); finish()
        }
    }

    private fun selectMethod(method: String, etUpi: EditText) {
        selectedMethod = method
        etUpi.visibility = if (method == "UPI") View.VISIBLE else View.GONE
        val msg = when (method) {
            "UPI"  -> "Contacting your UPI app..."
            "Card" -> "Processing card payment..."
            else   -> "Confirming booking..."
        }
        findViewById<TextView>(R.id.tvProcessingMsg).text = msg
    }

    private fun refreshMethods(cash: LinearLayout, upi: LinearLayout, card: LinearLayout) {
        val active = R.drawable.bg_payment_method_selected; val inactive = R.drawable.bg_payment_method
        cash.setBackgroundResource(if (selectedMethod == "Cash") active else inactive)
        upi.setBackgroundResource( if (selectedMethod == "UPI")  active else inactive)
        card.setBackgroundResource(if (selectedMethod == "Card") active else inactive)
        val green = 0xFF1D9E75.toInt(); val grey = 0xFF7FA8C0.toInt()
        (cash.getChildAt(1) as? TextView)?.setTextColor(if (selectedMethod == "Cash") green else grey)
        (upi.getChildAt(1)  as? TextView)?.setTextColor(if (selectedMethod == "UPI")  green else grey)
        (card.getChildAt(1) as? TextView)?.setTextColor(if (selectedMethod == "Card") green else grey)
    }

    private fun showPhase2() { phase1.visibility = View.GONE; phase2.visibility = View.VISIBLE; phase3.visibility = View.GONE }

    private fun showPhase3() {
        phase1.visibility = View.GONE; phase2.visibility = View.GONE; phase3.visibility = View.VISIBLE

        val txnId = "YM" + System.currentTimeMillis().toString().takeLast(8)
        val title = if (selectedMethod == "Cash") "Booking Confirmed!" else "Payment Successful!"

        findViewById<TextView>(R.id.tvSuccessTitle).text  = title
        findViewById<TextView>(R.id.tvTxnId).text         = txnId
        findViewById<TextView>(R.id.tvSuccessAmount).text = "₹$totalPaid"
        findViewById<TextView>(R.id.tvSuccessMethod).text = selectedMethod
        findViewById<TextView>(R.id.tvSuccessDriver).text = driverName

        // Animate success circle
        val circle = findViewById<View>(R.id.successCircle)
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(circle, "scaleX", 0f, 1.1f).setDuration(300),
                ObjectAnimator.ofFloat(circle, "scaleX", 1.1f, 1f).setDuration(100)
            ); start()
        }
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(circle, "scaleY", 0f, 1.1f).setDuration(300),
                ObjectAnimator.ofFloat(circle, "scaleY", 1.1f, 1f).setDuration(100)
            ); start()
        }

        // Save to prefs and Firebase
        saveTrip(txnId)

        // Auto-go home after 5s
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }); finish()
        }, 5000)
    }

    private fun saveTrip(txnId: String) {
        val prefs    = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
        val curTrips = prefs.getInt("total_trips", 0)
        val curSpent = prefs.getFloat("total_spent", 0f)
        prefs.edit()
            .putInt("total_trips", curTrips + 1)
            .putFloat("total_spent", curSpent + totalPaid)
            .putString("pref_last_txn_id",      txnId)
            .putInt("pref_last_txn_amount",      totalPaid)
            .putString("pref_last_txn_method",   selectedMethod)
            .putString("pref_last_driver_name",  driverName)
            .putString("pref_last_driver_plate", driverPlate)
            .putString("pref_last_driver_phone", driverPhone)
            .putString("pref_last_driver_rating",driverRating)
            .apply()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val tripData = hashMapOf(
            "userId"        to uid,
            "route"         to destName,
            "pickup"        to pickupName,
            "fare"          to fare,
            "tip"           to tipAmount,
            "totalPaid"     to totalPaid,
            "paymentMethod" to selectedMethod,
            "txnId"         to txnId,
            "timestamp"     to System.currentTimeMillis(),
            "driverName"    to driverName,
            "driverPlate"   to driverPlate,
            "driverRating"  to driverRating,
            "driverPhone"   to driverPhone,
            "vehicle"       to vehicle,
            "distKm"        to distKm,
            "status"        to "completed"
        )
        if (rideId.isNotEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("ride_requests").child(rideId)
                .child("status").setValue("completed")
        }
        FirebaseDatabase.getInstance().reference
            .child("trip_history").child(uid)
            .push().setValue(tripData)

        // Rating
        if (selectedRating > 0 && rideId.isNotEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("driver_ratings").child(rideId).push()
                .setValue(mapOf("rating" to selectedRating, "uid" to uid, "timestamp" to System.currentTimeMillis()))
        }
    }

    private fun updateStars() {
        stars.forEachIndexed { i, iv ->
            if (i < selectedRating) {
                iv.setImageResource(R.drawable.ic_star); iv.setColorFilter(Color.parseColor("#EF9F27"))
            } else {
                iv.setImageResource(R.drawable.ic_star_border); iv.clearColorFilter()
            }
        }
    }

    override fun onBackPressed() {
        when {
            phase3.visibility == View.VISIBLE -> { /* disabled in phase 3 */ }
            phase2.visibility == View.VISIBLE -> { phase1.visibility = View.VISIBLE; phase2.visibility = View.GONE }
            else -> super.onBackPressed()
        }
    }
}
