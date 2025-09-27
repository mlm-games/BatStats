package app.batstats.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.BatterySettings
import app.batstats.ui.components.SettingsItem
import app.batstats.ui.components.SettingsToggle
import app.batstats.ui.dialogs.SliderSettingDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(onBack: () -> Unit) {
    val settingsFlow = BatteryGraph.settings.flow
    val settings by settingsFlow.collectAsState(initial = BatterySettings())
    val scope = rememberCoroutineScope()

    var showLimit by remember { mutableStateOf(false) }
    var showTemp by remember { mutableStateOf(false) }
    var showDisch by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        LargeTopAppBar(
            title = { Text("Alarms") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
        )
    }) { pv ->
        Column(Modifier.padding(pv).fillMaxSize()) {
            SectionCard(title = "Charging") {
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
                LinearProgressIndicator(
                    progress = { settings.chargeLimitPercent / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            SectionCard(title = "Thermals") {
                SettingsItem(
                    title = "High temperature alert",
                    subtitle = "${settings.tempHighC} °C",
                    onClick = { showTemp = true }
                )
                LinearProgressIndicator(
                    progress = { (settings.tempHighC - 20).coerceIn(0, 40) / 40f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            SectionCard(title = "Discharge") {
                SettingsItem(
                    title = "High discharge alert",
                    subtitle = "${settings.dischargeHighMa} mA",
                    onClick = { showDisch = true }
                )
                LinearProgressIndicator(
                    progress = { (settings.dischargeHighMa / 2000f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), content = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            content()
        })
    }
}