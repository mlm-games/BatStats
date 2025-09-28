package app.batstats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
                val count = values.size
                val min = values.minOrNull() ?: 0f
                val max = values.maxOrNull() ?: 1f
                val hasRange = abs(max - min) > 1e-6f
                val range = if (hasRange) (max - min) else 1f
                val stepX = if (count > 1) size.width / (count - 1) else 0f
                fun y(v: Float) = if (hasRange) size.height - ((v - min) / range) * size.height else size.height * 0.5f

                var prev: Offset? = null
                values.forEachIndexed { i, v ->
                    val x = if (count > 1) i * stepX else size.width * 0.5f
                    val p = Offset(x, y(v))
                    prev?.let {
                        drawLine(
                            color = lineColor,
                            start = it,
                            end = p,
                            strokeWidth = 3f
                        )
                    } ?: if (count == 1) {
                        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = p)
                    } else {

                    }
                    prev = p
                }
            }
        }
    }
}
