package com.example.yatrimitra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActivityLogEntry(val message: String)
data class StopStat(val name: String, val pickups: Int)
data class LiveStats(
    val tripsToday: Int = 0,
    val avgEtaMin: Double = 0.0,
    val stopStats: List<StopStat> = emptyList(),
    val autoProgress: List<Double> = listOf(0.0, 0.0, 0.0),
    val activityLog: List<ActivityLogEntry> = emptyList()
)

class SimulationViewModel : ViewModel() {
    companion object {
        const val ROUTE_LENGTH_KM = 8.0
    }

    private val database = FirebaseDatabase.getInstance().reference

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _liveStats = MutableStateFlow(LiveStats())
    val liveStats: StateFlow<LiveStats> = _liveStats

    private val _elapsedSeconds = MutableStateFlow(0.0)
    val elapsedSeconds: StateFlow<Double> = _elapsedSeconds

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private var timerJob: Job? = null

    val stops = listOf(
        Stop("Town Ctr", 0.0),
        Stop("Market", 1.5),
        Stop("School", 3.2),
        Stop("Hospital", 5.0),
        Stop("Rly Stn", 8.0)
    )

    init {
        listenToGlobalStats()
        // Initialize dummy vehicles for simulation
        _vehicles.value = listOf(
            Vehicle(1, "Auto A", 0.5, 30.0, 0xFF1D9E75.toInt()),
            Vehicle(2, "Auto B", 2.0, 25.0, 0xFFEF9F27.toInt()),
            Vehicle(3, "Auto C", 4.5, 28.0, 0xFF4A90E2.toInt())
        )
    }

    private fun listenToGlobalStats() {
        database.child("trip_logs").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalTrips = snapshot.childrenCount.toInt()
                val logs = mutableListOf<ActivityLogEntry>()
                
                val children = snapshot.children.toList().reversed()
                children.take(10).forEach { log ->
                    val driver = log.child("driver_name").value?.toString() ?: "Auto"
                    val dest = log.child("destination_name").value?.toString() ?: "Location"
                    logs.add(ActivityLogEntry("$driver completed trip to $dest"))
                }

                _liveStats.value = _liveStats.value.copy(
                    tripsToday = totalTrips,
                    activityLog = logs
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun startSimulation() { 
        _isRunning.value = true 
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _isRunning.value) {
                delay(1000)
                _elapsedSeconds.value += 1.0
                
                // Update vehicle positions for simulation
                val currentVehicles = _vehicles.value
                val updatedVehicles = currentVehicles.map { v ->
                    var newPos = v.positionKm + (v.speedKmh / 3600.0)
                    if (newPos > ROUTE_LENGTH_KM) newPos = 0.0
                    v.copy(positionKm = newPos)
                }
                _vehicles.value = updatedVehicles
            }
        }
    }
    
    fun pauseSimulation() { 
        _isRunning.value = false 
        timerJob?.cancel()
    }
    
    fun resetSimulation() {
        _isRunning.value = false
        timerJob?.cancel()
        _elapsedSeconds.value = 0.0
    }
}
