package com.example.yatrimitra

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StatsActivity : AppCompatActivity() {

    private val viewModel: SimulationViewModel
        get() = (application as YatriMitraApp).simulationViewModel

    private lateinit var tvTripsToday: TextView
    private lateinit var tvAvgEta: TextView
    private lateinit var llStopStats: LinearLayout
    private lateinit var llAutoProgress: LinearLayout
    private lateinit var llActivityLog: LinearLayout
    private lateinit var tvNoActivity: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        tvTripsToday   = findViewById(R.id.tvTripsToday)
        tvAvgEta       = findViewById(R.id.tvAvgEta)
        llStopStats    = findViewById(R.id.llStopStats)
        llAutoProgress = findViewById(R.id.llAutoProgress)
        llActivityLog  = findViewById(R.id.llActivityLog)
        tvNoActivity   = findViewById(R.id.tvNoActivity)

        observeStats()
        setupNav()
    }

    private fun observeStats() {
        lifecycleScope.launch {
            viewModel.liveStats.collectLatest { stats -> updateUI(stats) }
        }
    }

    private fun updateUI(stats: LiveStats) {
        tvTripsToday.text = stats.tripsToday.toString()
        tvAvgEta.text = if (stats.avgEtaMin > 0) "%.1f".format(stats.avgEtaMin) else "—"

        // ── Busiest stops ─────────────────────────────────
        llStopStats.removeAllViews()
        val stopColors = listOf("#1D9E75","#EF9F27","#3A5270","#185FA5","#993C1D")
        val maxPickups = stats.stopStats.maxOfOrNull { it.pickups } ?: 1
        stats.stopStats.forEachIndexed { index, stop ->
            if (index > 0) llStopStats.addView(divider())
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.addView(View(this).apply {
                setBackgroundColor(Color.parseColor(stopColors.getOrElse(index){"#3A5270"}))
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(36)).apply { marginEnd = dp(12) }
            })
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(TextView(this).apply {
                text = stop.name; textSize = 13f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#E8F1FF"))
            })
            col.addView(TextView(this).apply {
                text = "${stop.pickups} pickups"; textSize = 10f
                setTextColor(Color.parseColor("#7FA8C0"))
            })
            row.addView(col)
            val (bgC, fgC, lbl) = when {
                stop.pickups == 0 -> Triple("#122030","#3A5270","—")
                stop.pickups >= maxPickups * 0.7 -> Triple("#0F4A3A","#1D9E75","High")
                stop.pickups >= maxPickups * 0.4 -> Triple("#412402","#EF9F27","Med")
                else -> Triple("#1A2F45","#7FA8C0","Low")
            }
            row.addView(TextView(this).apply {
                text = lbl; textSize = 10f
                setTextColor(Color.parseColor(fgC))
                setBackgroundColor(Color.parseColor(bgC))
                setPadding(dp(10), dp(4), dp(10), dp(4))
            })
            llStopStats.addView(row)
        }

        // ── Auto progress ──────────────────────────────────
        llAutoProgress.removeAllViews()
        val autoColors = listOf("#1D9E75","#185FA5","#993C1D")
        val autoNames  = listOf("Auto #1","Auto #2","Auto #3")
        stats.autoProgress.forEachIndexed { index, pct ->
            if (index > 0) llAutoProgress.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8))
            })
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.addView(TextView(this).apply {
                text = autoNames.getOrElse(index){"Auto #${index+1}"}
                textSize = 12f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#E8F1FF"))
                layoutParams = LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val track = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#122030"))
                layoutParams = LinearLayout.LayoutParams(0, dp(5), 1f).apply {
                    marginStart = dp(8); marginEnd = dp(8)
                }
            }
            val filled = pct.toFloat().coerceIn(0f, 100f)
            track.addView(View(this).apply {
                setBackgroundColor(Color.parseColor(autoColors.getOrElse(index){"#1D9E75"}))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, filled)
            })
            track.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (100f - filled).coerceAtLeast(0f))
            })
            row.addView(track)
            row.addView(TextView(this).apply {
                text = "${"%.0f".format(pct)}%"; textSize = 11f
                setTextColor(Color.parseColor("#7FA8C0"))
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(dp(36), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            llAutoProgress.addView(row)
        }

        // ── Activity log ───────────────────────────────────
        llActivityLog.removeAllViews()
        if (stats.activityLog.isEmpty()) {
            tvNoActivity.visibility = View.VISIBLE
        } else {
            tvNoActivity.visibility = View.GONE
            stats.activityLog.take(6).forEach { entry ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(5), 0, dp(5))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                row.addView(View(this).apply {
                    setBackgroundColor(Color.parseColor("#1D9E75"))
                    layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(10) }
                })
                row.addView(TextView(this).apply {
                    text = entry.message; textSize = 11f
                    setTextColor(Color.parseColor("#7FA8C0"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                llActivityLog.addView(row)
            }
        }
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(Color.parseColor("#0F1E30"))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
    }

    private fun setupNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP })
        }
        findViewById<LinearLayout>(R.id.navAbout)?.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navHistory)?.setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }
        // Trip history button in stats header
        findViewById<LinearLayout>(R.id.btnViewHistory).setOnClickListener {
            startActivity(Intent(this, TripHistoryActivity::class.java))
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}