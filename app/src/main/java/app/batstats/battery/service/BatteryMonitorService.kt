package app.batstats.battery.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.batstats.battery.BatteryGraph
import app.batstats.battery.drain.AdvancedDrainTracker
import app.batstats.battery.drain.DrainNotificationManager
import android.app.Notification
import android.app.NotificationManager
import android.util.Log
import app.batstats.battery.shizuku.BstatsCollector
import app.batstats.battery.util.Notifier
import app.batstats.battery.util.ShellRunner
import app.batstats.battery.data.db.BatterySample
import app.batstats.battery.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class BatteryMonitorService : Service() {
    companion object {
        private const val TAG = "BatteryMonitorService"
        private const val PUBLISH_MIN_INTERVAL_MS = 30_000L
        private const val PUBLISH_CURRENT_DELTA_MA = 25
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val advancedDrainTracker: AdvancedDrainTracker by inject()
    private val drainNotificationManager: DrainNotificationManager by inject()
    private val shellRunner: ShellRunner by inject()
    private val enhancedCollector: BstatsCollector by inject()

    private var useAdvancedNotification = false

    private var lastPushLevel: Int? = null
    private var lastPushPlugged = 0
    private var lastPushMa = 0L
    private var lastPushAt = 0L

    private val started = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!goForeground(Notifier.NOTIF_ID, Notifier.monitoringNotification(this, "Starting…"))) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!started.compareAndSet(false, true)) return START_STICKY

        serviceScope.launch {
            val settings = BatteryGraph.settings.flow.first()
            useAdvancedNotification = settings.showDrainNotification

            val hasAdvanced = shellRunner.hasAnyPrivilegedAccess()

            // Start sampling
            BatteryGraph.repo.startSampling()
            BatteryGraph.repo.runAutoCleanup()

            // Auto-detect drain mode strategy
            if (hasAdvanced) {
                advancedDrainTracker.start()

                if (useAdvancedNotification) {
                    if (goForeground(
                            DrainNotificationManager.NOTIFICATION_ID,
                            drainNotificationManager.getNotification()
                        )
                    ) {
                        runCatching {
                            getSystemService(NotificationManager::class.java)
                                ?.cancel(Notifier.NOTIF_ID)
                        }
                    }
                    drainNotificationManager.startNotification()
                }

                serviceScope.launch {
                    BatteryGraph.settings.flow
                        .map { it.trackForegroundApps }
                        .distinctUntilChanged()
                        .collect { track ->
                            if (track) enhancedCollector.start() else enhancedCollector.stop()
                        }
                }
            } else {
                // No privileged access: advanced statistics are unavailable.
                enhancedCollector.stop()
                advancedDrainTracker.stop()
                drainNotificationManager.stopNotification()
            }

            // Update notification and widgets
            combine(BatteryGraph.repo.realtimeFlow, BatteryGraph.settings.flow) { rt, s ->
                rt to s.updateWidgets
            }.distinctUntilChanged().collect { (rt, updateWidgets) ->
                val sample = rt.sample
                val fresh = sample != null && shouldPublish(sample)

                if (updateWidgets && fresh && sample != null) {
                    WidgetUpdater.push(this@BatteryMonitorService, sample)
                }

                // Update standard notification if not using advanced
                if ((!useAdvancedNotification || !hasAdvanced) && fresh) {
                    val text = if (sample == null)
                        "Waiting for battery data…"
                    else
                        "Level ${rt.level ?: "--"}% • ${rt.currentMa} mA • ${rt.voltageMv} mV"

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

    private fun shouldPublish(s: BatterySample): Boolean {
        val now = System.currentTimeMillis()
        val fresh = s.levelPercent != lastPushLevel ||
            s.plugged != lastPushPlugged ||
            abs((s.currentNowUa ?: 0L) / 1000 - lastPushMa) >= PUBLISH_CURRENT_DELTA_MA ||
            now - lastPushAt >= PUBLISH_MIN_INTERVAL_MS
        if (fresh) {
            lastPushLevel = s.levelPercent
            lastPushPlugged = s.plugged
            lastPushMa = (s.currentNowUa ?: 0L) / 1000
            lastPushAt = now
        }
        return fresh
    }

    private fun goForeground(id: Int, notification: Notification): Boolean = try {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
        true
    } catch (t: Throwable) {
        Log.e(TAG, "startForeground failed", t)
        false
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        started.set(false)
        BatteryGraph.repo.stopSampling()
        advancedDrainTracker.stop()
        enhancedCollector.stop()
        drainNotificationManager.stopNotification()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
