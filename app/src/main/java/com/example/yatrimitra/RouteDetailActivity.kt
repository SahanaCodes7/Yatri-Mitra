package com.example.yatrimitra

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RouteDetailActivity : AppCompatActivity() {

    private val routeData = listOf(
        mapOf("name" to "Route 1", "from" to "Town Center", "to" to "Railway Station",
            "stops" to "5", "distance" to "8 km", "time" to "~24 min", "autos" to "3"),
        mapOf("name" to "Route 2", "from" to "Market", "to" to "Bus Stand",
            "stops" to "4", "distance" to "4.5 km", "time" to "~14 min", "autos" to "2")
    )

    private val stopsByRoute = listOf(
        listOf(
            Triple("Town Ctr",  "0.0 km",  "Start"),
            Triple("Market",    "1.5 km",  "1.5 km"),
            Triple("School",    "3.2 km",  "1.7 km"),
            Triple("Hospital",  "5.0 km",  "1.8 km"),
            Triple("Rly Stn",   "8.0 km",  "3.0 km")
        ),
        listOf(
            Triple("Market",    "0.0 km", "Start"),
            Triple("Park",      "1.2 km", "1.2 km"),
            Triple("Hospital",  "2.8 km", "1.6 km"),
            Triple("Bus Stand", "4.5 km", "1.7 km")
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail)

        val routeIndex = intent.getIntExtra("route_index", 0)
        val data  = routeData[routeIndex]
        val stops = stopsByRoute[routeIndex]

        // Header
        findViewById<TextView>(R.id.tvRouteName).text = data["name"]
        findViewById<TextView>(R.id.tvRouteFromTo).text = "${data["from"]}  →  ${data["to"]}"

        // Stats row
        findViewById<TextView>(R.id.tvDetailStops).text    = data["stops"]!!
        findViewById<TextView>(R.id.tvDetailDistance).text = data["distance"]!!
        findViewById<TextView>(R.id.tvDetailTime).text     = data["time"]!!
        findViewById<TextView>(R.id.tvDetailAutos).text    = data["autos"]!!

        // Stop list
        val container = findViewById<LinearLayout>(R.id.stopListContainer)
        stops.forEachIndexed { i, (name, km, gap) ->
            // Gap label (except first)
            if (i > 0) {
                container.addView(TextView(this).apply {
                    text = "↓  $gap"
                    textSize = 10f
                    setTextColor(Color.parseColor("#3A5270"))
                    setPadding(dp(36), dp(4), 0, dp(4))
                })
            }
            // Stop row
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }
            // Dot
            val isFirst = i == 0; val isLast = i == stops.size - 1
            row.addView(TextView(this).apply {
                text = if (isFirst) "🟢" else if (isLast) "🏁" else "⚪"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(dp(32), LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
            })
            row.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@RouteDetailActivity).apply {
                    text = name; textSize = 14f; setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#E8F1FF"))
                })
                addView(TextView(this@RouteDetailActivity).apply {
                    text = km; textSize = 11f
                    setTextColor(Color.parseColor("#7FA8C0"))
                })
            })
            container.addView(row)
        }

        // Buttons
        findViewById<LinearLayout>(R.id.btnStartTracking).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("route_index", routeIndex)
            })
        }
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
        }
        setupNav(routeIndex)
    }

    private fun setupNav(routeIndex: Int) {
        findViewById<LinearLayout>(R.id.navTrack)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navStats)?.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navHistory)?.setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAbout)?.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}