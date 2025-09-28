package app.batstats.battery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.batstats.battery.BatteryGraph
import app.batstats.battery.util.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val pending = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Notifier.ensureChannel(appCtx)
                if (BatteryGraph.settings.flow.first().monitorOnBoot) {
                    appCtx.startForegroundService(Intent(appCtx, BatteryMonitorService::class.java))
                }
            } finally {
                pending.finish()
            }
        }
    }
}