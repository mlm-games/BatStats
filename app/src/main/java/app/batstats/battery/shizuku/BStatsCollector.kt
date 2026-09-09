package app.batstats.battery.shizuku

import android.util.Log
import app.batstats.battery.data.db.AppEnergyDao
import app.batstats.battery.util.ShellRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import java.util.concurrent.atomic.AtomicBoolean

class BstatsCollector(
    private val dao: AppEnergyDao,
    private val shellRunner: ShellRunner,
    // backward compat
    private val shizuku: ShizukuBridge? = null
) {
    companion object {
        private const val TAG = "BstatsCollector"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = AtomicBoolean(false)
    private var job: Job? = null
    private var last: Map<Int, Double> = emptyMap()

    fun isRunning(): Boolean = running.get()

    fun start(pollSec: Long = 300L) {
        if (!running.compareAndSet(false, true)) return
        job = scope.launch {
            while (isActive) {
                try {
                    val result = shellRunner.run("dumpsys batterystats --checkin")
                    if (result == null) {
                        Log.w(TAG, "No privileged access for batterystats --checkin")
                        delay(5_000)
                        continue
                    }

                    val snap = CheckinParser.parse(result.output.lineSequence())
                    val now = System.currentTimeMillis()

                    if (last.isNotEmpty()) {
                        for ((uid, cur) in snap.energyByUid) {
                            val prev = last[uid] ?: 0.0
                            val delta = max(0.0, cur - prev)
                            if (delta > 0.0001) {
                                // UID-level attribution: only use package name when unambiguous.
                                // Shared/missing mappings stay under "uid:<uid>" so one package
                                // never absorbs a whole shared UID (issue #36).
                                val pkgs = snap.packagesByUid[uid].orEmpty()
                                val key = pkgs.singleOrNull() ?: "uid:$uid"
                                dao.incrementHour(
                                    packageName = key,
                                    atMillis = now,
                                    deltaMah = delta,
                                    addSamples = 1,
                                    mode = result.mode.name
                                )
                            }
                        }
                    }
                    last = snap.energyByUid
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
