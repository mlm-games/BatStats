package app.batstats.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.batstats.battery.data.ExportImport
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var from by remember { mutableLongStateOf(0L) }
    var to by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var includeSamples by remember { mutableStateOf(true) }
    var includeSessions by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            busy = true
            val ok = ExportImport.exportJson(
                dest = uri,
                from = from,
                to = to,
                includeSamples = includeSamples,
                includeSessions = includeSessions
            )
            busy = false
            info = if (ok) "JSON exported" else "Export failed"
            snackbarHost.showSnackbar(info!!)
        }
    }

    val folderCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { tree ->
        if (tree != null) scope.launch {
            busy = true
            val ok = ExportImport.exportCsvToFolder(
                tree = tree,
                from = from,
                to = to
            )
            busy = false
            info = if (ok) "CSV exported" else "Export failed"
            snackbarHost.showSnackbar(info!!)
        }
    }

    val openJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val ok = ExportImport.importJson(uri)
            busy = false
            info = if (ok) "JSON imported" else "Import failed"
            snackbarHost.showSnackbar(info!!)
        }
    }

    val openCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val ok = ExportImport.importCsv(uri)
            busy = false
            info = if (ok) "CSV imported" else "Import failed"
            snackbarHost.showSnackbar(info!!)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Data export/import") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { pv ->
        Column(
            Modifier
                .padding(pv)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Date range", style = MaterialTheme.typography.titleMedium)
                    DateRangeRow(from = from, to = to, onFrom = { from = it }, onTo = { to = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = includeSamples,
                            onClick = { includeSamples = !includeSamples },
                            label = { Text("Samples") }
                        )
                        FilterChip(
                            selected = includeSessions,
                            onClick = { includeSessions = !includeSessions },
                            label = { Text("Sessions") }
                        )
                    }
                }
            }

            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Export", style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Outlined.Download, null)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { createJson.launch("BatteryExport.json") }, enabled = !busy) {
                            Text("Export JSON")
                        }
                        OutlinedButton(onClick = { folderCsv.launch(null) }, enabled = !busy) {
                            Text("Export CSV (folder)")
                        }
                    }
                    AnimatedVisibility(visible = busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }

            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Import", style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.Outlined.Upload, null)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { openJson.launch(arrayOf("application/json")) },
                            enabled = !busy
                        ) { Text("Import JSON") }
                        OutlinedButton(
                            onClick = { openCsv.launch(arrayOf("text/*", "application/octet-stream")) },
                            enabled = !busy
                        ) { Text("Import CSV") }
                    }
                    Text(
                        "CSV import: open either battery_samples.csv or charge_sessions.csv",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRangeRow(from: Long, to: Long, onFrom: (Long) -> Unit, onTo: (Long) -> Unit) {
    val df = remember(Locale.getDefault()) {
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    }
    fun format(ms: Long): String = Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault())
        .format(df)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text("From: ${if (from == 0L) "Beginning" else format(from)}")
        Text("To: ${format(to)}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onFrom(0L) }) { Text("All") }
            OutlinedButton(onClick = { onFrom(System.currentTimeMillis() - 7L * 24 * 3600000) }) { Text("Last 7 days") }
            OutlinedButton(onClick = { onFrom(System.currentTimeMillis() - 30L * 24 * 3600000) }) { Text("Last 30 days") }
        }
    }
}
