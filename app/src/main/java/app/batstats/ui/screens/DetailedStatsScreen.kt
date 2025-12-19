package app.batstats.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.batstats.battery.util.BatteryStatsParser
import app.batstats.battery.util.RootStatsCollector
import app.batstats.viewmodel.DetailedStatsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailedStatsScreen(
    onBack: () -> Unit,
    vm: DetailedStatsViewModel = koinViewModel()
) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val deviceIdle by vm.deviceIdle.collectAsStateWithLifecycle()
    val powerManager by vm.powerManager.collectAsStateWithLifecycle()
    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    val lastRefresh by vm.lastRefresh.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val hasShizuku by vm.hasShizuku.collectAsStateWithLifecycle()
    val hasRoot by vm.hasRoot.collectAsStateWithLifecycle()
    val kernelBattery by vm.kernelBattery.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHost = remember { SnackbarHostState() }

    val tabs = remember {
        listOf(
            StatsTab("Overview", Icons.Outlined.Dashboard),
            StatsTab("Apps", Icons.Outlined.Apps),
            StatsTab("Wakelocks", Icons.Outlined.Alarm),
            StatsTab("Network", Icons.Outlined.Wifi),
            StatsTab("Alarms", Icons.Outlined.Schedule),
            StatsTab("System", Icons.Outlined.SettingsApplications),
            StatsTab("Root", Icons.Outlined.AdminPanelSettings)
        )
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    LaunchedEffect(Unit) {
        vm.refresh()
    }

    LaunchedEffect(error) {
        error?.let { snackbarHost.showSnackbar(it) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Detailed Stats")
                        AnimatedVisibility(visible = lastRefresh > 0) {
                            val timeFormatter = remember(Locale.getDefault()) {
                                DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
                            }
                            val formatted = remember(lastRefresh) {
                                Instant.ofEpochMilli(lastRefresh)
                                    .atZone(ZoneId.systemDefault())
                                    .format(timeFormatter)
                            }
                            Text(
                                "Updated $formatted",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { scope.launch { vm.refresh() } }) {
                            Icon(Icons.Outlined.Refresh, "Refresh")
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            if (vm.resetStats()) {
                                snackbarHost.showSnackbar("Stats reset successfully")
                                vm.refresh()
                            } else {
                                snackbarHost.showSnackbar("Failed to reset stats")
                            }
                        }
                    }) {
                        Icon(Icons.Outlined.RestartAlt, "Reset stats")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!hasShizuku) {
                ShizukuRequiredCard(
                    onRequestPermission = { vm.requestShizukuPermission() }
                )
            } else {
                // Tab row
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(tab.title) },
                            icon = { Icon(tab.icon, null, Modifier.size(18.dp)) }
                        )
                    }
                }

                HorizontalDivider()

                // Pager content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> OverviewTab(snapshot, deviceIdle, powerManager)
                        1 -> AppsTab(snapshot?.apps ?: emptyList())
                        2 -> WakelocksTab(snapshot?.wakelocks ?: emptyList(), snapshot?.kernelWakelocks ?: emptyList())
                        3 -> NetworkTab(snapshot?.network ?: emptyList())
                        4 -> AlarmsJobsTab(snapshot?.alarms ?: emptyList(), snapshot?.jobs ?: emptyList(), snapshot?.syncs ?: emptyList())
                        5 -> SystemTab(snapshot, deviceIdle, powerManager)
                        6 -> RootTab(hasRoot, kernelBattery, onRefresh = { vm.refreshRootStats() })
                    }
                }
            }

            AnimatedVisibility(visible = error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Error,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

private data class StatsTab(val title: String, val icon: ImageVector)

@Composable
private fun ShizukuRequiredCard(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Outlined.Security,
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Shizuku Required",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Detailed battery stats require Shizuku to be running and permission granted. " +
                            "Shizuku provides ADB-level access without root.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(
    snapshot: BatteryStatsParser.FullSnapshot?,
    deviceIdle: BatteryStatsParser.DeviceIdleInfo?,
    powerManager: BatteryStatsParser.PowerManagerInfo?
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SummaryCard(snapshot) }
        item { DischargeBreakdownCard(snapshot) }
        item { ScreenTimeCard(snapshot) }
        item { SignalQualityCard(snapshot) }
        item { DozeStatsCard(snapshot?.doze) }
        item { BluetoothCard(snapshot?.bluetooth) }
        item { CurrentStateCard(deviceIdle, powerManager) }
    }
}

@Composable
private fun SummaryCard(snapshot: BatteryStatsParser.FullSnapshot?) {
    StatsCard(title = "Summary", icon = Icons.Outlined.Summarize) {
        if (snapshot == null) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val hours = snapshot.batteryRealtimeMs / 3600000.0
            val screenHours = snapshot.screenOnTimeMs / 3600000.0

            StatRow("Time on battery", String.format(Locale.getDefault(), "%.1f hours", hours))
            StatRow("Screen on time", String.format(Locale.getDefault(), "%.1f hours", screenHours))
            StatRow("Estimated capacity", "${snapshot.estimatedCapacityMah} mAh")
            StatRow("Apps tracked", "${snapshot.apps.size}")
            StatRow("Wakelocks", "${snapshot.wakelocks.size}")
            StatRow("Kernel wakelocks", "${snapshot.kernelWakelocks.size}")
        }
    }
}

@Composable
private fun DischargeBreakdownCard(snapshot: BatteryStatsParser.FullSnapshot?) {
    StatsCard(title = "Discharge Breakdown", icon = Icons.Outlined.BatteryAlert) {
        if (snapshot == null) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DischargeBox(
                    label = "Screen On",
                    percent = snapshot.screenOnDischargePercent,
                    color = MaterialTheme.colorScheme.primary
                )
                DischargeBox(
                    label = "Screen Off",
                    percent = snapshot.screenOffDischargePercent,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun DischargeBox(label: String, percent: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", percent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ScreenTimeCard(snapshot: BatteryStatsParser.FullSnapshot?) {
    StatsCard(title = "Screen Time Analysis", icon = Icons.Outlined.Smartphone) {
        if (snapshot == null) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val totalMs = snapshot.batteryRealtimeMs.toFloat().coerceAtLeast(1f)
            val screenOnPercent = (snapshot.screenOnTimeMs / totalMs * 100)
            val screenOffPercent = 100f - screenOnPercent

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Screen On", style = MaterialTheme.typography.labelMedium)
                    LinearProgressIndicator(
                        progress = { screenOnPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        String.format(Locale.getDefault(), "%.1f%% of battery time", screenOnPercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val drainPerHourScreenOn = if (snapshot.screenOnTimeMs > 0) {
                snapshot.screenOnDischargePercent / (snapshot.screenOnTimeMs / 3600000.0)
            } else 0.0
            val drainPerHourScreenOff = if (snapshot.batteryRealtimeMs - snapshot.screenOnTimeMs > 0) {
                snapshot.screenOffDischargePercent / ((snapshot.batteryRealtimeMs - snapshot.screenOnTimeMs) / 3600000.0)
            } else 0.0

            StatRow("Drain/hour (screen on)", String.format(Locale.getDefault(), "%.2f%%", drainPerHourScreenOn))
            StatRow("Drain/hour (screen off)", String.format(Locale.getDefault(), "%.2f%%", drainPerHourScreenOff))
        }
    }
}

@Composable
private fun SignalQualityCard(snapshot: BatteryStatsParser.FullSnapshot?) {
    StatsCard(title = "Signal Quality", icon = Icons.Outlined.SignalCellularAlt) {
        if (snapshot == null || snapshot.signalStrength.isEmpty()) {
            Text("No signal data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val signalLabels = listOf("None", "Poor", "Moderate", "Good", "Great")
            val colors = listOf(
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.primary
            )

            Text("Mobile Signal", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            snapshot.signalStrength.forEachIndexed { index, stat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        signalLabels.getOrNull(index) ?: "Level $index",
                        modifier = Modifier.width(80.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = { stat.percentOfTotal },
                        modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
                        color = colors.getOrNull(index) ?: MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        String.format(Locale.getDefault(), "%.0f%%", stat.percentOfTotal * 100),
                        modifier = Modifier.width(50.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            if (snapshot.wifiSignal.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("WiFi Signal", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                snapshot.wifiSignal.forEachIndexed { index, stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            signalLabels.getOrNull(index) ?: "Level $index",
                            modifier = Modifier.width(80.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearProgressIndicator(
                            progress = { stat.percentOfTotal },
                            modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)),
                            color = colors.getOrNull(index) ?: MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            String.format(Locale.getDefault(), "%.0f%%", stat.percentOfTotal * 100),
                            modifier = Modifier.width(50.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun DozeStatsCard(doze: BatteryStatsParser.DozeStats?) {
    StatsCard(title = "Doze Statistics", icon = Icons.Outlined.PowerSettingsNew) {
        if (doze == null) {
            Text("No Doze data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            StatRow("Deep Doze time", formatDuration(doze.deepIdleTimeMs))
            StatRow("Deep Doze count", "${doze.deepIdleCount}")
            StatRow("Light Doze time", formatDuration(doze.lightIdleTimeMs))
            StatRow("Light Doze count", "${doze.lightIdleCount}")
            StatRow("Maintenance windows", "${doze.maintenanceCount}")
            StatRow("Maintenance time", formatDuration(doze.maintenanceTimeMs))
        }
    }
}

@Composable
private fun BluetoothCard(bluetooth: BatteryStatsParser.BluetoothStats?) {
    StatsCard(title = "Bluetooth", icon = Icons.Outlined.Bluetooth) {
        if (bluetooth == null) {
            Text("No Bluetooth data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            StatRow("Idle time", formatDuration(bluetooth.idleTimeMs))
            StatRow("RX time", formatDuration(bluetooth.rxTimeMs))
            StatRow("TX time", formatDuration(bluetooth.txTimeMs))
            StatRow("Power usage", String.format(Locale.getDefault(), "%.2f mAh", bluetooth.powerMah))
            if (bluetooth.scanTimeMs > 0) {
                StatRow("Scan time", formatDuration(bluetooth.scanTimeMs))
            }
        }
    }
}

@Composable
private fun CurrentStateCard(
    deviceIdle: BatteryStatsParser.DeviceIdleInfo?,
    powerManager: BatteryStatsParser.PowerManagerInfo?
) {
    StatsCard(title = "Current State", icon = Icons.Outlined.Info) {
        if (deviceIdle != null) {
            StatRow("Doze state", deviceIdle.currentState)
            StatRow("Light state", deviceIdle.lightState)
            StatRow("Deep Doze enabled", if (deviceIdle.deepEnabled) "Yes" else "No")
            StatRow("Light Doze enabled", if (deviceIdle.lightEnabled) "Yes" else "No")
        }

        if (powerManager != null) {
            Spacer(Modifier.height(8.dp))
            StatRow("Screen", if (powerManager.isScreenOn) "On" else "Off")
            StatRow("Battery level", "${powerManager.batteryLevel}%")
            StatRow("Battery status", powerManager.batteryStatus)
            StatRow("Low power mode", if (powerManager.lowPowerMode) "Yes" else "No")
            StatRow("Device idle mode", powerManager.deviceIdleMode)

            if (powerManager.holdingWakeLocks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Active wake locks: ${powerManager.holdingWakeLocks.size}",
                    style = MaterialTheme.typography.labelMedium)
            }
        }

        if (deviceIdle == null && powerManager == null) {
            Text("No state data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsTab(apps: List<BatteryStatsParser.AppPowerStats>) {
    var sortBy by remember { mutableStateOf(AppSortOption.POWER) }
    var showSystemApps by remember { mutableStateOf(false) }

    val filteredApps = remember(apps, sortBy, showSystemApps) {
        apps.filter { app ->
            showSystemApps || !app.packageName.startsWith("com.android.") &&
                    !app.packageName.startsWith("android") &&
                    !app.packageName.startsWith("com.google.android")
        }.let { list ->
            when (sortBy) {
                AppSortOption.POWER -> list.sortedByDescending { it.powerMah }
                AppSortOption.CPU -> list.sortedByDescending { it.cpuTimeMs }
                AppSortOption.WAKELOCK -> list.sortedByDescending { it.wakeLockTimeMs }
                AppSortOption.NETWORK -> list.sortedByDescending {
                    it.mobileRxBytes + it.mobileTxBytes + it.wifiRxBytes + it.wifiTxBytes
                }
                AppSortOption.FOREGROUND -> list.sortedByDescending { it.foregroundTimeMs }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showSystemApps,
                onClick = { showSystemApps = !showSystemApps },
                label = { Text("System apps") }
            )

            Spacer(Modifier.weight(1f))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                AssistChip(
                    onClick = { expanded = true },
                    label = { Text("Sort: ${sortBy.label}") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AppSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                sortBy = option
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No apps found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(filteredApps, key = { _, app -> app.uid }) { index, app ->
                    AppStatsCard(index + 1, app)
                }
            }
        }
    }
}

private enum class AppSortOption(val label: String) {
    POWER("Power"),
    CPU("CPU Time"),
    WAKELOCK("Wakelock"),
    NETWORK("Network"),
    FOREGROUND("Foreground")
}

@Composable
private fun AppStatsCard(rank: Int, app: BatteryStatsParser.AppPowerStats) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$rank", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        String.format(Locale.getDefault(), "%.2f mAh", app.powerMah),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    Text("Power Breakdown", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    StatRow("CPU", String.format(Locale.getDefault(), "%.2f mAh", app.cpuPowerMah))
                    StatRow("Wakelock", String.format(Locale.getDefault(), "%.2f mAh", app.wakeLockPowerMah))
                    StatRow("Mobile radio", String.format(Locale.getDefault(), "%.2f mAh", app.mobilePowerMah))
                    StatRow("WiFi", String.format(Locale.getDefault(), "%.2f mAh", app.wifiPowerMah))
                    StatRow("GPS", String.format(Locale.getDefault(), "%.2f mAh", app.gpsPowerMah))
                    StatRow("Sensors", String.format(Locale.getDefault(), "%.2f mAh", app.sensorPowerMah))
                    StatRow("Camera", String.format(Locale.getDefault(), "%.2f mAh", app.cameraPowerMah))
                    StatRow("Bluetooth", String.format(Locale.getDefault(), "%.2f mAh", app.bluetoothPowerMah))

                    Spacer(Modifier.height(8.dp))
                    Text("Time Usage", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    StatRow("CPU time", formatDuration(app.cpuTimeMs))
                    StatRow("Wakelock time", formatDuration(app.wakeLockTimeMs))
                    StatRow("Foreground", formatDuration(app.foregroundTimeMs))
                    StatRow("Foreground service", formatDuration(app.foregroundServiceTimeMs))
                    StatRow("Top", formatDuration(app.topTimeMs))
                    StatRow("GPS", formatDuration(app.gpsTimeMs))
                    StatRow("Sensors", formatDuration(app.sensorTimeMs))

                    Spacer(Modifier.height(8.dp))
                    Text("Network", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    StatRow("Mobile RX", formatBytes(app.mobileRxBytes))
                    StatRow("Mobile TX", formatBytes(app.mobileTxBytes))
                    StatRow("WiFi RX", formatBytes(app.wifiRxBytes))
                    StatRow("WiFi TX", formatBytes(app.wifiTxBytes))
                }
            }
        }
    }
}

@Composable
private fun WakelocksTab(
    wakelocks: List<BatteryStatsParser.WakelockStats>,
    kernelWakelocks: List<BatteryStatsParser.KernelWakelockStats>
) {
    var showKernel by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showKernel,
                onClick = { showKernel = false },
                label = { Text("App wakelocks (${wakelocks.size})") }
            )
            FilterChip(
                selected = showKernel,
                onClick = { showKernel = true },
                label = { Text("Kernel (${kernelWakelocks.size})") }
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showKernel) {
                if (kernelWakelocks.isEmpty()) {
                    item {
                        Text("No kernel wakelocks found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(kernelWakelocks, key = { it.name }) { wl ->
                        KernelWakelockCard(wl)
                    }
                }
            } else {
                if (wakelocks.isEmpty()) {
                    item {
                        Text("No app wakelocks found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(wakelocks, key = { "${it.uid}_${it.tag}" }) { wl ->
                        WakelockCard(wl)
                    }
                }
            }
        }
    }
}

@Composable
private fun WakelockCard(wl: BatteryStatsParser.WakelockStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        wl.tag,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        wl.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(wl.type.name) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Count", style = MaterialTheme.typography.labelSmall)
                    Text("${wl.count}", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total time", style = MaterialTheme.typography.labelSmall)
                    Text(formatDuration(wl.totalTimeMs), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun KernelWakelockCard(wl: BatteryStatsParser.KernelWakelockStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                wl.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Count", style = MaterialTheme.typography.labelSmall)
                    Text("${wl.count}", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Active", style = MaterialTheme.typography.labelSmall)
                    Text("${wl.activeCount}", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total time", style = MaterialTheme.typography.labelSmall)
                    Text(formatDuration(wl.totalTimeMs), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun NetworkTab(network: List<BatteryStatsParser.NetworkStats>) {
    var sortBy by remember { mutableStateOf(NetworkSortOption.TOTAL) }

    val sorted = remember(network, sortBy) {
        when (sortBy) {
            NetworkSortOption.TOTAL -> network.sortedByDescending {
                it.mobileRxBytes + it.mobileTxBytes + it.wifiRxBytes + it.wifiTxBytes
            }
            NetworkSortOption.MOBILE -> network.sortedByDescending { it.mobileRxBytes + it.mobileTxBytes }
            NetworkSortOption.WIFI -> network.sortedByDescending { it.wifiRxBytes + it.wifiTxBytes }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NetworkSortOption.entries.forEach { option ->
                FilterChip(
                    selected = sortBy == option,
                    onClick = { sortBy = option },
                    label = { Text(option.label) }
                )
            }
        }

        if (sorted.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No network data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sorted, key = { it.uid }) { net ->
                    NetworkCard(net)
                }
            }
        }
    }
}

private enum class NetworkSortOption(val label: String) {
    TOTAL("Total"),
    MOBILE("Mobile"),
    WIFI("WiFi")
}

@Composable
private fun NetworkCard(net: BatteryStatsParser.NetworkStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                net.packageName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mobile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "↓ ${formatBytes(net.mobileRxBytes)}  ↑ ${formatBytes(net.mobileTxBytes)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("WiFi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    Text(
                        "↓ ${formatBytes(net.wifiRxBytes)}  ↑ ${formatBytes(net.wifiTxBytes)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmsJobsTab(
    alarms: List<BatteryStatsParser.AlarmStats>,
    jobs: List<BatteryStatsParser.JobStats>,
    syncs: List<BatteryStatsParser.SyncStats>
) {
    var selected by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selected) {
            Tab(
                selected = selected == 0,
                onClick = { selected = 0 },
                text = { Text("Alarms (${alarms.size})") }
            )
            Tab(
                selected = selected == 1,
                onClick = { selected = 1 },
                text = { Text("Jobs (${jobs.size})") }
            )
            Tab(
                selected = selected == 2,
                onClick = { selected = 2 },
                text = { Text("Syncs (${syncs.size})") }
            )
        }

        when (selected) {
            0 -> {
                if (alarms.isEmpty()) {
                    EmptyListMessage("No alarm data")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(alarms, key = { "${it.uid}_${it.tag}" }) { alarm ->
                            AlarmCard(alarm)
                        }
                    }
                }
            }
            1 -> {
                if (jobs.isEmpty()) {
                    EmptyListMessage("No job data")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(jobs, key = { "${it.uid}_${it.jobName}" }) { job ->
                            JobCard(job)
                        }
                    }
                }
            }
            2 -> {
                if (syncs.isEmpty()) {
                    EmptyListMessage("No sync data")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(syncs, key = { "${it.uid}_${it.authority}" }) { sync ->
                            SyncCard(sync)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: BatteryStatsParser.AlarmStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(alarm.tag, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(alarm.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("Count", "${alarm.count}")
                StatColumn("Wakeups", "${alarm.wakeups}")
                StatColumn("Time", formatDuration(alarm.totalTimeMs))
            }
        }
    }
}

@Composable
private fun JobCard(job: BatteryStatsParser.JobStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(job.jobName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(job.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("Count", "${job.count}")
                StatColumn("Total time", formatDuration(job.totalTimeMs))
            }
        }
    }
}

@Composable
private fun SyncCard(sync: BatteryStatsParser.SyncStats) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(sync.authority, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sync.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn("Count", "${sync.count}")
                StatColumn("Total time", formatDuration(sync.totalTimeMs))
            }
        }
    }
}

@Composable
private fun SystemTab(
    snapshot: BatteryStatsParser.FullSnapshot?,
    deviceIdle: BatteryStatsParser.DeviceIdleInfo?,
    powerManager: BatteryStatsParser.PowerManagerInfo?
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatsCard(title = "Process Statistics", icon = Icons.Outlined.Memory) {
                if (snapshot?.processStats.isNullOrEmpty()) {
                    Text("No process data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    snapshot.processStats.take(20).forEach { proc ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                proc.processName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                formatDuration(proc.userTimeMs + proc.systemTimeMs),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        item {
            StatsCard(title = "Sensor Usage", icon = Icons.Outlined.Sensors) {
                if (snapshot?.sensors.isNullOrEmpty()) {
                    Text("No sensor data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    snapshot.sensors.take(15).forEach { sensor ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // =====================================
// FILE: ui/screens/DetailedStatsScreen.kt (continued)
// =====================================

                                Text(
                                    sensor.sensorName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    sensor.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                formatDuration(sensor.totalTimeMs),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        item {
            StatsCard(title = "Doze Whitelist", icon = Icons.Outlined.BatteryChargingFull) {
                if (deviceIdle == null) {
                    Text("No whitelist data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        "Whitelisted apps: ${deviceIdle.whitelistedApps.size}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))

                    if (deviceIdle.whitelistedApps.isNotEmpty()) {
                        deviceIdle.whitelistedApps.take(10).forEach { pkg ->
                            Text(
                                pkg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (deviceIdle.whitelistedApps.size > 10) {
                            Text(
                                "... and ${deviceIdle.whitelistedApps.size - 10} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (deviceIdle.tempWhitelistedApps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Temporarily whitelisted: ${deviceIdle.tempWhitelistedApps.size}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        deviceIdle.tempWhitelistedApps.take(5).forEach { pkg ->
                            Text(
                                pkg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            StatsCard(title = "Suspend Blockers", icon = Icons.Outlined.Block) {
                if (powerManager?.suspendBlockers.isNullOrEmpty()) {
                    Text("No suspend blockers active", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    powerManager.suspendBlockers.forEach { blocker ->
                        Text(
                            blocker,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RootTab(
    hasRoot: Boolean,
    kernelBattery: RootStatsCollector.KernelBatteryInfo?,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var cpuInfo by remember { mutableStateOf<List<RootStatsCollector.CpuInfo>>(emptyList()) }
    var thermalZones by remember { mutableStateOf<List<RootStatsCollector.ThermalZone>>(emptyList()) }
    var kernelWakelocks by remember { mutableStateOf<List<RootStatsCollector.KernelWakelockInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(hasRoot) {
        if (hasRoot) {
            isLoading = true
            cpuInfo = RootStatsCollector.getCpuInfo()
            thermalZones = RootStatsCollector.getThermalZones()
            kernelWakelocks = RootStatsCollector.getKernelWakelocks()
            isLoading = false
        }
    }

    if (!hasRoot) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.AdminPanelSettings,
                        null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Root Access Required",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "These statistics require root access to read kernel-level battery information, " +
                                "including cycle count, true battery capacity, kernel wakelocks, and thermal data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Root-only features:", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            listOf(
                                "Battery cycle count",
                                "True capacity (design vs actual)",
                                "Battery age/health percentage",
                                "Kernel wakelocks (native)",
                                "CPU frequency states",
                                "Thermal zone monitoring",
                                "Direct sysfs access"
                            ).forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(feature, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Root Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                onRefresh()
                                cpuInfo = RootStatsCollector.getCpuInfo()
                                thermalZones = RootStatsCollector.getThermalZones()
                                kernelWakelocks = RootStatsCollector.getKernelWakelocks()
                                isLoading = false
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Refresh, "Refresh")
                        }
                    }
                }
            }

            // Battery Health Card
            item {
                BatteryHealthCard(kernelBattery)
            }

            // CPU Frequency Card
            item {
                CpuFrequencyCard(cpuInfo)
            }

            // Thermal Zones Card
            item {
                ThermalZonesCard(thermalZones)
            }

            // Kernel Wakelocks Card
            item {
                KernelWakelocksCard(kernelWakelocks)
            }
        }
    }
}

@Composable
private fun BatteryHealthCard(battery: RootStatsCollector.KernelBatteryInfo?) {
    StatsCard(title = "Battery Health (Kernel)", icon = Icons.Outlined.BatteryFull) {
        if (battery == null) {
            Text("Could not read battery info from /sys", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Cycle count with visual indicator
            battery.cycleCount?.let { cycles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cycle Count", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "$cycles cycles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                cycles < 300 -> MaterialTheme.colorScheme.primary
                                cycles < 500 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    // Health indicator
                    val healthPercent = when {
                        cycles < 100 -> 100
                        cycles < 300 -> 90
                        cycles < 500 -> 75
                        cycles < 800 -> 60
                        else -> 40
                    }
                    CircularProgressIndicator(
                        progress = { healthPercent / 100f },
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            // Capacity comparison
            if (battery.chargeFullDesign != null && battery.chargeFull != null) {
                val designMah = battery.chargeFullDesign / 1000
                val actualMah = battery.chargeFull / 1000
                val healthPct = battery.batteryAge ?: 0.0

                Text("Capacity", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Design", style = MaterialTheme.typography.labelSmall)
                        Text("$designMah mAh", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current", style = MaterialTheme.typography.labelSmall)
                        Text("$actualMah mAh", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Health", style = MaterialTheme.typography.labelSmall)
                        Text(
                            String.format(Locale.getDefault(), "%.1f%%", healthPct),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                healthPct >= 80 -> MaterialTheme.colorScheme.primary
                                healthPct >= 60 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (healthPct / 100f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = when {
                        healthPct >= 80 -> MaterialTheme.colorScheme.primary
                        healthPct >= 60 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                )

                Spacer(Modifier.height(12.dp))
            }

            // Other stats
            battery.technology?.let { StatRow("Technology", it) }
            battery.health?.let { StatRow("Health status", it) }
            battery.status?.let { StatRow("Status", it) }
            battery.currentNow?.let {
                StatRow("Current (kernel)", "${it / 1000} mA")
            }
            battery.voltageNow?.let {
                StatRow("Voltage (kernel)", "${it / 1000} mV")
            }
            battery.tempNow?.let {
                StatRow("Temperature", String.format(Locale.getDefault(), "%.1f °C", it / 10.0))
            }
            battery.timeToEmptyNow?.let {
                if (it > 0) StatRow("Time to empty", formatDuration(it * 1000))
            }
            battery.timeToFullNow?.let {
                if (it > 0) StatRow("Time to full", formatDuration(it * 1000))
            }
        }
    }
}

@Composable
private fun CpuFrequencyCard(cpuInfo: List<RootStatsCollector.CpuInfo>) {
    StatsCard(title = "CPU Frequency", icon = Icons.Outlined.Speed) {
        if (cpuInfo.isEmpty()) {
            Text("Could not read CPU info", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            cpuInfo.forEach { cpu ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Cluster ${cpu.cluster}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(cpu.governor) }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatColumn("Current", "${cpu.currentFreq / 1000} MHz")
                        StatColumn("Min", "${cpu.minFreq / 1000} MHz")
                        StatColumn("Max", "${cpu.maxFreq / 1000} MHz")
                    }

                    // Time in state visualization
                    if (cpu.timeInState.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Time in State", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))

                        val totalTime = cpu.timeInState.values.sum().toFloat().coerceAtLeast(1f)
                        val topStates = cpu.timeInState.entries
                            .sortedByDescending { it.value }
                            .take(5)

                        topStates.forEach { (freq, time) ->
                            val percent = time / totalTime
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${freq / 1000} MHz",
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                LinearProgressIndicator(
                                    progress = { percent },
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                )
                                Text(
                                    String.format(Locale.getDefault(), "%.1f%%", percent * 100),
                                    modifier = Modifier.width(50.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }

                    if (cpuInfo.indexOf(cpu) < cpuInfo.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalZonesCard(thermalZones: List<RootStatsCollector.ThermalZone>) {
    StatsCard(title = "Thermal Zones", icon = Icons.Outlined.Thermostat) {
        if (thermalZones.isEmpty()) {
            Text("Could not read thermal zones", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            thermalZones.forEach { zone ->
                val tempC = zone.tempMilliC / 1000.0
                val tempColor = when {
                    tempC < 40 -> MaterialTheme.colorScheme.primary
                    tempC < 50 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            zone.type,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            zone.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        String.format(Locale.getDefault(), "%.1f°C", tempC),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tempColor
                    )
                }

                // Show trip points if any
                if (zone.tripPoints.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        zone.tripPoints.take(3).forEach { trip ->
                            Text(
                                "${trip.type}: ${trip.tempMilliC / 1000}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (thermalZones.indexOf(zone) < thermalZones.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun KernelWakelocksCard(wakelocks: List<RootStatsCollector.KernelWakelockInfo>) {
    StatsCard(title = "Kernel Wakelocks (Native)", icon = Icons.Outlined.Lock) {
        if (wakelocks.isEmpty()) {
            Text(
                "Could not read kernel wakelocks.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Top ${wakelocks.size.coerceAtMost(15)} wakelocks by time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            wakelocks.take(15).forEach { wl ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            wl.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Count: ${wl.count} | Active: ${wl.activeCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatDuration(wl.totalTime / 1_000_000), // ns to ms
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyListMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(ms: Long): String {
    if (ms < 1000) return "${ms}ms"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.getDefault(), "%.2f GB", gb)
}