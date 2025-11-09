package app.batstats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.batstats.battery.data.db.SessionType
import app.batstats.battery.viewmodel.HistoryViewModel
import app.batstats.ui.components.SessionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory())
) {
    val allSessions by vm.sessions.collectAsStateWithLifecycle(initialValue = emptyList())
    var filter by remember { mutableStateOf<SessionType?>(null) }
    var query by remember { mutableStateOf("") }
    val sessions = remember(allSessions, filter, query) {
        allSessions
            .filter { s -> filter == null || s.type == filter }
            .filter { s ->
                if (query.isBlank()) true
                else s.sessionId.contains(query, ignoreCase = true)
            }
    }

    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(behavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* could open search field modal; kept inline below */ }) {
                        Icon(Icons.Outlined.Search, null)
                    }
                    IconButton(onClick = { /* filter popover handled inline */ }) {
                        Icon(Icons.Outlined.FilterAlt, null)
                    }
                },
                scrollBehavior = behavior
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .fillMaxSize()
        ) {
            // Filters
            FilterRow(
                filter = filter,
                onFilter = { filter = it },
                query = query,
                onQuery = { query = it }
            )

            // Content
            if (sessions.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sessions, key = { it.sessionId }) { s ->
                        SessionCard(
                            session = s,
                            modifier = Modifier.clickable { onOpenSession(s.sessionId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    filter: SessionType?,
    onFilter: (SessionType?) -> Unit,
    query: String,
    onQuery: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == null,
                onClick = { onFilter(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = filter == SessionType.CHARGE,
                onClick = { onFilter(SessionType.CHARGE) },
                label = { Text("Charge") }
            )
            FilterChip(
                selected = filter == SessionType.DISCHARGE,
                onClick = { onFilter(SessionType.DISCHARGE) },
                label = { Text("Discharge") }
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Search by session id…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No sessions yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Start a session or begin monitoring to see history here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
