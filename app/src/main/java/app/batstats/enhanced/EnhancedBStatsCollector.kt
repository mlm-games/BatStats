package app.batstats.enhanced

import app.batstats.battery.data.db.AppEnergyDao
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class EnhancedBstatsCollector(
    private val dao: AppEnergyDao,
    private val shizuku: ShizukuBridge
) {
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
                    val raw = shizuku.run("dumpsys batterystats --checkin") ?: ""
                    val snap = CheckinParser.parse(raw.lineSequence())
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
                } catch (_: Throwable) {
                    // swallow and retry
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
