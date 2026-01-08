package app.batstats.battery.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.batstats.battery.BatteryGraph
import app.batstats.battery.drain.AdvancedDrainTracker
import app.batstats.battery.drain.DrainNotificationManager
import app.batstats.battery.shizuku.BstatsCollector
import app.batstats.battery.shizuku.ShizukuBridge
import app.batstats.battery.util.Notifier
import app.batstats.battery.widget.WidgetUpdater
import app.batstats.insights.ForegroundDrainTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BatteryMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val drainTracker: ForegroundDrainTracker by inject()
    private val advancedDrainTracker: AdvancedDrainTracker by inject()
    private val drainNotificationManager: DrainNotificationManager by inject()
    private val shizukuBridge: ShizukuBridge by inject()
    private val enhancedCollector: BstatsCollector by inject()

    private var useAdvancedNotification = false

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val settings = BatteryGraph.settings.flow.first()
            useAdvancedNotification = settings.showDrainNotification

            val hasShizuku = shizukuBridge.ping() && shizukuBridge.hasPermission()

            val notif = if (useAdvancedNotification && hasShizuku) {
                drainNotificationManager.getNotification()
            } else {
                Notifier.monitoringNotification(this@BatteryMonitorService, "Starting…")
            }

            val notifId = if (useAdvancedNotification && hasShizuku) {
                DrainNotificationManager.NOTIFICATION_ID
            } else {
                Notifier.NOTIF_ID
            }

            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(
                        notifId,
                        notif,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(notifId, notif)
                }
            } catch (_: Throwable) {
                stopSelf()
                return@launch
            }

            // Start sampling
            BatteryGraph.repo.startSampling()

            // Auto-detect drain mode strategy
            if (hasShizuku) {
                drainTracker.stop()
                advancedDrainTracker.start()

                if (useAdvancedNotification) {
                    drainNotificationManager.startNotification()
                }

                if (!enhancedCollector.isRunning()) {
                    enhancedCollector.start()
                }
            } else {
                // Fall back to heuristic drain tracker
                enhancedCollector.stop()
                advancedDrainTracker.stop()
                drainNotificationManager.stopNotification()
                if (!drainTracker.isRunning()) {
                    drainTracker.start()
                }
            }

            // Update notification and widgets
            BatteryGraph.repo.realtimeFlow.collect { rt ->
                // Always update widgets
                rt.sample?.let { WidgetUpdater.push(this@BatteryMonitorService, it) }

                // Update standard notification if not using advanced
                if (!useAdvancedNotification || !hasShizuku) {
                    val text = if (rt.sample == null)
                        "Waiting for battery data…"
                    else
                        "Level ${rt.level}% • ${rt.currentMa} mA • ${rt.voltageMv} mV"

                    val running = Notifier.monitoringNotification(this@BatteryMonitorService, text)
                    try {
                        val nm = getSystemService(android.app.NotificationManager::class.java)
                        nm.notify(Notifier.NOTIF_ID, running)
                    } catch (_: Throwable) { }
                }
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
        advancedDrainTracker.stop()
        enhancedCollector.stop()
        drainNotificationManager.stopNotification()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}