package app.batstats.battery.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.batstats.battery.BatteryGraph
import app.batstats.battery.service.BatteryMonitorService
import app.batstats.battery.viewmodel.DashboardViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.batstats.battery.data.db.BatterySample
import app.batstats.ui.components.MyScreenScaffold

@Composable
fun DashboardScreen(
    onOpenHistory: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory())
) {
    val rt by vm.realtime.collectAsState()
    val session = vm.activeSession.value

    MyScreenScaffold(
        title = "Battery",
        actions = {
            TextButton(onClick = onOpenHistory) { Text("History") }
            TextButton(onClick = onOpenAlarms) { Text("Alarms") }
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Big meter
            ElevatedCard {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Level: ${rt.level}%")
                    Text("Current: ${rt.currentMa} mA")
                    Text("Voltage: ${rt.voltageMv} mV")
                    Text("Power: ${"%.1f".format(rt.powerMw)} mW")
                    Text("Temp: ${"%.1f".format(rt.temperatureC)} °C")
                }
            }

            // Session controls
            ElevatedCard {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (session == null) "No active session" else "Session: ${session!!.type}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.startOrBindService() }) { Text("Start monitor") }
                        OutlinedButton(onClick = { vm.stopService() }) { Text("Stop") }
                        if (session == null) {
                            OutlinedButton(onClick = { vm.startManualSession() }) { Text("Start session") }
                        } else {
                            OutlinedButton(onClick = { vm.endSession() }) { Text("End session") }
                        }
                    }
                }
            }

            // Realtime mini chart (last 15 min)
            RealtimeStripChart()
        }
    }
}

@Composable
private fun RealtimeStripChart(vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory())) {
    val strip by vm.recentSamples.collectAsState(listOf())
    MiniLineChart(
        title = "Current (mA, 15 min)",
        values = strip.map { (it.currentNowUa ?: 0L).toFloat() / 1000f }
    )
}