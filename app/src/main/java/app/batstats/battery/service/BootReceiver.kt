package app.batstats.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.batstats.battery.BatteryGraph
import app.batstats.battery.util.Notifier
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        Notifier.ensureChannel(context)
        // Auto start monitoring if enabled
        val s = BatteryGraph.settings
        // Launch in a short async to read settings
        GlobalScope.launch {
            if (s.flow.first().monitorOnBoot) {
                context.startForegroundService(Intent(context, BatteryMonitorService::class.java))
            }
        }
    }
}