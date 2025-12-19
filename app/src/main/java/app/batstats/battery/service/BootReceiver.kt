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
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Notifier.ensureChannel(context)
        val repository = BatteryGraph.settings

        GlobalScope.launch {
            val config = repository.flow.first()
            if (!config.autoStartOnBoot) return@launch

            if (Build.VERSION.SDK_INT >= 35) {
                Notifier.promptStartOnBoot(context)
            } else {
                context.startForegroundService(Intent(context, BatteryMonitorService::class.java))
            }
        }
    }
}