package app.batstats.battery.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun MiniLineChart(title: String, values: List<Float>) {
    val lineColor = MaterialTheme.colorScheme.primary
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (values.isEmpty()) return@Canvas
                val min = values.min()
                val max = values.max()
                val range = (max - min).takeIf { abs(it) > 1e-6 } ?: 1f
                val stepX = size.width / (values.size - 1).coerceAtLeast(1)
                var prev: Offset? = null
                values.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = size.height - ((v - min) / range) * size.height
                    val p = Offset(x, y)
                    prev?.let {
                        drawLine(
                            color = lineColor,
                            start = it,
                            end = p,
                            strokeWidth = 3f
                        )
                    }
                    prev = p
                }
            }
        }
    }
}