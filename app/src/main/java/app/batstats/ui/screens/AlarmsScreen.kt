package app.batstats.battery.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.batstats.battery.data.BatterySettingsRepository
import app.batstats.battery.BatteryGraph
import app.batstats.ui.components.AppTopBar
import app.batstats.ui.components.SettingsSection
import app.batstats.ui.components.SettingsToggle
import app.batstats.ui.components.SettingsItem
import app.batstats.ui.dialogs.SliderSettingDialog
import kotlinx.coroutines.launch

@Composable
fun AlarmsScreen(onBack: () -> Unit) {
    val settingsFlow = BatteryGraph.settings.flow
    val settings by settingsFlow.collectAsState(initial = app.batstats.battery.data.BatterySettings())
    val scope = rememberCoroutineScope()

    var showLimit by remember { mutableStateOf(false) }
    var showTemp by remember { mutableStateOf(false) }
    var showDisch by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        AppTopBar(title = { Text("Alarms") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { pv ->
        Column(Modifier.padding(pv).fillMaxSize()) {
            SettingsSection("Charging") {
                SettingsToggle(
                    title = "Charge limit reminder",
                    isChecked = settings.chargeLimitPercent > 0,
                    onCheckedChange = { ch ->
                        scope.launch {
                            BatteryGraph.settings.update {
                                it.copy(chargeLimitPercent = if (ch) it.chargeLimitPercent.coerceAtLeast(60) else 0)
                            }
                        }
                    },
                    description = "Notify at ${settings.chargeLimitPercent}% to unplug"
                )
                SettingsItem(
                    title = "Limit percentage",
                    subtitle = "${settings.chargeLimitPercent}%",
                    onClick = { showLimit = true }
                )
            }
            SettingsSection("Thermals") {
                SettingsItem(
                    title = "High temperature alert",
                    subtitle = "${settings.tempHighC} °C",
                    onClick = { showTemp = true }
                )
            }
            SettingsSection("Discharge") {
                SettingsItem(
                    title = "High discharge alert",
                    subtitle = "${settings.dischargeHighMa} mA",
                    onClick = { showDisch = true }
                )
            }
        }
    }

    if (showLimit) SliderSettingDialog(
        title = "Charge limit (%)",
        currentValue = settings.chargeLimitPercent.toFloat(), min = 50f, max = 100f, step = 1f,
        onDismiss = { showLimit = false },
        onValueSelected = { v -> scope.launch { BatteryGraph.settings.update { it.copy(chargeLimitPercent = v.toInt()) } } }
    )

    if (showTemp) SliderSettingDialog(
        title = "High temperature (°C)",
        currentValue = settings.tempHighC.toFloat(), min = 35f, max = 55f, step = 1f,
        onDismiss = { showTemp = false },
        onValueSelected = { v -> scope.launch { BatteryGraph.settings.update { it.copy(tempHighC = v.toInt()) } } }
    )

    if (showDisch) SliderSettingDialog(
        title = "High discharge (mA)",
        currentValue = settings.dischargeHighMa.toFloat(), min = 200f, max = 2000f, step = 50f,
        onDismiss = { showDisch = false },
        onValueSelected = { v -> scope.launch { BatteryGraph.settings.update { it.copy(dischargeHighMa = v.toInt()) } } }
    )
}
