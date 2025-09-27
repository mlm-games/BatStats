package app.batstats.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.db.ChargeSession
import app.batstats.battery.util.TimeEstimator
import app.batstats.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

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
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
                                        Date()
                                    )}",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .verticalScroll(scrollState)
        ) {
            // Hero Battery Status Card
            HeroBatteryCard(rt, session)

            // Control Center
            ControlCenter(
                session = session,
                onStartMonitor = { vm.startOrBindService() },
                onStopMonitor = { vm.stopService() },
                onStartSession = { vm.startManualSession() },
                onEndSession = { vm.endSession() }
            )

            // Stats Grid
            StatsGrid(rt)

            // Live Chart
            LiveChartCard(vm)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroBatteryCard(
    rt: BatteryRepository.Realtime,
    session: ChargeSession?
) {
    Card(
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

                Spacer(Modifier.height(20.dp))

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
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "%",
                style = MaterialTheme.typography.titleLarge,
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
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit
) {
    Card(
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
                    AnimatedContent(
                        targetState = session,
                        label = "session"
                    ) { s ->
                        Text(
                            if (s == null) "No active session" else "${s.type} session active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

//                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    FilledTonalButton(
//                        onClick = onStartMonitor,
//                        colors = ButtonDefaults.filledTonalButtonColors(
//                            containerColor = MaterialTheme.colorScheme.primaryContainer
//                        )
//                    ) {
//                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
//                        Spacer(Modifier.width(4.dp))
//                        Text("Start")
//                    }
//
//                    OutlinedButton(onClick = onStopMonitor) {
//                        Icon(Icons.Default.Stop, null, Modifier.size(18.dp))
//                        Spacer(Modifier.width(4.dp))
//                        Text("Stop")
//                    }
//                }
            }

            AnimatedVisibility(visible = true) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

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
                        TextButton(onClick = onStartSession) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start")
                        }
                    } else {
                        TextButton(onClick = onEndSession) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("End")
                        }
                    }
                }
            }
        }
    }
}

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
    Card(
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
    val samples by vm.recentSamples.collectAsState(listOf())

    Card(
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
                values = samples.map { (it.currentNowUa ?: 0L).toFloat() / 1000f },
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

        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = (max - min).takeIf { abs(it) > 1e-6 } ?: 1f
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)

        // Draw gradient fill
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range) * size.height

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Complete the path for fill
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
        path.close()

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryContainer.copy(alpha = 0.3f),
                    Color.Transparent
                )
            )
        )

        // Draw the line
        var prevPoint: Offset? = null
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range) * size.height
            val currentPoint = Offset(x, y)

            prevPoint?.let { prev ->
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(primary, primaryContainer)
                    ),
                    start = prev,
                    end = currentPoint,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Draw point
            if (i == values.lastIndex) {
                drawCircle(
                    color = primary,
                    radius = 4.dp.toPx(),
                    center = currentPoint
                )
                drawCircle(
                    color = primary.copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = currentPoint
                )
            }

            prevPoint = currentPoint
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