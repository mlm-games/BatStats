package app.batstats.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.batstats.viewmodel.SettingsViewModel
import io.github.mlmgames.settings.ui.components.SettingsItem
import io.github.mlmgames.settings.ui.components.SettingsToggle
import io.github.mlmgames.settings.ui.dialogs.SliderSettingDialog
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinViewModel()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showLimit by rememberSaveable { mutableStateOf(false) }
    var showTemp by rememberSaveable { mutableStateOf(false) }
    var showDisch by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Alarms") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { pv ->
        Column(
            Modifier
                .padding(pv)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Charging Section
            SectionCard(title = "Charging") {
                SettingsToggle(
                    title = "High Battery Alert",
                    checked = settings.highBatteryAlertEnabled,
                    onCheckedChange = { vm.updateSetting("highBatteryAlertEnabled", it) },
                    description = "Notify when charging reaches ${settings.highBatteryThreshold}%"
                )
                SettingsItem(
                    title = "Threshold",
                    subtitle = "${settings.highBatteryThreshold}%",
                    onClick = { showLimit = true },
                    enabled = settings.highBatteryAlertEnabled
                )
                val pct = settings.highBatteryThreshold.coerceIn(0, 100) / 100f
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            // Thermals Section
            SectionCard(title = "Thermals") {
                SettingsToggle(
                    title = "High Temperature Alert",
                    checked = settings.temperatureWarningEnabled,
                    onCheckedChange = { vm.updateSetting("temperatureWarningEnabled", it) },
                    description = "Notify when battery exceeds ${settings.temperatureThreshold}°C"
                )
                SettingsItem(
                    title = "Threshold",
                    subtitle = "${settings.temperatureThreshold}°C",
                    onClick = { showTemp = true },
                    enabled = settings.temperatureWarningEnabled
                )
                val tempProg = ((settings.temperatureThreshold - 20).coerceIn(0f, 40f)) / 40f
                LinearProgressIndicator(
                    progress = { tempProg },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            // Discharge Section
            SectionCard(title = "Discharge") {
                SettingsToggle(
                    title = "High Discharge Alert",
                    checked = settings.dischargeAlertEnabled,
                    onCheckedChange = { vm.updateSetting("dischargeAlertEnabled", it) },
                    description = "Notify when draining > ${settings.dischargeCurrentThreshold}mA"
                )
                SettingsItem(
                    title = "Threshold",
                    subtitle = "${settings.dischargeCurrentThreshold} mA",
                    onClick = { showDisch = true },
                    enabled = settings.dischargeAlertEnabled
                )
                val dischProg = (settings.dischargeCurrentThreshold.toFloat() / 2000f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { dischProg },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            // Add spacer at bottom to ensure last card isn't cut off by gesture nav
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLimit) {
        SliderSettingDialog(
            title = "High Battery (%)",
            currentValue = settings.highBatteryThreshold.toFloat(),
            min = 50f, max = 100f, step = 1f,
            onDismiss = { showLimit = false },
            onValueSelected = { v ->
                vm.updateSetting("highBatteryThreshold", v.toInt())
                showLimit = false
            }
        )
    }

    if (showTemp) {
        SliderSettingDialog(
            title = "High Temperature (°C)",
            currentValue = settings.temperatureThreshold,
            min = 35f, max = 55f, step = 1f,
            onDismiss = { showTemp = false },
            onValueSelected = { v ->
                vm.updateSetting("temperatureThreshold", v)
                showTemp = false
            }
        )
    }

    if (showDisch) {
        SliderSettingDialog(
            title = "High Discharge (mA)",
            currentValue = settings.dischargeCurrentThreshold.toFloat(),
            min = 200f, max = 2000f, step = 50f,
            onDismiss = { showDisch = false },
            onValueSelected = { v ->
                vm.updateSetting("dischargeCurrentThreshold", v.toInt())
                showDisch = false
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}