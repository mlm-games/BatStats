package app.batstats.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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
        val s = BatteryGraph.settings
        GlobalScope.launch {
            val cfg = s.flow.first()
            if (!cfg.monitorOnBoot) return@launch
            if (Build.VERSION.SDK_INT >= 35) {
                Notifier.promptStartOnBoot(context)
            } else {
                context.startForegroundService(Intent(context, BatteryMonitorService::class.java))
            }
        }
    }
}
