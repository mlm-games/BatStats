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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import app.batstats.R
import app.batstats.battery.util.BatteryStatsParser
import app.batstats.battery.util.RootStatsCollector
import app.batstats.battery.util.ShellRunner
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
    val shizukuRunning by vm.shizukuRunning.collectAsStateWithLifecycle()
    val shizukuDenied by vm.shizukuDenied.collectAsStateWithLifecycle()
    val hasRoot by vm.hasRoot.collectAsStateWithLifecycle()
    val hasAdb by vm.hasAdb.collectAsStateWithLifecycle()
    val hasAdvanced by vm.hasAdvanced.collectAsStateWithLifecycle()
    val advMode by vm.advMode.collectAsStateWithLifecycle()
    val kernelBattery by vm.kernelBattery.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHost = remember { SnackbarHostState() }
    val statsResetSuccessMsg = stringResource(R.string.stats_reset_success)
    val statsResetFailedMsg = stringResource(R.string.stats_reset_failed)

    val tabs = remember {
        listOf(
            StatsTab(R.string.tab_overview, Icons.Outlined.Dashboard),
            StatsTab(R.string.tab_apps, Icons.Outlined.Apps),
            StatsTab(R.string.tab_wakelocks, Icons.Outlined.Alarm),
            StatsTab(R.string.tab_network, Icons.Outlined.Wifi),
            StatsTab(R.string.tab_alarms, Icons.Outlined.Schedule),
            StatsTab(R.string.tab_system, Icons.Outlined.SettingsApplications),
            StatsTab(R.string.tab_root, Icons.Outlined.AdminPanelSettings)
        )
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    LaunchedEffect(Unit) {
        vm.refresh()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.detailed_stats))
                        AnimatedVisibility(visible = lastRefresh > 0) {
                            val timeFormatter = remember(Locale.getDefault()) {
                                DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
                            }
                            val formatted = remember(lastRefresh) {
                                Instant.ofEpochMilli(lastRefresh)
                                    .atZone(ZoneId.systemDefault())
                                    .format(timeFormatter)
                            }
                            val via = when (advMode) {
                                ShellRunner.Mode.NONE -> ""
                                else -> stringResource(R.string.via_mode, advMode.name.lowercase())
                            }
                            Text(
                                stringResource(R.string.updated, formatted + via),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (isRefreshing) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        IconButton(onClick = { scope.launch { vm.refresh() } }) {
                            Icon(Icons.Outlined.Refresh, stringResource(R.string.refresh))
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            if (vm.resetStats()) {
                                snackbarHost.showSnackbar(statsResetSuccessMsg)
                                vm.refresh()
                            } else {
                                snackbarHost.showSnackbar(statsResetFailedMsg)
                            }
                        }
                    }) {
                        Icon(Icons.Outlined.RestartAlt, stringResource(R.string.reset_stats))
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
            if (!hasAdvanced) {
                PrivilegeRequiredCard(
                    hasShizuku = hasShizuku,
                    hasAdb = hasAdb,
                    hasRoot = hasRoot,
                    shizukuRunning = shizukuRunning,
                    shizukuDenied = shizukuDenied,
                    onRequestShizuku = { vm.requestShizukuPermission() },
                    onRecheck = { vm.recheck() }
                )
            } else {
                // Tab row
                SecondaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(stringResource(tab.titleRes)) },
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
                val message = remember(error) { error }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Error,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            message.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        IconButton(onClick = { vm.clearError() }) {
                            Icon(
                                Icons.Outlined.Close,
                                stringResource(R.string.dismiss),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StatsTab(val titleRes: Int, val icon: ImageVector)

@Composable
private fun PrivilegeRequiredCard(
    hasShizuku: Boolean,
    hasAdb: Boolean,
    hasRoot: Boolean,
    shizukuRunning: Boolean,
    shizukuDenied: Boolean,
    onRequestShizuku: () -> Unit,
    onRecheck: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.advanced_stats_required), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        stringResource(R.string.advanced_stats_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        StatusChip(stringResource(R.string.shizuku), hasShizuku)
                        StatusChip(stringResource(R.string.adb_dump), hasAdb)
                        StatusChip(stringResource(R.string.root), hasRoot)
                    }
                    val shizukuHint = when {
                        hasShizuku -> null
                        !shizukuRunning -> stringResource(R.string.shizuku_not_running)
                        shizukuDenied -> stringResource(R.string.shizuku_denied)
                        else -> stringResource(R.string.shizuku_running)
                    }
                    if (shizukuHint != null) {
                        Text(
                            shizukuHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRequestShizuku,
                            enabled = shizukuRunning && !hasShizuku
                        ) { Text(stringResource(R.string.request_shizuku)) }
                        OutlinedButton(onClick = onRecheck) { Text(stringResource(R.string.recheck)) }
                    }
                }
            }
        }
        item {
            AdbGrantCard(context)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.troubleshooting), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.troubleshoot_1), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.troubleshoot_2), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.troubleshoot_3), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.troubleshoot_4), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.troubleshoot_5), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, granted: Boolean) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                stringResource(
                    R.string.granted_yes_no,
                    label,
                    stringResource(if (granted) R.string.yes else R.string.no)
                )
            )
        },
        leadingIcon = {
            Icon(
                if (granted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                null,
                tint = if (granted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (granted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    )
}

@Composable
private fun AdbGrantCard(context: Context) {
    val pkg = context.packageName
    val commands = listOf(
        "adb shell pm grant $pkg android.permission.BATTERY_STATS",
        "adb shell pm grant $pkg android.permission.DUMP",
        "adb shell pm grant $pkg android.permission.PACKAGE_USAGE_STATS",
        "adb shell pm grant $pkg android.permission.INTERACT_ACROSS_USERS",
        "adb shell settings put global hidden_api_policy 1  # optional"
    )
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.adb), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.adb_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            commands.forEach { cmd ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cmd, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()))
                    IconButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("adb", cmd))
                    }) { Icon(Icons.Outlined.ContentCopy, stringResource(R.string.copy), modifier = Modifier.size(18.dp)) }
                }
                HorizontalDivider()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("adb", commands.joinToString("\n")))
                }) { Text(stringResource(R.string.copy_all)) }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text(stringResource(R.string.app_info)) }
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
    StatsCard(titleRes = R.string.summary, icon = Icons.Outlined.Summarize) {
        if (snapshot == null) {
            Text(stringResource(R.string.no_data_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val hours = snapshot.batteryRealtimeMs / 3600000.0
            val screenHours = snapshot.screenOnTimeMs / 3600000.0

            StatRow(R.string.time_on_battery, stringResource(R.string.hours_value, String.format(Locale.getDefault(), "%.1f", hours)))
            StatRow(R.string.screen_on_time, stringResource(R.string.hours_value, String.format(Locale.getDefault(), "%.1f", screenHours)))
            StatRow(R.string.estimated_capacity, "${snapshot.estimatedCapacityMah} mAh")
            StatRow(R.string.apps_tracked, "${snapshot.apps.size}")
            StatRow(R.string.wakelocks, "${snapshot.wakelocks.size}")
            StatRow(R.string.kernel_wakelocks, "${snapshot.kernelWakelocks.size}")
        }
    }
}

@Composable
private fun DischargeBreakdownCard(snapshot: BatteryStatsParser.FullSnapshot?) {
    StatsCard(titleRes = R.string.discharge_breakdown, icon = Icons.Outlined.BatteryAlert) {
        if (snapshot == null) {
            Text(stringResource(R.string.no_data_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DischargeBox(
                    labelRes = R.string.screen_on,
                    percent = snapshot.screenOnDischargePercent,
                    color = MaterialTheme.colorScheme.primary
                )
                DischargeBox(
                    labelRes = R.string.screen_off,
                    percent = snapshot.screenOffDischargePercent,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun DischargeBox(labelRes: Int, percent: Float, color: Color) {
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
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ScreenTimeCard(snapshot: BatteryStatsParser.FullSnapshot?) {
    StatsCard(titleRes = R.string.screen_time_analysis, icon = Icons.Outlined.Smartphone) {
        if (snapshot == null) {
            Text(stringResource(R.string.no_data_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val totalMs = snapshot.batteryRealtimeMs.toFloat().coerceAtLeast(1f)
            val screenOnPercent = (snapshot.screenOnTimeMs / totalMs * 100)
            val screenOffPercent = 100f - screenOnPercent

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.screen_on), style = MaterialTheme.typography.labelMedium)
                    LinearWavyProgressIndicator(
                        progress = { screenOnPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(
                            R.string.percent_of_battery_time,
                            String.format(Locale.getDefault(), "%.1f", screenOnPercent)
                        ),
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

            StatRow(R.string.drain_hour_screen_on, String.format(Locale.getDefault(), "%.2f%%", drainPerHourScreenOn))
            StatRow(R.string.drain_hour_screen_off, String.format(Locale.getDefault(), "%.2f%%", drainPerHourScreenOff))
        }
    }
}

@Composable
private fun SignalQualityCard(snapshot: BatteryStatsParser.FullSnapshot?) {
    StatsCard(titleRes = R.string.signal_quality, icon = Icons.Outlined.SignalCellularAlt) {
        if (snapshot == null || snapshot.signalStrength.isEmpty()) {
            Text(stringResource(R.string.no_signal_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val signalLabels = listOf(
                R.string.signal_none,
                R.string.signal_poor,
                R.string.signal_moderate,
                R.string.signal_good,
                R.string.signal_great
            )
            val colors = listOf(
                MaterialTheme.colorScheme.error,
                MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                MaterialTheme.colorScheme.primary
            )

            Text(stringResource(R.string.mobile_signal), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            snapshot.signalStrength.forEachIndexed { index, stat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(signalLabels.getOrNull(index) ?: R.string.signal_level, index),
                        modifier = Modifier.width(80.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearWavyProgressIndicator(
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
                Text(stringResource(R.string.wifi_signal), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                snapshot.wifiSignal.forEachIndexed { index, stat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(signalLabels.getOrNull(index) ?: R.string.signal_level, index),
                            modifier = Modifier.width(80.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearWavyProgressIndicator(
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
    StatsCard(titleRes = R.string.doze_statistics, icon = Icons.Outlined.PowerSettingsNew) {
        if (doze == null) {
            Text(stringResource(R.string.no_doze_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            StatRow(R.string.deep_doze_time, formatDuration(doze.deepIdleTimeMs))
            StatRow(R.string.deep_doze_count, "${doze.deepIdleCount}")
            StatRow(R.string.light_doze_time, formatDuration(doze.lightIdleTimeMs))
            StatRow(R.string.light_doze_count, "${doze.lightIdleCount}")
            StatRow(R.string.deep_idling_time, formatDuration(doze.deepIdlingTimeMs))
            StatRow(R.string.deep_idling_count, "${doze.deepIdlingCount}")
            StatRow(R.string.light_idling_time, formatDuration(doze.lightIdlingTimeMs))
            StatRow(R.string.light_idling_count, "${doze.lightIdlingCount}")
        }
    }
}

@Composable
private fun BluetoothCard(bluetooth: BatteryStatsParser.BluetoothStats?) {
    StatsCard(titleRes = R.string.bluetooth, icon = Icons.Outlined.Bluetooth) {
        if (bluetooth == null) {
            Text(stringResource(R.string.no_bluetooth_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            StatRow(R.string.idle_time, formatDuration(bluetooth.idleTimeMs))
            StatRow(R.string.rx_time, formatDuration(bluetooth.rxTimeMs))
            StatRow(R.string.tx_time, formatDuration(bluetooth.txTimeMs))
            StatRow(R.string.power_usage, String.format(Locale.getDefault(), "%.2f mAh", bluetooth.powerMah))
            if (bluetooth.scanTimeMs > 0) {
                StatRow(R.string.scan_time, formatDuration(bluetooth.scanTimeMs))
            }
        }
    }
}

@Composable
private fun CurrentStateCard(
    deviceIdle: BatteryStatsParser.DeviceIdleInfo?,
    powerManager: BatteryStatsParser.PowerManagerInfo?
) {
    StatsCard(titleRes = R.string.current_state, icon = Icons.Outlined.Info) {
        if (deviceIdle != null) {
            StatRow(R.string.doze_state, deviceIdle.currentState)
            StatRow(R.string.light_state, deviceIdle.lightState)
            StatRow(R.string.deep_doze_enabled, stringResource(if (deviceIdle.deepEnabled) R.string.yes else R.string.no))
            StatRow(R.string.light_doze_enabled, stringResource(if (deviceIdle.lightEnabled) R.string.yes else R.string.no))
        }

        if (powerManager != null) {
            Spacer(Modifier.height(8.dp))
            StatRow(R.string.screen, stringResource(if (powerManager.isScreenOn) R.string.on else R.string.off))
            StatRow(R.string.battery_level_label, stringResource(R.string.battery_percent, powerManager.batteryLevel))
            StatRow(R.string.battery_status, powerManager.batteryStatus)
            StatRow(R.string.low_power_mode, stringResource(if (powerManager.lowPowerMode) R.string.yes else R.string.no))
            StatRow(R.string.device_idle_mode, powerManager.deviceIdleMode)

            if (powerManager.holdingWakeLocks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    pluralStringResource(
                        R.plurals.active_wakelocks_count,
                        powerManager.holdingWakeLocks.size,
                        powerManager.holdingWakeLocks.size
                    ),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        if (deviceIdle == null && powerManager == null) {
            Text(stringResource(R.string.no_state_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            if (showSystemApps) true else isUserApp(app)
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
                label = { Text(stringResource(R.string.system_apps)) }
            )

            Spacer(Modifier.weight(1f))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                AssistChip(
                    onClick = { expanded = true },
                    label = { Text(stringResource(R.string.sort_by, stringResource(sortBy.titleRes))) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AppSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.titleRes)) },
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
                Text(stringResource(R.string.no_apps_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private enum class AppSortOption(val titleRes: Int) {
    POWER(R.string.sort_power),
    CPU(R.string.sort_cpu_time),
    WAKELOCK(R.string.sort_wakelock),
    NETWORK(R.string.sort_network),
    FOREGROUND(R.string.sort_foreground)
}

private fun isUserApp(app: BatteryStatsParser.AppPowerStats): Boolean {
    val names = if (app.packages.isNotEmpty()) app.packages else listOf(app.packageName)

    if (names.isEmpty() || app.packageName.startsWith("uid:")) {
        return BatteryStatsParser.isUserApp(app.uid, names)
    }
    return BatteryStatsParser.isUserApp(app.uid, names)
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
                        stringResource(
                            R.string.uid_and_power,
                            app.uid,
                            String.format(Locale.getDefault(), "%.2f mAh", app.powerMah)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (app.packages.size > 1) {
                        Text(
                            pluralStringResource(
                                R.plurals.packages_share_uid,
                                app.packages.size,
                                app.packages.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

                    if (app.packages.size > 1) {
                        Text(stringResource(R.string.packages_shared_uid, app.uid), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        app.packages.take(20).forEach { pkg ->
                            Text(pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (app.packages.size > 20) {
                            val more = app.packages.size - 20
                            Text(
                                pluralStringResource(R.plurals.more_count, more, more),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(stringResource(R.string.power_breakdown), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    StatRow(R.string.cpu, String.format(Locale.getDefault(), "%.2f mAh", app.cpuPowerMah))
                    StatRow(R.string.wakelock, String.format(Locale.getDefault(), "%.2f mAh", app.wakeLockPowerMah))
                    StatRow(R.string.mobile_radio, String.format(Locale.getDefault(), "%.2f mAh", app.mobilePowerMah))
                    StatRow(R.string.wifi, String.format(Locale.getDefault(), "%.2f mAh", app.wifiPowerMah))
                    StatRow(R.string.gps, String.format(Locale.getDefault(), "%.2f mAh", app.gpsPowerMah))
                    StatRow(R.string.sensors, String.format(Locale.getDefault(), "%.2f mAh", app.sensorPowerMah))
                    StatRow(R.string.camera, String.format(Locale.getDefault(), "%.2f mAh", app.cameraPowerMah))
                    StatRow(R.string.bluetooth, String.format(Locale.getDefault(), "%.2f mAh", app.bluetoothPowerMah))

                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.time_usage), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    StatRow(R.string.cpu_time, formatDuration(app.cpuTimeMs))
                    StatRow(R.string.wakelock_time, formatDuration(app.wakeLockTimeMs))
                    StatRow(R.string.foreground, formatDuration(app.foregroundTimeMs))
                    StatRow(R.string.foreground_service, formatDuration(app.foregroundServiceTimeMs))
                    StatRow(R.string.top, formatDuration(app.topTimeMs))
                    StatRow(R.string.gps, formatDuration(app.gpsTimeMs))
                    StatRow(R.string.sensors, formatDuration(app.sensorTimeMs))

                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.network), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    StatRow(R.string.mobile_rx, formatBytes(app.mobileRxBytes))
                    StatRow(R.string.mobile_tx, formatBytes(app.mobileTxBytes))
                    StatRow(R.string.wifi_rx, formatBytes(app.wifiRxBytes))
                    StatRow(R.string.wifi_tx, formatBytes(app.wifiTxBytes))
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
                label = {
                    Text(
                        pluralStringResource(
                            R.plurals.app_wakelocks_count,
                            wakelocks.size,
                            wakelocks.size
                        )
                    )
                }
            )
            FilterChip(
                selected = showKernel,
                onClick = { showKernel = true },
                label = { Text(stringResource(R.string.kernel_count, kernelWakelocks.size)) }
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showKernel) {
                if (kernelWakelocks.isEmpty()) {
                    item {
                        Text(stringResource(R.string.no_kernel_wakelocks), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(kernelWakelocks, key = { it.name }) { wl ->
                        KernelWakelockCard(wl)
                    }
                }
            } else {
                if (wakelocks.isEmpty()) {
                    item {
                        Text(stringResource(R.string.no_app_wakelocks), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(stringResource(R.string.count), style = MaterialTheme.typography.labelSmall)
                    Text("${wl.count}", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_time), style = MaterialTheme.typography.labelSmall)
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
                    Text(stringResource(R.string.count), style = MaterialTheme.typography.labelSmall)
                    Text("${wl.count}", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.active), style = MaterialTheme.typography.labelSmall)
                    Text("${wl.activeCount}", style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.total_time), style = MaterialTheme.typography.labelSmall)
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
                    label = { Text(stringResource(option.titleRes)) }
                )
            }
        }

        if (sorted.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_network_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private enum class NetworkSortOption(val titleRes: Int) {
    TOTAL(R.string.total),
    MOBILE(R.string.mobile),
    WIFI(R.string.wifi)
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
                    Text(stringResource(R.string.mobile), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.rx_tx, formatBytes(net.mobileRxBytes), formatBytes(net.mobileTxBytes)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.wifi), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    Text(
                        stringResource(R.string.rx_tx, formatBytes(net.wifiRxBytes), formatBytes(net.wifiTxBytes)),
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
        SecondaryTabRow(selectedTabIndex = selected) {
            Tab(
                selected = selected == 0,
                onClick = { selected = 0 },
                text = {
                    Text(
                        pluralStringResource(R.plurals.alarms_count_plural, alarms.size, alarms.size)
                    )
                }
            )
            Tab(
                selected = selected == 1,
                onClick = { selected = 1 },
                text = { Text(pluralStringResource(R.plurals.jobs_count, jobs.size, jobs.size)) }
            )
            Tab(
                selected = selected == 2,
                onClick = { selected = 2 },
                text = { Text(pluralStringResource(R.plurals.syncs_count, syncs.size, syncs.size)) }
            )
        }

        when (selected) {
            0 -> {
                if (alarms.isEmpty()) {
                    EmptyListMessage(R.string.no_alarm_data)
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
                    EmptyListMessage(R.string.no_job_data)
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
                    EmptyListMessage(R.string.no_sync_data)
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
                StatColumn(R.string.count, "${alarm.count}")
                StatColumn(R.string.wakeups, "${alarm.wakeups}")
                StatColumn(R.string.time, formatDuration(alarm.totalTimeMs))
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
                StatColumn(R.string.count, "${job.count}")
                StatColumn(R.string.total_time, formatDuration(job.totalTimeMs))
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
                StatColumn(R.string.count, "${sync.count}")
                StatColumn(R.string.total_time, formatDuration(sync.totalTimeMs))
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
            StatsCard(titleRes = R.string.process_statistics, icon = Icons.Outlined.Memory) {
                if (snapshot?.processStats.isNullOrEmpty()) {
                    Text(stringResource(R.string.no_process_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            StatsCard(titleRes = R.string.sensor_usage, icon = Icons.Outlined.Sensors) {
                if (snapshot?.sensors.isNullOrEmpty()) {
                    Text(stringResource(R.string.no_sensor_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    snapshot.sensors.take(15).forEach { sensor ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {

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
            StatsCard(titleRes = R.string.doze_whitelist, icon = Icons.Outlined.BatteryChargingFull) {
                if (deviceIdle == null) {
                    Text(stringResource(R.string.no_whitelist), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        stringResource(R.string.whitelisted_apps, deviceIdle.whitelistedApps.size),
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
                            val more = deviceIdle.whitelistedApps.size - 10
                            Text(
                                pluralStringResource(R.plurals.and_more_count, more, more),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (deviceIdle.tempWhitelistedApps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.temporarily_whitelisted, deviceIdle.tempWhitelistedApps.size),
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
            StatsCard(titleRes = R.string.suspend_blockers, icon = Icons.Outlined.Block) {
                if (powerManager?.suspendBlockers.isNullOrEmpty()) {
                    Text(stringResource(R.string.no_suspend_blockers), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        stringResource(R.string.root_access_required),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        stringResource(R.string.root_access_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.root_only), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            listOf(
                                R.string.root_feature_cycles,
                                R.string.root_feature_capacity,
                                R.string.root_feature_health,
                                R.string.root_feature_wakelocks,
                                R.string.root_feature_cpu,
                                R.string.root_feature_thermal,
                                R.string.root_feature_sysfs
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
                                    Text(stringResource(feature), style = MaterialTheme.typography.bodySmall)
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
                            CircularWavyProgressIndicator(modifier = Modifier.size(20.dp))
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
    StatsCard(titleRes = R.string.battery_health_kernel, icon = Icons.Outlined.BatteryFull) {
        if (battery == null) {
            Text(stringResource(R.string.no_battery_info), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            battery.cycleCount?.let { cycles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.cycle_count), style = MaterialTheme.typography.labelMedium)
                        Text(
                            stringResource(R.string.cycles_value, cycles),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                cycles < 300 -> MaterialTheme.colorScheme.primary
                                cycles < 500 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    val healthPercent = when {
                        cycles < 100 -> 100
                        cycles < 300 -> 90
                        cycles < 500 -> 75
                        cycles < 800 -> 60
                        else -> 40
                    }
                    CircularWavyProgressIndicator(
                        progress = { healthPercent / 100f },
                        modifier = Modifier.size(48.dp),
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

                Text(stringResource(R.string.capacity), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(stringResource(R.string.design), style = MaterialTheme.typography.labelSmall)
                        Text("$designMah mAh", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.current), style = MaterialTheme.typography.labelSmall)
                        Text("$actualMah mAh", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.health), style = MaterialTheme.typography.labelSmall)
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
                LinearWavyProgressIndicator(
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
            battery.technology?.let { StatRow(R.string.technology, it) }
            battery.health?.let { StatRow(R.string.health_status, it) }
            battery.status?.let { StatRow(R.string.status, it) }
            battery.currentNow?.let {
                StatRow(R.string.current_kernel, "${it / 1000} mA")
            }
            battery.voltageNow?.let {
                StatRow(R.string.voltage_kernel, "${it / 1000} mV")
            }
            battery.tempNow?.let {
                StatRow(R.string.temperature, stringResource(R.string.temperature_value, String.format(Locale.getDefault(), "%.1f", it / 10.0)))
            }
            battery.timeToEmptyNow?.let {
                if (it > 0) StatRow(R.string.time_to_empty, formatDuration(it * 1000))
            }
            battery.timeToFullNow?.let {
                if (it > 0) StatRow(R.string.time_to_full, formatDuration(it * 1000))
            }
        }
    }
}

@Composable
private fun CpuFrequencyCard(cpuInfo: List<RootStatsCollector.CpuInfo>) {
    StatsCard(titleRes = R.string.cpu_frequency, icon = Icons.Outlined.Speed) {
        if (cpuInfo.isEmpty()) {
            Text(stringResource(R.string.no_cpu_info), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            cpuInfo.forEach { cpu ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.cluster_n, cpu.cluster),
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
                        StatColumn(R.string.current, "${cpu.currentFreq / 1000} MHz")
                        StatColumn(R.string.min, "${cpu.minFreq / 1000} MHz")
                        StatColumn(R.string.max, "${cpu.maxFreq / 1000} MHz")
                    }

                    // Time in state visualization
                    if (cpu.timeInState.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.time_in_state), style = MaterialTheme.typography.labelSmall)
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
                                LinearWavyProgressIndicator(
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
    StatsCard(titleRes = R.string.thermal_zones, icon = Icons.Outlined.Thermostat) {
        if (thermalZones.isEmpty()) {
            Text(stringResource(R.string.no_thermal_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    StatsCard(titleRes = R.string.kernel_wakelocks, icon = Icons.Outlined.Lock) {
        if (wakelocks.isEmpty()) {
            Text(
                stringResource(R.string.could_not_read_wakelocks),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val topCount = wakelocks.size.coerceAtMost(15)
            Text(
                pluralStringResource(R.plurals.top_wakelocks_count, topCount, topCount),
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
                            stringResource(R.string.count_active, wl.count, wl.activeCount),
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
    titleRes: Int,
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
                    stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            content()
        }
    }
}

@Composable
private fun StatRow(labelRes: Int, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            stringResource(labelRes),
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
private fun StatColumn(labelRes: Int, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyListMessage(messageRes: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(messageRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
