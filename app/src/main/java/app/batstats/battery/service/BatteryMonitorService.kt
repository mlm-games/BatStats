package app.batstats.battery.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.batstats.battery.BatteryGraph
import app.batstats.battery.shizuku.BstatsCollector
import app.batstats.battery.shizuku.ShizukuBridge
import app.batstats.battery.util.Notifier
import app.batstats.battery.widget.WidgetUpdater
import app.batstats.insights.ForegroundDrainTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BatteryMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val drainTracker: ForegroundDrainTracker by inject()
    private val shizukuBridge: ShizukuBridge by inject()
    private val enhancedCollector: BstatsCollector by inject()

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = Notifier.monitoringNotification(this, "Starting…")
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    Notifier.NOTIF_ID,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(Notifier.NOTIF_ID, notif)
            }
        } catch (_: Throwable) {
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            // Start sampling (idempotent)
            BatteryGraph.repo.startSampling()

            // Auto-detect drain mode strategy
            // If Shizuku is available and permitted, use Enhanced Collector.
            // Otherwise, fall back to Heuristic Drain Tracker.
            if (shizukuBridge.ping() && shizukuBridge.hasPermission()) {
                drainTracker.stop()
                if (!enhancedCollector.isRunning()) enhancedCollector.start()
            } else {
                enhancedCollector.stop()
                if (!drainTracker.isRunning()) drainTracker.start()
            }

            // Keep notification and widgets in sync
            BatteryGraph.repo.realtimeFlow.collect { rt ->
                val text = if (rt.sample == null)
                    "Waiting for battery data…" else
                    "Level ${rt.level}% • ${rt.currentMa} mA • ${rt.voltageMv} mV"

                val running = Notifier.monitoringNotification(this@BatteryMonitorService, text)
                try {
                    val nm = getSystemService(android.app.NotificationManager::class.java)
                    nm.notify(Notifier.NOTIF_ID, running)
                } catch (_: Throwable) { }

                rt.sample?.let { WidgetUpdater.push(this@BatteryMonitorService, it) }
            }
        }
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        if (Build.VERSION.SDK_INT >= 35 &&
            (fgsType and ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) != 0
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        BatteryGraph.repo.stopSampling()
        drainTracker.stop()
        enhancedCollector.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}