package app.batstats.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.battery.data.db.BatterySample
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SessionDetailsViewModel(
    app: Application,
    private val repo: BatteryRepository,
    private val db: BatteryDatabase,
    private val sessionId: String
) : AndroidViewModel(app) {

    data class Point(val currentMa: Int?, val voltageMv: Int?, val tempC: Double?)
    data class Ui(
        val type: String = "",
        val start: Long = 0L,
        val end: Long? = null,
        val levelRange: String = "",
        val capacityMah: Int? = null,
        val avgCurrent: Long? = null,
        val points: List<Point> = emptyList()
    )
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            db.sessionDao().session(sessionId).combine(
                repo.realtimeFlow
            ) { session, _ -> session }.filterNotNull().collect { s ->
                val end = s.endTime ?: System.currentTimeMillis()
                val samples = repo.samplesBetween(s.startTime, end).first()

                val startPct = (s.startLevel).coerceIn(0, 100)
                val endPct = (s.endLevel ?: samples.lastOrNull()?.levelPercent ?: startPct).coerceIn(0, 100)

                val points = aggregatePerMinute(samples)
                _ui.value = Ui(
                    type = s.type.name,
                    start = s.startTime,
                    end = s.endTime,
                    levelRange = "$startPct% → $endPct%",
                    capacityMah = s.estCapacityMah,
                    avgCurrent = s.avgCurrentUa,
                    points = points
                )
            }
        }
    }

    private fun aggregatePerMinute(samples: List<BatterySample>): List<Point> {
        if (samples.isEmpty()) return emptyList()
        val byMinute = samples.groupBy { it.timestamp / 60000L }
        return byMinute.toSortedMap().values.map { minute ->
            val cur = minute.mapNotNull { it.currentNowUa }.average().takeIf { !it.isNaN() }?.div(1000.0)?.roundToInt()
            val volt = minute.mapNotNull { it.voltageMv }.average().takeIf { !it.isNaN() }?.roundToInt()
            val tempC = minute.mapNotNull { it.temperatureDeciC }.average().takeIf { !it.isNaN() }?.div(10.0)
            Point(cur, volt, tempC)
        }
    }
}