package app.batstats.battery.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.batstats.R
import app.batstats.battery.BatteryMainActivity
import app.batstats.battery.data.db.BatterySample
import app.batstats.battery.util.TimeEstimator

object WidgetUpdater {
    const val ACTION_REFRESH = "app.batstats.battery.widget.ACTION_REFRESH"

    fun push(ctx: Context, s: BatterySample) {
        updateLevel(ctx, s)
        updateTemp(ctx, s)
        updateTime(ctx, s)
    }

    fun showPlaceholder(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)

        // Level widget placeholder
        val levelIds = mgr.getAppWidgetIds(ComponentName(ctx, BatteryLevelWidget::class.java))
        val levelRv = createRemoteViews(ctx).apply {
            setTextViewText(R.id.title, "Battery")
            setTextViewText(R.id.value, "—")
        }
        levelIds.forEach { mgr.updateAppWidget(it, levelRv) }

        // Temp widget placeholder
        val tempIds = mgr.getAppWidgetIds(ComponentName(ctx, BatteryTempWidget::class.java))
        val tempRv = createRemoteViews(ctx).apply {
            setTextViewText(R.id.title, "Temperature")
            setTextViewText(R.id.value, "—")
        }
        tempIds.forEach { mgr.updateAppWidget(it, tempRv) }

        // Time widget placeholder
        val timeIds = mgr.getAppWidgetIds(ComponentName(ctx, BatteryTimeWidget::class.java))
        val timeRv = createRemoteViews(ctx).apply {
            setTextViewText(R.id.title, "ETA")
            setTextViewText(R.id.value, "—")
        }
        timeIds.forEach { mgr.updateAppWidget(it, timeRv) }
    }

    private fun createRemoteViews(ctx: Context): RemoteViews {
        // Since we're using a common layout, create base RemoteViews
        val rv = RemoteViews(ctx.packageName, R.layout.widget_common)

        // Add click intent to open app
        val intent = Intent(ctx, BatteryMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        rv.setOnClickPendingIntent(R.id.root, pendingIntent)

        return rv
    }

    private fun updateLevel(ctx: Context, s: BatterySample) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, BatteryLevelWidget::class.java))
        val rv = createRemoteViews(ctx).apply {
            setTextViewText(R.id.title, "Battery")
            setTextViewText(R.id.value, "${s.levelPercent}%")
        }
        ids.forEach { mgr.updateAppWidget(it, rv) }
    }

    private fun updateTemp(ctx: Context, s: BatterySample) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, BatteryTempWidget::class.java))
        val tempC = (s.temperatureDeciC ?: 0) / 10.0
        val rv = createRemoteViews(ctx).apply {
            setTextViewText(R.id.title, "Temperature")
            setTextViewText(R.id.value, String.format("%.1f °C", tempC))
        }
        ids.forEach { mgr.updateAppWidget(it, rv) }
    }

    private fun updateTime(ctx: Context, s: BatterySample) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, BatteryTimeWidget::class.java))
        val eta = TimeEstimator.etaString(s) ?: "—"
        val rv = createRemoteViews(ctx).apply {
            setTextViewText(R.id.title, "ETA")
            setTextViewText(R.id.value, eta)
        }
        ids.forEach { mgr.updateAppWidget(it, rv) }
    }

    fun requestRefresh(ctx: Context) {
        // Fix: use actual package name
        ctx.sendBroadcast(Intent(ACTION_REFRESH).setPackage(ctx.packageName))
    }
}