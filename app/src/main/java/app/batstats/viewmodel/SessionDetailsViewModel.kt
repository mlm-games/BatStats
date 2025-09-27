package app.batstats.viewmodel

import android.app.Application
import androidx.lifecycle.*
import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.db.BatterySample
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SessionDetailsViewModel(private val app: Application, private val sessionId: String) : AndroidViewModel(app) {
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
            BatteryGraph.db.sessionDao().session(sessionId).combine(
                BatteryGraph.repo.realtime
            ) { session, _ -> session }.filterNotNull().collect { s ->
                val end = s.endTime ?: System.currentTimeMillis()
                val samples = BatteryGraph.repo.samplesBetween(s.startTime, end).first()
                val points = aggregatePerMinute(samples)
                val cap = s.estCapacityMah
                val avg = s.avgCurrentUa
                _ui.value = Ui(
                    type = s.type.name,
                    start = s.startTime,
                    end = s.endTime,
                    levelRange = "${samples.firstOrNull()?.levelPercent ?: 0}% → ${samples.lastOrNull()?.levelPercent ?: 0}%",
                    capacityMah = cap,
                    avgCurrent = avg,
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

    companion object {
        fun factory(id: String) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SessionDetailsViewModel(BatteryGraph.app, id) as T
            }
        }
    }
}
