package com.example.yatrimitra

data class Vehicle(
    val id: Int,
    val name: String,
    var positionKm: Double,
    val speedKmh: Double,
    val colorHex: Int
) {
    // Simulation Logic — ETA Formula: ETA (minutes) = Distance / Speed * 60
    // Returns null if vehicle has already passed the stop
    fun calculateETA(stopKm: Double): Double? {
        val distance = stopKm - positionKm
        return if (distance > 0) (distance / speedKmh) * 60.0 else null
    }
}
