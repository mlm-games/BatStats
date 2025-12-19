package app.batstats.battery.widget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import app.batstats.battery.BatteryGraph

class BatteryTimeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        BatteryGraph.repo.realtimeFlow.value.sample?.let { WidgetUpdater.push(context, it) }
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetUpdater.ACTION_REFRESH) {
            BatteryGraph.repo.realtimeFlow.value.sample?.let { WidgetUpdater.push(context, it) }
        }
    }
}
