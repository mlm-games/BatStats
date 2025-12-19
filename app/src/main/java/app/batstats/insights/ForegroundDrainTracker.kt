package app.batstats.insights

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageEventsQuery
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.db.AppEnergyDao
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max

/**
 * Tracks foreground app and attributes "excess" current to it (heuristic).
 * Requires Usage Access (PACKAGE_USAGE_STATS).
 */
class ForegroundDrainTracker(
    private val context: Context,
    private val batteryRepo: BatteryRepository,
    private val appEnergyDao: AppEnergyDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val running = AtomicBoolean(false)
    private var job: Job? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        job = scope.launch {
            var lastTs = System.currentTimeMillis()
            var lastPkg: String? = null

            batteryRepo.startSampling() // ensure sampling is on (idempotent)
            batteryRepo.realtimeFlow.collect { rt ->
                val now = rt.sample?.timestamp ?: System.currentTimeMillis()
                val dtHours = max(0.0, (now - lastTs) / 3_600_000.0)

                val pkg = currentForegroundPackage() ?: lastPkg
                if (pkg != null && dtHours > 0.0) {
                    // Heuristic "excess" over a coarse baseline
                    val ma = abs(rt.currentMa.toDouble())
                    val baseline = baselineMilliAmps(screenOn = rt.sample?.screenOn == true)
                    val deltaMa = max(0.0, ma - baseline)
                    val deltaMah = deltaMa * dtHours
                    val samples = 1
                    appEnergyDao.incrementHour(pkg, now, deltaMah, samples)
                }
                lastPkg = pkg
                lastTs = now
            }
        }
    }

    fun stop() {
        running.set(false)
        job?.cancel()
        job = null
    }

    fun isRunning(): Boolean = running.get()

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = try {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } catch (_: Throwable) {
            return false
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        context.startActivity(
            android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun currentForegroundPackage(): String? {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return null
        val end = System.currentTimeMillis()
        val begin = end - 60_000L

        // API 35+: narrow the query to relevant event types
        val events = if (Build.VERSION.SDK_INT >= 35) {
            val q = UsageEventsQuery.Builder(begin, end)
                .setEventTypes(
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED
                )
                .build()
            usm.queryEvents(q)
        } else {
            usm.queryEvents(begin, end)
        } ?: return null

        var lastPkg: String? = null
        var lastTs = -1L
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED && e.timeStamp >= lastTs) {
                lastTs = e.timeStamp
                lastPkg = e.packageName
            }
        }
        return lastPkg
    }

    private fun baselineMilliAmps(screenOn: Boolean): Double =
        if (screenOn) 80.0 else 20.0
}