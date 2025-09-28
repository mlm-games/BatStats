package app.batstats.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.batstats.viewmodel.SessionDetailsViewModel
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailsScreen(
    sessionId: String,
    onBack: () -> Unit,
    vm: SessionDetailsViewModel = viewModel(factory = SessionDetailsViewModel.factory(sessionId))
) {
    val ui by vm.ui.collectAsState()

    Scaffold(topBar = {
        LargeTopAppBar(
            title = { Text("Session details") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            actions = {
                IconButton(onClick = { /* share later */ }) { Icon(Icons.Outlined.Share, null) }
                IconButton(onClick = { /* export later */ }) { Icon(Icons.Outlined.Download, null) }
            }
        )
    }) { pv ->
        Column(
            Modifier.padding(pv).fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${ui.type} • ${ui.levelRange}", style = MaterialTheme.typography.titleMedium)
                    Text("Start: ${df.format(Date(ui.start))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("End: ${ui.end?.let { df.format(Date(it)) } ?: "Active"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ui.capacityMah?.let {
                            AssistChip(onClick = {}, label = { Text("~${it} mAh") })
                        }
                        ui.avgCurrent?.let {
                            AssistChip(onClick = {}, label = { Text("${it / 1000} mA avg") })
                        }
                    }
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val errorColor = MaterialTheme.colorScheme.error

            ChartCard("Current (mA)") {
                val values = ui.points.map { it.currentMa?.toFloat() ?: 0f }
                drawSeries(values, primaryColor)
            }
            ChartCard("Voltage (mV)") {
                val values = ui.points.map { it.voltageMv?.toFloat() ?: 0f }
                drawSeries(values, tertiaryColor)
            }
            ChartCard("Temperature (°C)") {
                val values = ui.points.map { it.tempC?.toFloat() ?: 0f }
                drawSeries(values, errorColor)
            }

            AnimatedVisibility(visible = ui.points.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "No data points captured yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    drawBlock: DrawScope.() -> Unit
) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            val scroll = rememberScrollState()
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .horizontalScroll(scroll)
            ) {
                Canvas(modifier = Modifier.width(1100.dp).height(180.dp)) {
                    drawBlock()
                }
            }
        }
    }
}

private fun DrawScope.drawSeries(values: List<Float>, color: Color) {
    if (values.isEmpty()) return
    val count = values.size
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 1f
    val hasRange = kotlin.math.abs(max - min) > 1e-6f
    val range = if (hasRange) (max - min) else 1f
    val stepX = if (count > 1) size.width / (count - 1) else 0f

    fun mapY(v: Float) = if (hasRange) {
        size.height - ((v - min) / range) * size.height
    } else size.height * 0.5f

    var prev: Offset? = null
    values.forEachIndexed { i, v ->
        val x = if (count > 1) i * stepX else size.width * 0.5f
        val p = Offset(x, mapY(v))
        prev?.let {
            drawLine(color = color, start = it, end = p, strokeWidth = 3f)
        } ?: if (count == 1) {
            drawCircle(color = color, radius = 4.dp.toPx(), center = p)
        } else {

        }
        prev = p
    }
}