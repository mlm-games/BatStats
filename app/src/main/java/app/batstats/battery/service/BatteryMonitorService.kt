package app.batstats.battery.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.batstats.battery.BatteryGraph
import app.batstats.battery.util.Notifier
import app.batstats.battery.widget.WidgetUpdater
import kotlinx.coroutines.*

class BatteryMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC // match manifest
                )
            } else {
                startForeground(Notifier.NOTIF_ID, notif)
            }
        } catch (_: Throwable) {
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            BatteryGraph.repo.startSampling()
            BatteryGraph.repo.realtime.collect { rt ->
                val text = if (rt.sample == null)
                    "Waiting for battery data…" else
                    "Level ${rt.level}% • ${rt.currentMa} mA • ${rt.voltageMv} mV"
                val running = Notifier.monitoringNotification(this@BatteryMonitorService, text)
                try {
                    if (Build.VERSION.SDK_INT >= 34) {
                        startForeground(
                            Notifier.NOTIF_ID,
                            running,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(Notifier.NOTIF_ID, running)
                    }
                } catch (_: Throwable) { /* keep previous notif */ }
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
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}