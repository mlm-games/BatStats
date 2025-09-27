package app.batstats.battery.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.batstats.ui.components.*
import app.batstats.battery.BatteryGraph
import kotlinx.coroutines.launch

@Composable
fun BatterySettingsScreen(onBack: () -> Unit) {
    val settings by BatteryGraph.settings.flow.collectAsState(initial = app.batstats.battery.data.BatterySettings())
    val scope = rememberCoroutineScope()

    MyScreenScaffold(title = "Settings", actions = { TextButton(onClick = onBack) { Text("Back") } }) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                SettingsSection("Monitoring") {
                    SettingsToggle(
                        title = "Start on boot",
                        isChecked = settings.monitorOnBoot,
                        onCheckedChange = { ch -> scope.launch { BatteryGraph.settings.update { it.copy(monitorOnBoot = ch) } } },
                        description = "Automatically start monitoring after reboot"
                    )
                    SettingsToggle(
                        title = "Monitor while charging only",
                        isChecked = settings.monitorWhileChargingOnly,
                        onCheckedChange = { ch -> scope.launch { BatteryGraph.settings.update { it.copy(monitorWhileChargingOnly = ch) } } }
                    )
                    SettingsItem(
                        title = "Sample interval",
                        subtitle = "${settings.sampleIntervalSec} s",
                        description = "5..60 seconds",
                        onClick = {
                            scope.launch {
                                BatteryGraph.settings.update {
                                    val step = if (it.sampleIntervalSec >= 60) 5 else 5
                                    it.copy(sampleIntervalSec = (it.sampleIntervalSec + step).coerceAtMost(60).coerceAtLeast(5))
                                }
                            }
                        }
                    )
                    SettingsItem(
                        title = "Keep history",
                        subtitle = "${settings.keepHistoryDays} days",
                        onClick = {
                            scope.launch {
                                BatteryGraph.settings.update {
                                    val next = listOf(15, 30, 45, 60, 90).firstOrNull { v -> v > it.keepHistoryDays } ?: 15
                                    it.copy(keepHistoryDays = next)
                                }
                            }
                        }
                    )
                }
                SettingsSection("Sessions") {
                    SettingsToggle(
                        title = "Auto-start on plug",
                        isChecked = settings.autoStartSessionOnPlug,
                        onCheckedChange = { ch -> scope.launch { BatteryGraph.settings.update { it.copy(autoStartSessionOnPlug = ch) } } }
                    )
                    SettingsToggle(
                        title = "Auto-stop on unplug",
                        isChecked = settings.autoStopSessionOnUnplug,
                        onCheckedChange = { ch -> scope.launch { BatteryGraph.settings.update { it.copy(autoStopSessionOnUnplug = ch) } } }
                    )
                }
            }
        }
    }
}