package app.batstats.battery.shizuku

import android.util.Log
import app.batstats.battery.data.db.AppEnergyDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class BstatsCollector(
    private val dao: AppEnergyDao,
    private val shizuku: ShizukuBridge
) {
    companion object {
        private const val TAG = "BstatsCollector"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = AtomicBoolean(false)
    private var job: Job? = null
    private var last: Map<String, Double> = emptyMap()

    fun isRunning(): Boolean = running.get()

    fun start(pollSec: Long = 300L) {
        if (!running.compareAndSet(false, true)) return
        job = scope.launch {
            while (isActive) {
                try {
                    if (!shizuku.ping() || !shizuku.hasPermission()) {
                        delay(2_000)
                        continue
                    }

                    when (val result = shizuku.run("dumpsys batterystats --checkin")) {
                        is ShizukuBridge.RunResult.Success -> {
                            val snap = CheckinParser.parse(result.output.lineSequence())
                            val now = System.currentTimeMillis()

                            if (last.isNotEmpty()) {
                                for ((pkg, cur) in snap.perPackageMah) {
                                    val prev = last[pkg] ?: 0.0
                                    val delta = max(0.0, cur - prev)
                                    if (delta > 0.0001) {
                                        dao.incrementHour(
                                            packageName = pkg,
                                            atMillis = now,
                                            deltaMah = delta,
                                            addSamples = 1,
                                            mode = "SHIZUKU"
                                        )
                                    }
                                }
                            }
                            last = snap.perPackageMah
                        }
                        is ShizukuBridge.RunResult.Error -> {
                            Log.w(TAG, "Command failed: ${result.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling loop", e)
                }
                delay(pollSec * 1000L)
            }
        }
    }

    fun stop() {
        running.set(false)
        job?.cancel()
        job = null
        last = emptyMap()
    }
}