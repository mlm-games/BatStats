package app.batstats.battery.service

import android.app.Service
import android.content.Intent
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
        startForeground(Notifier.NOTIF_ID, Notifier.monitoringNotification(this, "Starting…"))
        serviceScope.launch {
            BatteryGraph.repo.startSampling()
            BatteryGraph.repo.realtime.collect { rt ->
                val text = if (rt.sample == null) "Waiting for battery data…" else
                    "Level ${rt.level}% • ${rt.currentMa} mA • ${rt.voltageMv} mV"
                val notif = Notifier.monitoringNotification(this@BatteryMonitorService, text)
                startForeground(Notifier.NOTIF_ID, notif)
                rt.sample?.let { WidgetUpdater.push(this@BatteryMonitorService, it) }
            }
        }
    }

    override fun onDestroy() {
        BatteryGraph.repo.stopSampling()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
