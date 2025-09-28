package app.batstats.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Battery0Bar
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsPower
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.db.ChargeSession
import app.batstats.battery.data.db.SessionType
import app.batstats.battery.util.TimeEstimator
import app.batstats.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenHistory: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenData: () -> Unit,
    vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory())
) {
    val rt by vm.realtime.collectAsState()
    val session by vm.activeSession.collectAsState()
    val isMonitoring by vm.isMonitoring.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Battery Monitor",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        AnimatedVisibility(rt.sample != null) {
                            Text(
                                "Updated ${
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                        .format(Date(rt.sample?.timestamp ?: System.currentTimeMillis()))
                                }",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Outlined.History, "History")
                    }
                    IconButton(onClick = onOpenAlarms) {
                        Icon(Icons.Outlined.Notifications, "Alarms")
                    }
                    IconButton(onClick = onOpenData) {
                        Icon(Icons.Outlined.CloudDownload, "Data")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, "Settings")
                    }
                },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { pv ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = pv,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { HeroBatteryCard(rt, session) }
            item {
                ControlCenter(
                    session = session,
                    isMonitoring = isMonitoring,
                    onToggleMonitor = { vm.toggleMonitoring() },
                    onStartSession = { vm.startManualSession(it) },
                    onEndSession = { vm.endSession() },
                )
            }
            item { StatsGrid(rt) }
            item { LiveChartCard(vm) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HeroBatteryCard(
    rt: BatteryRepository.Realtime,
    session: ChargeSession?
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Animated gradient background
            val infiniteTransition = rememberInfiniteTransition()
            val animatedOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(10000, easing = LinearEasing)
                ),
                label = "gradient"
            )

            val primaryColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(
                            size.width * (0.3f + animatedOffset * 0.4f),
                            size.height * 0.5f
                        ),
                        radius = size.width * 0.8f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Circular battery indicator
                CircularBatteryIndicator(
                    level = rt.level,
                    isCharging = rt.plugged != 0,
                    current = rt.currentMa,
                    modifier = Modifier.size(180.dp)
                )

                Spacer(Modifier.height(10.dp))

                // ETA with animation
                AnimatedContent(
                    targetState = TimeEstimator.etaString(rt.sample),
                    transitionSpec = {
                        fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                    },
                    label = "eta"
                ) { eta ->
                    if (eta != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = eta,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularBatteryIndicator(
    level: Int,
    isCharging: Boolean,
    current: Int,
    modifier: Modifier = Modifier
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "level"
    )

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = size.center

            // Background circle
            drawCircle(
                color = surfaceVariantColor,
                radius = radius,
                center = center,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Battery level arc
            val sweepAngle = animatedLevel * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = when {
                        isCharging -> listOf(
                            primaryColor,
                            tertiaryColor
                        )
                        level < 20 -> listOf(
                            errorColor,
                            errorContainerColor
                        )
                        else -> listOf(
                            primaryColor,
                            primaryContainerColor
                        )
                    },
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$level",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
//            Text(
//                text = "%",
//                style = MaterialTheme.typography.displaySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
            val statusText = when {
                isCharging && level >= 100 -> "Full"
                isCharging -> "Charging"
                else -> "Discharging"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(current != 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = if (current > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (current > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "${abs(current)} mA",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Charging animation
        if (isCharging) {
            val infiniteTransition = rememberInfiniteTransition()
            val animatedAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "charging"
            )

            Icon(
                imageVector = Icons.Default.OfflineBolt,
                contentDescription = "Charging",
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.TopEnd)
                    .alpha(animatedAlpha),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ControlCenter(
    session: ChargeSession?,
    isMonitoring: Boolean,
    onToggleMonitor: () -> Unit,
    onStartSession: (SessionType) -> Unit,
    onEndSession: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Monitoring",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (isMonitoring) "Running (foreground)" else "Stopped",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMonitoring) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Single toggle button that reflects state
                val busy = remember { mutableStateOf(false) }
                FilledTonalButton(
                    enabled = !busy.value,
                    onClick = {
//                        busy.value = true
                        onToggleMonitor()
//                        busy.value = false
                              },
                    colors = if (isMonitoring)
                        ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    else
                        ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isMonitoring) "Stop" else "Start")
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Session controls
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Session tracking",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (session == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { onStartSession(SessionType.CHARGE) },
                                label = { Text("Charge") },
                                leadingIcon = {
                                    Icon(Icons.Default.BatteryChargingFull, null, Modifier.size(16.dp))
                                }
                            )
                            AssistChip(
                                onClick = { onStartSession(SessionType.DISCHARGE) },
                                label = { Text("Discharge") },
                                leadingIcon = {
                                    Icon(Icons.Default.Battery0Bar, null, Modifier.size(16.dp))
                                }
                            )
                        }
                    } else {
                        TextButton(onClick = onEndSession) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("End")
                        }
                    }
                }

                if (session != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${session.type} session active • started ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                            Date(session.startTime)
                        )}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsGrid(rt: BatteryRepository.Realtime) {
    val stats = listOf(
        Triple(Icons.Outlined.ElectricBolt, "Voltage", "${rt.voltageMv} mV"),
        Triple(Icons.Outlined.SettingsPower, "Power", "%.1f mW".format(rt.powerMw)),
        Triple(Icons.Outlined.Thermostat, "Temperature", "%.1f°C".format(rt.temperatureC)),
        Triple(Icons.Outlined.Battery0Bar, "Health", getHealthString(rt.sample?.health))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.chunked(2).forEach { chunk ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunk.forEach { (icon, label, value) ->
                    StatCard(
                        icon = icon,
                        label = label,
                        value = value,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LiveChartCard(vm: DashboardViewModel) {
    val samples by vm.recentSamples.collectAsState(initial = emptyList())
    val live by vm.liveCurrent.collectAsState(initial = emptyList())

    val chartValues: List<Float> =
        if (samples.isNotEmpty()) samples.map { (it.currentNowUa ?: 0L).toFloat() / 1000f }
        else live

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Current (last 15 minutes)",
                    style = MaterialTheme.typography.titleMedium
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Live",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedLineChart(
                values = chartValues,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
    }
}

@Composable
private fun AnimatedLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val count = values.size
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val hasRange = abs(max - min) > 1e-6f
        val range = if (hasRange) (max - min) else 1f
        val stepX = if (count > 1) size.width / (count - 1) else 0f

        fun mapY(v: Float): Float =
            if (hasRange) size.height - ((v - min) / range) * size.height
            else size.height * 0.5f

        // Gradient fill only when we have a path
        if (count >= 2) {
            val path = Path().apply {
                values.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = mapY(v)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryContainer.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
        }

        // Stroke + marker rendering
        var prev: Offset? = null
        values.forEachIndexed { i, v ->
            val x = if (count > 1) i * stepX else size.width * 0.5f
            val y = mapY(v)
            val p = Offset(x, y)

            prev?.let { pr ->
                drawLine(
                    brush = Brush.horizontalGradient(listOf(primary, primaryContainer)),
                    start = pr,
                    end = p,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            } ?: if (count == 1) {
                drawCircle(color = primary, radius = 4.dp.toPx(), center = p)
                drawCircle(color = primary.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = p)
            } else {
                // nothing
            }

            if (i == values.lastIndex && count > 1) {
                drawCircle(color = primary, radius = 4.dp.toPx(), center = p)
                drawCircle(color = primary.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = p)
            }
            prev = p
        }
    }
}

private fun getHealthString(health: Int?): String = when(health) {
    2 -> "Good"
    3 -> "Overheat"
    4 -> "Dead"
    5 -> "Over voltage"
    6 -> "Failed"
    7 -> "Cold"
    else -> "Unknown"
}