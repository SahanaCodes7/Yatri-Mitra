package com.example.yatrimitra

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TripHistoryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private var listener: ValueEventListener? = null
    private var dbRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_history)
        container = findViewById(R.id.tripsContainer)
        tvEmpty   = findViewById(R.id.tvNoTrips)
        showLocalTrips()
        loadTripsFromFirebase()
        setupNav()
    }

    // ── Data loading ─────────────────────────────────

    private fun showLocalTrips() {
        val prefs   = getSharedPreferences(YatriMitraApp.PREFS, Context.MODE_PRIVATE)
        val lastId  = prefs.getString("pref_last_txn_id",      null) ?: return
        val lastAmt = prefs.getInt("pref_last_txn_amount",     0)
        val lastMth = prefs.getString("pref_last_txn_method",  "Cash") ?: "Cash"
        val lastDriverName  = prefs.getString("pref_last_driver_name",  "Your Driver") ?: "Your Driver"
        val lastDriverPlate = prefs.getString("pref_last_driver_plate", "") ?: ""
        val lastDriverPhone = prefs.getString("pref_last_driver_phone", "") ?: ""
        val lastDriverRating= prefs.getString("pref_last_driver_rating","") ?: ""
        tvEmpty.visibility = View.GONE
        if (container.childCount == 0) {
            container.addView(buildCard(TripData(
                pickup = "Your Location", dest = "Recent Destination",
                driverName = lastDriverName, driverPlate = lastDriverPlate,
                driverRating = lastDriverRating, driverPhone = lastDriverPhone,
                vehicle = "Auto", distKm = 0.0,
                ts = System.currentTimeMillis(),
                totalPaid = lastAmt.toLong(), fare = lastAmt.toLong(), tip = 0L,
                method = lastMth, txnId = lastId
            )))
        }
    }

    private fun loadTripsFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        dbRef    = FirebaseDatabase.getInstance().reference.child("trip_history").child(uid)
        listener = dbRef!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                container.removeAllViews()
                if (!snapshot.exists() || !snapshot.hasChildren()) { showLocalTrips(); return }
                tvEmpty.visibility = View.GONE
                val list = mutableListOf<DataSnapshot>()
                snapshot.children.forEach { list.add(0, it) }
                list.forEach { s ->
                    container.addView(buildCard(TripData(
                        pickup      = s.str("pickup")       ?: "Pickup",
                        dest        = s.str("route")        ?: "Destination",
                        driverName  = s.str("driverName")   ?: "Driver",
                        driverPlate = s.str("driverPlate")  ?: "",
                        driverRating= s.str("driverRating") ?: "",
                        driverPhone = s.str("driverPhone")  ?: "",
                        vehicle     = s.str("vehicle")      ?: "Auto",
                        distKm      = s.dbl("distKm"),
                        ts          = s.lng("timestamp"),
                        totalPaid   = s.lng("totalPaid"),
                        fare        = s.lng("fare"),
                        tip         = s.lng("tip"),
                        method      = s.str("paymentMethod") ?: "Cash",
                        txnId       = s.str("txnId")        ?: ""
                    )))
                }
            }
            override fun onCancelled(e: DatabaseError) { if (container.childCount == 0) showLocalTrips() }
        })
    }

    private data class TripData(
        val pickup: String, val dest: String,
        val driverName: String, val driverPlate: String, val driverRating: String,
        val driverPhone: String,
        val vehicle: String, val distKm: Double, val ts: Long,
        val totalPaid: Long, val fare: Long, val tip: Long,
        val method: String, val txnId: String
    )

    // DataSnapshot helpers
    private fun DataSnapshot.str(k: String) = child(k).getValue(String::class.java)
    private fun DataSnapshot.lng(k: String) = child(k).getValue(Long::class.java)   ?: 0L
    private fun DataSnapshot.dbl(k: String) = child(k).getValue(Double::class.java) ?: 0.0

    // ── Card builder ─────────────────────────────────

    private fun buildCard(t: TripData): LinearLayout {
        val tf    = SimpleDateFormat("hh:mm a",     Locale.getDefault())
        val df    = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date  = if (t.ts > 0) Date(t.ts) else Date()
        val timeStr = tf.format(date)
        val dateStr = df.format(date)

        // Derive timeline times
        val tBooked   = if (t.ts > 0) tf.format(Date(t.ts - 8 * 60_000)) else "—"
        val tArrived  = if (t.ts > 0) tf.format(Date(t.ts - 3 * 60_000)) else "—"
        val tStarted  = if (t.ts > 0) timeStr else "—"
        val estMins   = if (t.distKm > 0) (t.distKm / 28.0 * 60).toInt().coerceAtLeast(5) else 15
        val tReached  = if (t.ts > 0) tf.format(Date(t.ts + estMins * 60_000)) else "—"

        val vIcon = when (t.vehicle.lowercase()) { "bike" -> "🏍️"; "cab" -> "🚗"; else -> "🛺" }
        val vRate = when (t.vehicle.lowercase()) { "bike" -> "₹14/km"; "cab" -> "₹28/km"; else -> "₹20/km" }

        // ── Outer wrapper ──
        val card = vbox().apply {
            setBackgroundResource(R.drawable.bg_card_main)
            layoutParams = lp(MATCH, WRAP).apply { bottomMargin = dp(12) }
        }

        // ── SUMMARY (always visible) ──
        val summary = vbox().apply {
            setPadding(dp(16), dp(14), dp(16), dp(12))
            isClickable = true; isFocusable = true
        }

        // Route dots
        val routeRow = hbox(Gravity.TOP).apply { layoutParams = lp(MATCH, WRAP).apply { bottomMargin = dp(10) } }
        val dotsCol = vbox(Gravity.CENTER_HORIZONTAL).apply {
            layoutParams = lp(dp(18), WRAP).apply { marginEnd = dp(10) }
        }
        dotsCol.addView(colorDot(R.drawable.bg_circle_green).apply {
            layoutParams = lp(dp(10), dp(10)).apply { topMargin = dp(3); gravity = Gravity.CENTER_HORIZONTAL }
        })
        dotsCol.addView(View(this).apply {
            setBackgroundColor(rc(R.color.border))
            layoutParams = lp(dp(2), dp(22)).apply { gravity = Gravity.CENTER_HORIZONTAL }
        })
        dotsCol.addView(colorDot(R.drawable.bg_route_icon_amber).apply {
            layoutParams = lp(dp(10), dp(10)).apply { gravity = Gravity.CENTER_HORIZONTAL }
        })
        val labelsCol = vbox().apply { layoutParams = lp(0, WRAP, 1f) }
        labelsCol.addView(label(t.pickup, 12f, R.color.text_secondary).apply {
            layoutParams = lp(MATCH, WRAP).apply { bottomMargin = dp(10) }
        })
        labelsCol.addView(label(t.dest, 14f, R.color.text_primary, bold = true))
        routeRow.addView(dotsCol); routeRow.addView(labelsCol)
        summary.addView(routeRow)

        // Separator
        summary.addView(hdivider().apply { layoutParams = lp(MATCH, dp(1)).apply { topMargin = dp(6); bottomMargin = dp(10) } })

        // Date · vehicle · driver row
        val metaRow = hbox(Gravity.CENTER_VERTICAL).apply {
            layoutParams = lp(MATCH, WRAP).apply { bottomMargin = dp(6) }
        }
        metaRow.addView(label(dateStr, 11f, R.color.text_muted).apply {
            layoutParams = lp(0, WRAP, 1f)
        })
        metaRow.addView(label("$vIcon ${t.vehicle}", 11f, R.color.text_secondary).apply {
            layoutParams = lp(WRAP, WRAP).apply { marginEnd = dp(8) }
        })
        metaRow.addView(label("· ${t.driverName}", 11f, R.color.text_secondary))
        summary.addView(metaRow)

        // Fare + payment method
        val fareRow = hbox(Gravity.CENTER_VERTICAL).apply { layoutParams = lp(MATCH, WRAP) }
        fareRow.addView(label("₹${t.totalPaid}", 20f, R.color.teal_primary, bold = true).apply {
            layoutParams = lp(0, WRAP, 1f)
        })
        fareRow.addView(chip(t.method))
        summary.addView(fareRow)

        // Expand link
        val tvExpand = label("View trip details  ▾", 11f, R.color.teal_primary).apply {
            layoutParams = lp(MATCH, WRAP).apply { topMargin = dp(8) }
        }
        summary.addView(tvExpand)
        card.addView(summary)

        // ── DETAIL (hidden, toggled on tap) ──
        val detail = buildDetail(t, vIcon, vRate, tBooked, tArrived, tStarted, tReached, dateStr, timeStr)
        detail.visibility = View.GONE
        card.addView(detail)

        var expanded = false
        summary.setOnClickListener {
            expanded = !expanded
            detail.visibility = if (expanded) View.VISIBLE else View.GONE
            tvExpand.text = if (expanded) "Hide details  ▴" else "View trip details  ▾"
        }
        return card
    }

    private fun buildDetail(
        t: TripData, vIcon: String, vRate: String,
        tBooked: String, tArrived: String, tStarted: String, tReached: String,
        dateStr: String, timeStr: String
    ): LinearLayout {
        val d = vbox().apply { setPadding(dp(16), dp(4), dp(16), dp(16)) }

        d.addView(hdivider().apply { layoutParams = lp(MATCH, dp(1)).apply { bottomMargin = dp(14) } })

        // ── TIMELINE ──
        sectionHeader("TRIP TIMELINE", d)
        val timelineItems = listOf(
            Triple(R.drawable.bg_circle_green,     "Ride Booked",           tBooked),
            Triple(R.drawable.bg_route_icon_amber, "$vIcon Driver Arrived", tArrived),
            Triple(R.drawable.bg_circle_green,     "Trip Started",          tStarted),
            Triple(R.drawable.bg_route_icon_amber, "Reached Destination",   tReached)
        )
        timelineItems.forEachIndexed { i, (dotRes, lbl, time) ->
            val row = hbox(Gravity.TOP).apply { layoutParams = lp(MATCH, WRAP) }
            val leftCol = vbox(Gravity.CENTER_HORIZONTAL).apply {
                layoutParams = lp(dp(20), WRAP).apply { marginEnd = dp(12) }
            }
            leftCol.addView(colorDot(dotRes).apply {
                layoutParams = lp(dp(10), dp(10)).apply { topMargin = dp(4); gravity = Gravity.CENTER_HORIZONTAL }
            })
            if (i < timelineItems.size - 1) {
                leftCol.addView(View(this).apply {
                    setBackgroundColor(rc(R.color.border))
                    layoutParams = lp(dp(2), dp(26)).apply { gravity = Gravity.CENTER_HORIZONTAL }
                })
            }
            row.addView(leftCol)
            val rightCol = vbox().apply { layoutParams = lp(0, WRAP, 1f) }
            rightCol.addView(label(lbl, 13f, R.color.text_primary, bold = true).apply {
                layoutParams = lp(MATCH, WRAP).apply { topMargin = dp(2) }
            })
            rightCol.addView(label(time, 11f, R.color.text_muted).apply {
                layoutParams = lp(MATCH, WRAP).apply { topMargin = dp(2); bottomMargin = dp(10) }
            })
            row.addView(rightCol)
            d.addView(row)
        }

        d.addView(hdivider().apply { layoutParams = lp(MATCH, dp(1)).apply { topMargin = dp(4); bottomMargin = dp(14) } })

        // ── DRIVER ──
        sectionHeader("DRIVER DETAILS", d)
        val dCard = hbox(Gravity.CENTER_VERTICAL).apply {
            setBackgroundResource(R.drawable.bg_card_main)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = lp(MATCH, WRAP).apply { bottomMargin = dp(14) }
        }
        // Avatar
        val av = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_avatar_circle)
            layoutParams = lp(dp(46), dp(46)).apply { marginEnd = dp(12) }
        }
        av.addView(TextView(this).apply {
            text = t.driverName.firstOrNull()?.uppercaseChar()?.toString() ?: "D"
            textSize = 18f; setTextColor(rc(R.color.teal_primary))
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
        })
        dCard.addView(av)
        // Info col
        val info = vbox().apply { layoutParams = lp(0, WRAP, 1f) }
        info.addView(label(t.driverName, 15f, R.color.text_primary, bold = true))
        val vehicleRow = hbox(Gravity.CENTER_VERTICAL).apply {
            layoutParams = lp(MATCH, WRAP).apply { topMargin = dp(3); bottomMargin = dp(3) }
        }
        vehicleRow.addView(label("$vIcon ${t.vehicle}", 12f, R.color.text_secondary))
        dCard.addView(info.also { it.addView(vehicleRow) })

        // Plate + rating + phone row beneath name
        if (t.driverPlate.isNotEmpty() || t.driverRating.isNotEmpty() || t.driverPhone.isNotEmpty()) {
            val extraRow = hbox(Gravity.CENTER_VERTICAL).apply {
                layoutParams = lp(MATCH, WRAP).apply { topMargin = dp(2) }
            }
            if (t.driverPlate.isNotEmpty()) {
                extraRow.addView(infoChip("🚘 ${t.driverPlate}", R.color.text_secondary, R.drawable.bg_txn_chip).apply {
                    layoutParams = lp(WRAP, WRAP).apply { marginEnd = dp(6) }
                })
            }
            if (t.driverRating.isNotEmpty() && t.driverRating != "—") {
                val ratingClean = t.driverRating.replace(Regex("[^0-9.]"), "").trim()
                if (ratingClean.isNotEmpty()) {
                    extraRow.addView(infoChip("⭐ $ratingClean", R.color.amber_primary, R.drawable.bg_txn_chip).apply {
                        layoutParams = lp(WRAP, WRAP).apply { marginEnd = dp(6) }
                    })
                }
            }
            info.addView(extraRow)
        }
        if (t.driverPhone.isNotEmpty()) {
            info.addView(label("📞 ${t.driverPhone}", 12f, R.color.text_secondary).apply {
                layoutParams = lp(MATCH, WRAP).apply { topMargin = dp(4) }
            })
        }
        d.addView(dCard)
        d.addView(hdivider().apply { layoutParams = lp(MATCH, dp(1)).apply { bottomMargin = dp(14) } })

        // ── FARE BREAKDOWN ──
        sectionHeader("FARE BREAKDOWN", d)
        val fCard = vbox().apply {
            setBackgroundResource(R.drawable.bg_card_main)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = lp(MATCH, WRAP).apply { bottomMargin = dp(14) }
        }
        // Always show base fare (use totalPaid if fare field is 0)
        val baseFare = if (t.fare > 0) t.fare else t.totalPaid - t.tip
        infoRow(fCard, "$vIcon Base fare  ($vRate)", "₹$baseFare")
        if (t.distKm > 0) infoRow(fCard, "📍 Distance", "%.1f km".format(t.distKm))
        if (t.tip > 0)    infoRow(fCard, "💚 Tip to driver", "+₹${t.tip}")
        fCard.addView(hdivider().apply { layoutParams = lp(MATCH, dp(1)).apply { topMargin = dp(8); bottomMargin = dp(8) } })
        infoRow(fCard, "💰 Total Paid", "₹${t.totalPaid}", valueColor = R.color.teal_primary, bold = true)
        d.addView(fCard)
        d.addView(hdivider().apply { layoutParams = lp(MATCH, dp(1)).apply { bottomMargin = dp(14) } })

        // ── PAYMENT ──
        sectionHeader("PAYMENT", d)
        val pCard = vbox().apply {
            setBackgroundResource(R.drawable.bg_card_main)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = lp(MATCH, WRAP)
        }
        infoRow(pCard, "💳 Method", t.method)
        infoRow(pCard, "🕐 Date & Time", "$dateStr  ·  $timeStr")
        if (t.txnId.isNotEmpty() && t.txnId != "—") {
            infoRow(pCard, "🔖 Txn ID", t.txnId)
        }
        d.addView(pCard)

        return d
    }

    // ── View helpers ─────────────────────────────────

    private fun vbox(g: Int = Gravity.NO_GRAVITY) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        if (g != Gravity.NO_GRAVITY) gravity = g
    }
    private fun hbox(g: Int = Gravity.NO_GRAVITY) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        if (g != Gravity.NO_GRAVITY) gravity = g
    }
    private fun label(text: String, size: Float, colorRes: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text; textSize = size; setTextColor(rc(colorRes))
            if (bold) typeface = Typeface.DEFAULT_BOLD
            layoutParams = lp(WRAP, WRAP)
        }
    private fun chip(text: String) = TextView(this).apply {
        this.text = text; textSize = 11f; setTextColor(rc(R.color.text_secondary))
        setBackgroundResource(R.drawable.bg_txn_chip)
        setPadding(dp(8), dp(3), dp(8), dp(3))
    }
    private fun infoChip(text: String, colorRes: Int, bgRes: Int) = TextView(this).apply {
        this.text = text; textSize = 11f; setTextColor(rc(colorRes))
        setBackgroundResource(bgRes); setPadding(dp(8), dp(3), dp(8), dp(3))
    }
    private fun colorDot(res: Int) = View(this).apply { setBackgroundResource(res) }
    private fun hdivider() = View(this).apply { setBackgroundColor(rc(R.color.border)) }
    private fun sectionHeader(text: String, parent: LinearLayout) {
        parent.addView(TextView(this).apply {
            this.text = text; textSize = 10f; letterSpacing = 0.08f
            setTextColor(rc(R.color.text_muted)); typeface = Typeface.DEFAULT_BOLD
            layoutParams = lp(MATCH, WRAP).apply { bottomMargin = dp(10) }
        })
    }
    private fun infoRow(parent: LinearLayout, lbl: String, value: String,
                        valueColor: Int = R.color.text_primary, bold: Boolean = false) {
        val row = hbox(Gravity.CENTER_VERTICAL).apply {
            layoutParams = lp(MATCH, WRAP).apply { bottomMargin = dp(8) }
        }
        row.addView(label(lbl, 13f, R.color.text_secondary).apply {
            layoutParams = lp(0, WRAP, 1f)
        })
        row.addView(label(value, 13f, valueColor, bold))
        parent.addView(row)
    }

    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP  = LinearLayout.LayoutParams.WRAP_CONTENT
    private fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rc(id: Int) = resources.getColor(id, theme)

    // ── Nav ──────────────────────────────────────────

    private fun setupNav() {
        try { findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }); finish()
        }} catch (_: Exception) {}
        try { findViewById<LinearLayout>(R.id.navTrack).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }} catch (_: Exception) {}
        try { findViewById<LinearLayout>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java)); finish()
        }} catch (_: Exception) {}
    }

    override fun onDestroy() { super.onDestroy(); listener?.let { dbRef?.removeEventListener(it) } }
}
