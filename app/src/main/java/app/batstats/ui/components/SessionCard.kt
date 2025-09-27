package app.batstats.battery.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import app.batstats.battery.data.db.ChargeSession

@Composable
fun SessionCard(session: ChargeSession) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${session.type} session")
            val duration = if (session.endTime != null) session.endTime - session.startTime else null
            Text("Start: ${session.startTime}")
            session.endTime?.let { Text("End: $it") }
            duration?.let { Text("Duration: ${it/60000} min") }
            session.estCapacityMah?.let { Text("Est. capacity: ${it} mAh") }
        }
    }
}