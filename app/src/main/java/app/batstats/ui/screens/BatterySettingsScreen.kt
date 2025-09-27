package app.batstats.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.BatterySettings
import app.batstats.ui.components.SettingsItem
import app.batstats.ui.components.SettingsToggle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatterySettingsScreen(onBack: () -> Unit) {
    val settings by BatteryGraph.settings.flow.collectAsState(initial = BatterySettings())
    val scope = rememberCoroutineScope()

    Scaffold(topBar = {
        LargeTopAppBar(
            title = { Text("Settings") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
    }) { pv ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .padding(pv)
                .fillMaxSize()
        ) {
            item {
                SettingsCategory("Monitoring")
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
                    description = "Tap to cycle 5 → 10 → 15 … 60s",
                    onClick = {
                        scope.launch {
                            BatteryGraph.settings.update {
                                val steps = listOf(5, 10, 15, 20, 30, 45, 60)
                                val idx = steps.indexOf(it.sampleIntervalSec).takeIf { i -> i >= 0 } ?: 0
                                val next = steps[(idx + 1) % steps.size]
                                it.copy(sampleIntervalSec = next)
                            }
                        }
                    }
                )
                SettingsItem(
                    title = "Keep history",
                    subtitle = "${settings.keepHistoryDays} days",
                    description = "Tap to cycle 15/30/45/60/90",
                    onClick = {
                        scope.launch {
                            BatteryGraph.settings.update {
                                val cycle = listOf(15, 30, 45, 60, 90)
                                val idx = cycle.indexOf(it.keepHistoryDays).takeIf { i -> i >= 0 } ?: 0
                                val next = cycle[(idx + 1) % cycle.size]
                                it.copy(keepHistoryDays = next)
                            }
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))
                SettingsCategory("Sessions")
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

                Spacer(Modifier.height(24.dp))
                Text(
                    "Tip: lower sample interval while charging for finer estimates; increase when idle to save power.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SettingsCategory(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}