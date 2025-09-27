package app.batstats.battery.util

import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.db.BatterySample
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

object TimeEstimator {
    suspend fun capacityGuessMah(): Int {
        val list = BatteryGraph.db.sessionDao().sessionsPaged(1000, 0).first()
        val best = list.mapNotNull { it.estCapacityMah }.maxOrNull()
        return best ?: 4000 // sensible default
    }

    fun etaString(sample: BatterySample?): String? {
        sample ?: return null
        val level = sample.levelPercent
        val currentMa = ((sample.currentNowUa ?: return null) / 1000.0).roundToInt()
        val cap = 4000 // lightweight, async better: capacityGuessMah(); but avoid suspend here
        return if (sample.plugged != 0 && currentMa > 0) {
            val remMah = cap * (100 - level) / 100.0
            val hours = remMah / currentMa
            "Est. full in ${formatHours(hours)}"
        } else if (sample.plugged == 0 && currentMa < 0) {
            val remMah = cap * (level) / 100.0
            val hours = remMah / abs(currentMa)
            "Est. empty in ${formatHours(hours)}"
        } else null
    }

    private fun formatHours(h: Double): String {
        val m = (h * 60).roundToInt().coerceAtLeast(0)
        val hh = m / 60
        val mm = m % 60
        return if (hh > 0) "${hh}h ${mm}m" else "${mm}m"
    }
}