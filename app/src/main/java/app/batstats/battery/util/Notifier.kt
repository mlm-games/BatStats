package app.batstats.battery.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import app.batstats.battery.BatteryMainActivity
import app.batstats.battery.service.BatteryMonitorService

object Notifier {
    private const val CH_ID = "battery_monitor"
    const val NOTIF_ID = 11

    fun ensureChannel(ctx: Context) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CH_ID) == null) {
            val ch = NotificationChannel(
                CH_ID,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                enableLights(false)
                enableVibration(false)
                lightColor = Color.GREEN
                setShowBadge(false)
            }
            mgr.createNotificationChannel(ch)
        }
    }

    fun promptStartOnBoot(ctx: Context) {
        ensureChannel(ctx)
        val startIntent = Intent(ctx, BatteryMonitorService::class.java)
        val pi = if (Build.VERSION.SDK_INT >= 26) {
            PendingIntent.getForegroundService(
                ctx, 1, startIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                ctx, 1, startIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val n = NotificationCompat.Builder(ctx, CH_ID)
            .setContentTitle("Monitoring ready")
            .setContentText("Tap to start battery monitoring")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Start monitoring", pi)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1000, n)
    }

    fun monitoringNotification(ctx: Context, text: String): Notification {
        ensureChannel(ctx)
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, BatteryMainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(ctx, CH_ID)
            .setContentTitle("Monitoring battery")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun notifyChargeLimit(ctx: Context, limit: Int) {
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CH_ID)
            .setContentTitle("Charge limit reached")
            .setContentText("Battery at $limit% — consider unplugging.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1001, n)
    }

    fun notifyTempHigh(ctx: Context, tempC: Int) {
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CH_ID)
            .setContentTitle("High temperature")
            .setContentText("$tempC °C — cool down the device.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOnlyAlertOnce(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1002, n)
    }

    fun notifyDischargeHigh(ctx: Context, ma: Int) {
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CH_ID)
            .setContentTitle("High discharge")
            .setContentText("$ma mA — heavy drain detected.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOnlyAlertOnce(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1003, n)
    }
}