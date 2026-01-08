package app.batstats.battery.drain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.batstats.battery.BatteryMainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

/**
 * Manages the persistent drain statistics notification.
 */
class DrainNotificationManager(
    private val context: Context,
    private val drainTracker: AdvancedDrainTracker,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    companion object {
        const val CHANNEL_ID = "drain_stats_channel"
        const val NOTIFICATION_ID = 2001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var updateJob: Job? = null
    private var isShowing = false

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Drain Statistics",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time battery drain statistics"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startNotification() {
        if (isShowing) return
        isShowing = true

        updateJob = scope.launch {
            drainTracker.drainState.collectLatest { state ->
                if (isShowing) {
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
                }
            }
        }
    }

    fun stopNotification() {
        isShowing = false
        updateJob?.cancel()
        updateJob = null
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun getNotification(): Notification {
        return buildNotification(drainTracker.drainState.value)
    }

    private fun buildNotification(state: DrainState): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, BatteryMainActivity::class.java).apply {
                putExtra("open_drain_stats", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, DrainNotificationReceiver::class.java).apply {
                action = DrainNotificationReceiver.ACTION_RESET
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentStateText = when {
            state.isCharging -> "⚡ Charging"
            state.isDeepSleep -> "😴 Deep Sleep"
            state.isDozing -> "💤 Dozing"
            state.isScreenOn -> "📱 Screen On"
            else -> "🌙 Screen Off"
        }

        val title = "${state.batteryLevel}% • $currentStateText"
        
        val contentText = buildString {
            append("On: ${formatDrainRate(state.screenOnDrainRate)}")
            append(" • Off: ${formatDrainRate(state.screenOffDrainRate)}")
            append(" • Sleep: ${formatDrainRate(state.deepSleepDrainRate)}")
        }

        val bigText = buildString {
            appendLine("━━━ Drain Rates ━━━")
            appendLine("📱 Screen On: ${formatDrainRate(state.screenOnDrainRate)} (${formatDuration(state.screenOnTimeMs)})")
            appendLine("🌙 Screen Off: ${formatDrainRate(state.screenOffDrainRate)} (${formatDuration(state.screenOffTimeMs)})")
            appendLine("😴 Deep Sleep: ${formatDrainRate(state.deepSleepDrainRate)} (${formatDuration(state.deepSleepTimeMs)}) [${String.format(Locale.getDefault(), "%.0f%%", state.deepSleepPercentage)}]")
            appendLine("⚡ Awake: ${formatDrainRate(state.awakeDrainRate)} (${formatDuration(state.awakeTimeMs)})")
            appendLine()
            appendLine("━━━ Activity ━━━")
            appendLine("🔥 Active: ${formatDrainRate(state.activeDrainRate)} (${formatDuration(state.activeTimeMs)})")
            appendLine("💤 Idle: ${formatDrainRate(state.idleDrainRate)} (${formatDuration(state.idleTimeMs)})")
            appendLine()
            appendLine("━━━ Session ━━━")
            appendLine("Total: ${String.format(Locale.getDefault(), "%.1f mAh", state.totalDrainMah)} in ${formatDuration(state.totalTimeMs)}")
            append("Average: ${formatDrainRate(state.averageDrainRate)}")
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_rotate,
                "Reset",
                resetIntent
            )
            .build()
    }
}