package app.batstats.battery.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import app.batstats.R
import app.batstats.battery.BatteryMainActivity
import app.batstats.battery.service.BatteryMonitorService

object Notifier {
    private const val CH_ID = "battery_monitor"
    private const val CH_ALERT = "battery_alert"
    const val NOTIF_ID = 11

    fun ensureChannel(ctx: Context) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CH_ID) == null) {
            val ch = NotificationChannel(
                CH_ID,
                ctx.getString(R.string.monitoring),
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
            .setContentTitle(ctx.getString(R.string.monitoring_ready))
            .setContentText(ctx.getString(R.string.tap_to_start))
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, ctx.getString(R.string.start_monitoring), pi)
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
            .setContentTitle(ctx.getString(R.string.monitoring_battery))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun notifyLowBattery(ctx: Context, level: Int, sound: Boolean, vibrate: Boolean) =
        alert(
            ctx, 1004,
            ctx.getString(R.string.low_battery_alert),
            ctx.getString(R.string.low_battery_text, level),
            sound, vibrate
        )

    fun notifyChargeLimit(ctx: Context, limit: Int, sound: Boolean, vibrate: Boolean) =
        alert(
            ctx, 1001,
            ctx.getString(R.string.charge_limit_reached),
            ctx.getString(R.string.battery_at_percent, limit),
            sound, vibrate
        )

    fun notifyChargingComplete(ctx: Context, level: Int, sound: Boolean, vibrate: Boolean) =
        alert(
            ctx, 1005,
            ctx.getString(R.string.charging_complete),
            ctx.getString(R.string.battery_at_percent, level),
            sound, vibrate
        )

    fun notifyTempHigh(ctx: Context, tempC: Int, sound: Boolean, vibrate: Boolean) =
        alert(
            ctx, 1002,
            ctx.getString(R.string.high_temperature),
            ctx.getString(R.string.temperature_high, tempC),
            sound, vibrate
        )

    fun notifyDischargeHigh(ctx: Context, ma: Int, sound: Boolean, vibrate: Boolean) =
        alert(
            ctx, 1003,
            ctx.getString(R.string.high_discharge),
            ctx.getString(R.string.heavy_drain, ma),
            sound, vibrate
        )

    private fun alert(ctx: Context, id: Int, title: String, text: String, sound: Boolean, vibrate: Boolean) {
        val n = NotificationCompat.Builder(ctx, alertChannel(ctx, sound, vibrate))
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(id, n)
    }

    private fun alertChannel(ctx: Context, sound: Boolean, vibrate: Boolean): String {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val uri = if (sound) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null
        val existing = mgr.getNotificationChannel(CH_ALERT)
        if (existing == null || existing.sound != uri || existing.shouldVibrate() != vibrate) {
            mgr.deleteNotificationChannel(CH_ALERT)
            mgr.createNotificationChannel(
                NotificationChannel(
                    CH_ALERT,
                    ctx.getString(R.string.battery_alerts),
                    if (sound || vibrate) NotificationManager.IMPORTANCE_DEFAULT
                    else NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(
                        uri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    enableVibration(vibrate)
                    if (vibrate) vibrationPattern = longArrayOf(0, 250, 250, 250)
                    setShowBadge(false)
                }
            )
        }
        return CH_ALERT
    }
}