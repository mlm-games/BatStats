package app.batstats.battery.util

import android.app.*
import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import app.batstats.battery.BatteryMainActivity
//import app.batstats.battery.R // If you don't have icons, switch to system icon

object Notifier {
    private const val CH_ID = "battery_monitor"
    const val NOTIF_ID = 11

    fun ensureChannel(ctx: Context) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CH_ID) == null) {
            val ch = NotificationChannel(CH_ID, "Battery Monitor", NotificationManager.IMPORTANCE_LOW).apply {
                enableLights(false); enableVibration(false)
                lightColor = Color.GREEN
            }
            mgr.createNotificationChannel(ch)
        }
    }

    fun monitoringNotification(ctx: Context, text: String): android.app.Notification {
        ensureChannel(ctx)
        val pi = PendingIntent.getActivity(
            ctx, 0, android.content.Intent(ctx, BatteryMainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(ctx, CH_ID)
            .setContentTitle("Monitoring battery")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    fun notifyChargeLimit(ctx: Context, limit: Int) {
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CH_ID)
            .setContentTitle("Charge limit reached")
            .setContentText("Battery at $limit% — consider unplugging.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
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
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(1003, n)
    }
}