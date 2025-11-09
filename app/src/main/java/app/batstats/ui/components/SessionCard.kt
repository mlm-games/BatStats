package app.batstats.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.batstats.battery.data.db.ChargeSession
import app.batstats.battery.data.db.SessionType
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCard(
    session: ChargeSession,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember(Locale.getDefault()) {
        DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.getDefault())
    }
    val startText = remember(session.startTime) {
        Instant.ofEpochMilli(session.startTime)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }
    val isActive = session.endTime == null
    val durationMs = (session.endTime ?: System.currentTimeMillis()) - session.startTime

    Card(
        modifier = modifier,
        onClick = {},
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Session type icon with background
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (session.type) {
                    SessionType.CHARGE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    SessionType.DISCHARGE -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = when (session.type) {
                        SessionType.CHARGE -> Icons.Default.BatteryChargingFull
                        SessionType.DISCHARGE -> Icons.Default.Battery0Bar
                    },
                    contentDescription = session.type.name,
                    tint = when (session.type) {
                        SessionType.CHARGE -> MaterialTheme.colorScheme.primary
                        SessionType.DISCHARGE -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = when (session.type) {
                                SessionType.CHARGE -> "Charging Session"
                                SessionType.DISCHARGE -> "Discharge Session"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = startText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BatteryLevelChip(session.startLevel)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BatteryLevelChip(session.endLevel ?: session.startLevel)
                }

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatChip(
                        icon = Icons.Default.Schedule,
                        text = formatDuration(durationMs)
                    )

                    session.estCapacityMah?.let {
                        StatChip(
                            icon = Icons.Default.Battery0Bar,
                            text = "~${it} mAh"
                        )
                    }

                    session.avgCurrentUa?.let {
                        val maAbs = kotlin.math.abs(it / 1000)
                        StatChip(
                            icon = Icons.Default.ElectricBolt,
                            text = "$maAbs mA avg"
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun BatteryLevelChip(level: Int) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            level < 20 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            level < 50 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        }
    ) {
        Text(
            text = "$level%",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = millis / 60000
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}