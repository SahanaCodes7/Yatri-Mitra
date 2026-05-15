package com.example.yatrimitra

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private val slides = listOf(
        Triple("🛺", "Track Your Auto Live",
            "See exactly where shared autos are on the route — in real time, every second."),
        Triple("🔔", "Get Alerts Before It Arrives",
            "Yatri-Mitra tells you when an auto is less than 2 minutes away so you're always ready."),
        Triple("📍", "Pick Your Stop, Know Your ETA",
            "Choose any stop on the route and instantly see the exact ETA for all three autos.")
    )

    private var currentSlide = 0
    private lateinit var tvEmoji: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDesc: TextView
    private lateinit var dotsContainer: LinearLayout
    private lateinit var btnNext: LinearLayout
    private lateinit var btnNextText: TextView
    private lateinit var btnSkip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        tvEmoji        = findViewById(R.id.tvSlideEmoji)
        tvTitle        = findViewById(R.id.tvSlideTitle)
        tvDesc         = findViewById(R.id.tvSlideDesc)
        dotsContainer  = findViewById(R.id.dotsContainer)
        btnNext        = findViewById(R.id.btnNext)
        btnNextText    = findViewById(R.id.tvBtnNext)
        btnSkip        = findViewById(R.id.tvSkip)

        renderSlide(0)

        btnNext.setOnClickListener {
            if (currentSlide < slides.size - 1) {
                currentSlide++
                renderSlide(currentSlide)
            } else {
                goHome()
            }
        }
    }

    private fun renderSlide(index: Int) {
        val (emoji, title, desc) = slides[index]
        tvEmoji.text = emoji
        tvTitle.text = title
        tvDesc.text  = desc
        btnNextText.text = if (index == slides.size - 1) "Get Started →" else "Next →"
        btnSkip.visibility = if (index == slides.size - 1) View.INVISIBLE else View.VISIBLE

        // Dots
        dotsContainer.removeAllViews()
        slides.forEachIndexed { i, _ ->
            val dot = View(this).apply {
                val w = if (i == index) dp(20) else dp(6)
                layoutParams = LinearLayout.LayoutParams(w, dp(6)).apply { marginEnd = dp(5) }
                setBackgroundColor(if (i == index) Color.parseColor("#1D9E75") else Color.parseColor("#1A3550"))
            }
            val roundedDot = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dp(6)).apply { marginEnd = dp(5) }
            }
            dotsContainer.addView(dot)
        }
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}