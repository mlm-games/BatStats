package app.batstats.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.batstats.battery.drain.DrainState
import app.batstats.battery.drain.formatDrainRate
import app.batstats.battery.drain.formatDuration
import app.batstats.viewmodel.DrainStatsViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrainStatsScreen(
    onBack: () -> Unit,
    vm: DrainStatsViewModel = koinViewModel()
) {
    val drainState by vm.drainState.collectAsStateWithLifecycle()
    val isTracking by vm.isTracking.collectAsStateWithLifecycle()
    val snapshots by vm.snapshots.collectAsStateWithLifecycle()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Drain Statistics")
                        Text(
                            if (isTracking) "Tracking active" else "Tracking paused",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isTracking) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.resetSession() }) {
                        Icon(Icons.Outlined.RestartAlt, "Reset Session")
                    }
                    IconButton(
                        onClick = { if (isTracking) vm.stopTracking() else vm.startTracking() }
                    ) {
                        Icon(
                            if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isTracking) "Pause" else "Start"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CurrentStateCard(drainState)
            }
            item {
                DrainRatesCard(drainState)
            }
            item {
                ScreenBreakdownCard(drainState)
            }
            item {
                DeepSleepCard(drainState)
            }
            item {
                ActivityBreakdownCard(drainState)
            }
            item {
                SessionSummaryCard(drainState)
            }
            item {
                DrainHistoryCard(snapshots)
            }
            
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CurrentStateCard(state: DrainState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Battery Level Circle
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                val progress by animateFloatAsState(
                    targetValue = state.batteryLevel / 100f,
                    animationSpec = tween(1000),
                    label = "battery_progress"
                )
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.3f),
                        radius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                    )
                    
                    drawArc(
                        color = when {
                            state.batteryLevel < 20 -> Color(0xFFE53935)
                            state.batteryLevel < 50 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        },
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }
                
                Text(
                    "${state.batteryLevel}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val (icon, label, color) = when {
                    state.isCharging -> Triple(Icons.Default.BatteryChargingFull, "Charging", MaterialTheme.colorScheme.primary)
                    state.isDeepSleep -> Triple(Icons.Default.NightsStay, "Deep Sleep", Color(0xFF4CAF50))
                    state.isDozing -> Triple(Icons.Default.BedtimeOff, "Dozing", Color(0xFF9C27B0))
                    state.isScreenOn -> Triple(Icons.Default.Smartphone, "Screen On", Color(0xFFFF9800))
                    else -> Triple(Icons.Default.PhonelinkErase, "Screen Off", Color(0xFF2196F3))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    "Session: ${formatDuration(state.totalTimeMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    "Total drain: ${String.format(Locale.getDefault(), "%.1f mAh", state.totalDrainMah)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DrainRatesCard(state: DrainState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Drain Rates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DrainRateItem(
                    icon = Icons.Outlined.Smartphone,
                    label = "Screen On",
                    rate = state.screenOnDrainRate,
                    color = Color(0xFFFF9800)
                )
                DrainRateItem(
                    icon = Icons.Outlined.PhonelinkErase,
                    label = "Screen Off",
                    rate = state.screenOffDrainRate,
                    color = Color(0xFF2196F3)
                )
                DrainRateItem(
                    icon = Icons.Outlined.NightsStay,
                    label = "Deep Sleep",
                    rate = state.deepSleepDrainRate,
                    color = Color(0xFF4CAF50)
                )
                DrainRateItem(
                    icon = Icons.Outlined.WbSunny,
                    label = "Awake",
                    rate = state.awakeDrainRate,
                    color = Color(0xFFE91E63)
                )
            }
        }
    }
}

@Composable
private fun DrainRateItem(
    icon: ImageVector,
    label: String,
    rate: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.padding(12.dp),
                tint = color
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            formatDrainRate(rate),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScreenBreakdownCard(state: DrainState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Screen Time Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Screen On
            DrainStatRow(
                icon = Icons.Outlined.Smartphone,
                label = "Screen On",
                drainRate = state.screenOnDrainRate,
                drainTotal = state.screenOnDrainMah,
                time = state.screenOnTimeMs,
                color = Color(0xFFFF9800),
                percentage = state.screenOnPercentage
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Screen Off
            DrainStatRow(
                icon = Icons.Outlined.PhonelinkErase,
                label = "Screen Off",
                drainRate = state.screenOffDrainRate,
                drainTotal = state.screenOffDrainMah,
                time = state.screenOffTimeMs,
                color = Color(0xFF2196F3),
                percentage = 100f - state.screenOnPercentage
            )
        }
    }
}

@Composable
private fun DrainStatRow(
    icon: ImageVector,
    label: String,
    drainRate: Double,
    drainTotal: Double,
    time: Long,
    color: Color,
    percentage: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(
                formatDrainRate(drainRate),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatDuration(time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                String.format(Locale.getDefault(), "%.1f mAh", drainTotal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(Modifier.height(6.dp))
        
        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun DeepSleepCard(state: DrainState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.NightsStay,
                        null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Deep Sleep",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                ) {
                    Text(
                        String.format(Locale.getDefault(), "%.0f%%", state.deepSleepPercentage),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                "of screen-off time in deep sleep",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { (state.deepSleepPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDuration(state.deepSleepTimeMs),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatDrainRate(state.deepSleepDrainRate),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Drain Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format(Locale.getDefault(), "%.1f mAh", state.deepSleepDrainMah),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Total Drain",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityBreakdownCard(state: DrainState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Activity Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Active
                ActivityStatItem(
                    icon = Icons.Outlined.FlashOn,
                    label = "Active",
                    drainRate = state.activeDrainRate,
                    time = state.activeTimeMs,
                    drainTotal = state.activeDrainMah,
                    color = Color(0xFFF44336)
                )
                
                // Idle
                ActivityStatItem(
                    icon = Icons.Outlined.Bedtime,
                    label = "Idle",
                    drainRate = state.idleDrainRate,
                    time = state.idleTimeMs,
                    drainTotal = state.idleDrainMah,
                    color = Color(0xFF2196F3)
                )
                
                // Awake (Screen Off)
                ActivityStatItem(
                    icon = Icons.Outlined.WbSunny,
                    label = "Awake",
                    drainRate = state.awakeDrainRate,
                    time = state.awakeTimeMs,
                    drainTotal = state.awakeDrainMah,
                    color = Color(0xFFE91E63)
                )
            }
        }
    }
}

@Composable
private fun ActivityStatItem(
    icon: ImageVector,
    label: String,
    drainRate: Double,
    time: Long,
    drainTotal: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.padding(10.dp),
                tint = color
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            formatDrainRate(drainRate),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            formatDuration(time),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            String.format(Locale.getDefault(), "%.1f mAh", drainTotal),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionSummaryCard(state: DrainState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Session Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = "Duration",
                    value = formatDuration(state.totalTimeMs),
                    icon = Icons.Outlined.Timer
                )
                SummaryItem(
                    label = "Total Drain",
                    value = String.format(Locale.getDefault(), "%.1f mAh", state.totalDrainMah),
                    icon = Icons.Outlined.BatteryAlert
                )
                SummaryItem(
                    label = "Avg Rate",
                    value = formatDrainRate(state.averageDrainRate),
                    icon = Icons.Outlined.Speed
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DrainHistoryCard(snapshots: List<app.batstats.battery.drain.DrainSnapshot>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Drain History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(16.dp))
            
            AnimatedVisibility(
                visible = snapshots.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val values = snapshots.map { it.batteryLevel.toFloat() }
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    if (values.isEmpty()) return@Canvas
                    
                    val min = values.minOrNull() ?: 0f
                    val max = values.maxOrNull() ?: 100f
                    val range = (max - min).coerceAtLeast(1f)
                    
                    val stepX = size.width / (values.size - 1).coerceAtLeast(1)
                    
                    // Draw gradient fill
                    val path = androidx.compose.ui.graphics.Path().apply {
                        values.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = size.height - ((v - min) / range) * size.height
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    
                    drawPath(
                        path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    
                    // Draw line
                    var prev: Offset? = null
                    values.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - ((v - min) / range) * size.height
                        val current = Offset(x, y)
                        
                        prev?.let { pr ->
                            drawLine(
                                color = Color(0xFF4CAF50),
                                start = pr,
                                end = current,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        prev = current
                    }
                }
            }
            
            AnimatedVisibility(
                visible = snapshots.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Collecting data...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}