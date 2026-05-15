package com.example.yatrimitra

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class RouteMapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var vehicles: List<Vehicle> = emptyList()
    private var stops: List<Stop> = emptyList()
    private var selectedStopIndex: Int = 0
    private val routeLengthKm = SimulationViewModel.ROUTE_LENGTH_KM

    // Road
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A3550"); strokeWidth = 6f
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    // Stop circles
    private val stopFillPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A2E40"); style = Paint.Style.FILL }
    private val stopStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3A5270"); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val selFillPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1D9E75"); style = Paint.Style.FILL }
    private val selStrokePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F6E56"); strokeWidth = 2.5f; style = Paint.Style.STROKE }
    // Passenger dot (amber)
    private val passengerPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EF9F27"); style = Paint.Style.FILL }

    // Labels
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A5270"); textSize = 24f; textAlign = Paint.Align.CENTER
    }
    private val selLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8F1FF"); textSize = 24f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // Vehicle
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 38f; textAlign = Paint.Align.CENTER }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(50, 0, 0, 0); style = Paint.Style.FILL }
    private val badgePaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 18f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    fun updateData(newVehicles: List<Vehicle>, newStops: List<Stop>, newSelectedStop: Int) {
        vehicles = newVehicles; stops = newStops; selectedStopIndex = newSelectedStop
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (stops.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val padH = 55f
        val routeY = h * 0.58f

        fun kmToX(km: Double) = padH + ((km / routeLengthKm) * (w - 2 * padH)).toFloat()

        // Road line
        canvas.drawLine(kmToX(0.0), routeY, kmToX(routeLengthKm), routeY, roadPaint)

        // Stops
        stops.forEachIndexed { i, stop ->
            val x = kmToX(stop.distanceKm)
            val selected = i == selectedStopIndex
            val r = if (selected) 13f else 8f

            canvas.drawCircle(x, routeY, r, if (selected) selFillPaint else stopFillPaint)
            canvas.drawCircle(x, routeY, r, if (selected) selStrokePaint else stopStrokePaint)

            // Passenger dot (amber dot below stop for non-selected stops with activity)
            if (!selected && i % 2 == 1) {
                canvas.drawCircle(x, routeY + r + 8f, 4f, passengerPaint)
            }

            // Label — alternating above/below
            val labelY = if (i % 2 == 0) routeY + 36f else routeY - 24f
            canvas.drawText(stop.name, x, labelY, if (selected) selLabelPaint else labelPaint)
        }

        // Vehicles — auto emoji above the road
        vehicles.forEach { v ->
            val x = kmToX(v.positionKm)
            val iconY = routeY - 14f

            // Shadow ellipse
            canvas.drawOval(x - 14f, routeY + 2f, x + 14f, routeY + 8f, shadowPaint)

            // Flip the auto-rickshaw emoji horizontally so it faces forward (right)
            canvas.save()
            canvas.scale(-1f, 1f, x, iconY)
            canvas.drawText("🛺", x, iconY, emojiPaint)
            canvas.restore()

            // Color badge with vehicle number
            badgePaint.color = v.colorHex
            canvas.drawCircle(x + 15f, iconY - 12f, 12f, badgePaint)
            canvas.drawText(
                v.id.toString(),
                x + 15f,
                iconY - 12f + badgeTextPaint.textSize / 3f,
                badgeTextPaint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 200)
    }
}
