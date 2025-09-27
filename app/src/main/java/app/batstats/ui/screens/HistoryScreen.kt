package app.batstats.battery.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.batstats.battery.viewmodel.HistoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.batstats.ui.components.MyScreenScaffold
import app.batstats.battery.ui.components.SessionCard

@Composable
fun HistoryScreen(onBack: () -> Unit, vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory())) {
    val sessions by vm.sessions.collectAsState(listOf())
    MyScreenScaffold(title = "History", actions = { TextButton(onClick = onBack) { Text("Back") } }) { _ ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sessions.size) { idx ->
                val s = sessions[idx]
                SessionCard(session = s)
            }
        }
    }
}
