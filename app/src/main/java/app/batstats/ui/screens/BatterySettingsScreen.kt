package app.batstats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.batstats.battery.BatteryGraph
import app.batstats.settings.AppSettings
import app.batstats.settings.AppSettingsSchema
import app.batstats.settings.Data
import app.batstats.settings.Display
import app.batstats.settings.General
import app.batstats.settings.Notifications
import app.batstats.viewmodel.SettingsViewModel
import io.github.mlmgames.settings.core.SettingField
import io.github.mlmgames.settings.core.SettingMeta
import io.github.mlmgames.settings.core.backup.ExportResult
import io.github.mlmgames.settings.core.backup.ImportResult
import io.github.mlmgames.settings.core.resources.StringResourceProvider
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle
import io.github.mlmgames.settings.ui.ProvideStringResources
import io.github.mlmgames.settings.ui.components.SettingsAction
import io.github.mlmgames.settings.ui.components.SettingsItem
import io.github.mlmgames.settings.ui.components.SettingsSection
import io.github.mlmgames.settings.ui.components.SettingsToggle
import io.github.mlmgames.settings.ui.dialogs.DropdownSettingDialog
import io.github.mlmgames.settings.ui.dialogs.SliderSettingDialog
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatterySettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinViewModel(),
    stringProvider: StringResourceProvider = koinInject()
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHost = remember { SnackbarHostState() }

    var showResetDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    var showDropdown by remember { mutableStateOf(false) }
    var showSlider by remember { mutableStateOf(false) }
    var currentField by remember { mutableStateOf<SettingField<AppSettings, *>?>(null) }

    val schema = AppSettingsSchema
    val grouped = remember { schema.groupedByCategory() }

    val categoryOrder = listOf(
        General::class to "General",
        Notifications::class to "Notifications",
        Display::class to "Display",
        Data::class to "Data & Export"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Settings")
                        Text(
                            "Customize app behavior",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Outlined.Backup, contentDescription = "Export settings")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Outlined.Restore, contentDescription = "Import settings")
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset settings")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        ProvideStringResources(stringProvider) {
            LazyColumn(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryOrder.forEach { (categoryClass, categoryTitle) ->
                    val fields = grouped[categoryClass].orEmpty()
                    if (fields.isEmpty()) return@forEach

                    item(key = "header_$categoryTitle") {
                        Text(
                            text = categoryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    item(key = "section_$categoryTitle") {
                        SettingsSection(title = "") {
                            fields.forEach { field ->
                                val meta = field.meta ?: return@forEach
                                val enabled = schema.isEnabled(settings, field)

                                RenderSettingField(
                                    field = field,
                                    meta = meta,
                                    settings = settings,
                                    enabled = enabled,
                                    onToggle = { value ->
                                        vm.updateSetting(field.name, value)
                                    },
                                    onOpenDropdown = {
                                        currentField = field
                                        showDropdown = true
                                    },
                                    onOpenSlider = {
                                        currentField = field
                                        showSlider = true
                                    }
                                )
                            }

                            // Add action buttons for Data category
                            if (categoryClass == Data::class) {
                                SettingsAction(
                                    title = "Export Battery Data",
                                    description = "Export battery history to file",
                                    buttonText = "Export",
                                    onClick = {
                                        // TODO: Link to the export screen or remove (can directly navigate there...)
                                        scope.launch {
                                            snackbarHost.showSnackbar("Export not implemented yet")
                                        }
                                    }
                                )

                                SettingsAction(
                                    title = "Clear All Data",
                                    description = "Delete all stored battery data",
                                    buttonText = "Clear",
                                    onClick = { showClearDataDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dropdown Dialog
    val cf = currentField
    if (showDropdown && cf?.meta != null) {
        val meta = cf.meta!!
        @Suppress("UNCHECKED_CAST")
        val anyField = cf as SettingField<AppSettings, Any?>
        val value = anyField.get(settings)

        // Handle both Int and Enum Dropdowns safely
        val index = when(value) {
            is Int -> value
            is Enum<*> -> value.ordinal
            else -> 0
        }

        if (meta.options.isNotEmpty()) {
            DropdownSettingDialog(
                title = meta.title,
                options = meta.options,
                selectedIndex = index,
                onDismiss = { showDropdown = false },
                onOptionSelected = { idx ->
                    // For Enum fields, we need to handle mapping if we were supporting them generically
                    // Since AppSettings mostly uses indices (Int), we pass Int directly.
                    vm.updateSetting(cf.name, idx)
                    showDropdown = false
                }
            )
        } else {
            showDropdown = false
        }
    }

    // Slider Dialog - Fixed Type Safety
    if (showSlider && cf?.meta != null) {
        val meta = cf.meta!!
        @Suppress("UNCHECKED_CAST")
        val anyField = cf as SettingField<AppSettings, Any?>
        val value = anyField.get(settings)

        // Robustly handle both Float and Int types without unchecked casting crashes
        val currentVal = when (value) {
            is Float -> value
            is Int -> value.toFloat()
            is Long -> value.toFloat()
            is Double -> value.toFloat()
            else -> 0f
        }

        SliderSettingDialog(
            title = meta.title,
            currentValue = currentVal,
            min = meta.min,
            max = meta.max,
            step = meta.step,
            onDismiss = { showSlider = false },
            onValueSelected = { v ->
                // Convert back to the original type expected by the field
                when (value) {
                    is Float -> vm.updateSetting(cf.name, v)
                    is Int -> vm.updateSetting(cf.name, v.toInt())
                    is Long -> vm.updateSetting(cf.name, v.toLong())
                    is Double -> vm.updateSetting(cf.name, v.toDouble())
                }
                showSlider = false
            }
        )
    }

    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Settings") },
            text = {
                Column {
                    Text("Choose what to reset:")
                    Spacer(Modifier.height(16.dp))
                    Text("• Reset UI Settings: Resets visible settings only", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("• Reset All: Resets everything", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch {
                            vm.resetUISettings()
                            snackbarHost.showSnackbar("UI settings reset")
                            showResetDialog = false
                        }
                    }) {
                        Text("Reset UI")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            scope.launch {
                                vm.resetAll()
                                snackbarHost.showSnackbar("All settings reset")
                                showResetDialog = false
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset All")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Settings") },
            text = { Text("Export all settings to JSON for backup or transfer.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (val result = vm.export()) {
                            is ExportResult.Success -> {
                                // TODO: Share or save the JSON
                                snackbarHost.showSnackbar("Exported ${result.json.length} bytes")
                            }
                            is ExportResult.Error -> {
                                snackbarHost.showSnackbar("Export failed: ${result.message}")
                            }
                        }
                        showExportDialog = false
                    }
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Settings") },
            text = {
                Column {
                    Text("Paste exported settings JSON:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = jsonInput,
                        onValueChange = { jsonInput = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        placeholder = { Text("Paste JSON here...") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (val result = vm.import(jsonInput)) {
                                is ImportResult.Success -> {
                                    snackbarHost.showSnackbar("Imported ${result.appliedCount} settings")
                                }
                                is ImportResult.Error -> {
                                    snackbarHost.showSnackbar("Import failed: ${result.error}")
                                }
                            }
                            showImportDialog = false
                        }
                    },
                    enabled = jsonInput.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all battery history, sessions, and statistics. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            BatteryGraph.db.clearAllTables()
                            snackbarHost.showSnackbar("All data cleared")
                            showClearDataDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RenderSettingField(
    field: SettingField<AppSettings, *>,
    meta: SettingMeta,
    settings: AppSettings,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenDropdown: () -> Unit,
    onOpenSlider: () -> Unit
) {
    when (meta.type) {
        Toggle::class -> {
            @Suppress("UNCHECKED_CAST")
            val boolField = field as? SettingField<AppSettings, Boolean>
            if (boolField != null) {
                SettingsToggle(
                    title = meta.title,
                    description = meta.description.takeIf { it.isNotBlank() },
                    checked = boolField.get(settings),
                    enabled = enabled,
                    onCheckedChange = onToggle
                )
            }
        }

        Dropdown::class -> {
            @Suppress("UNCHECKED_CAST")
            val anyField = field as SettingField<AppSettings, Any?>
            val value = anyField.get(settings)

            // Handle Int (Index) or Enum safely
            val index = when (value) {
                is Int -> value
                is Enum<*> -> value.ordinal
                else -> 0
            }

            if (meta.options.isNotEmpty()) {
                SettingsItem(
                    title = meta.title,
                    subtitle = meta.options.getOrNull(index) ?: "Unknown",
                    description = meta.description.takeIf { it.isNotBlank() },
                    enabled = enabled,
                    onClick = onOpenDropdown
                )
            }
        }

        Slider::class -> {
            // Safer way to access value despite type erasure
            @Suppress("UNCHECKED_CAST")
            val anyField = field as SettingField<AppSettings, Any?>
            val value = anyField.get(settings)

            // Ensure String.format gets a float if checking for float format, or handle Int explicitly
            val subtitle = when (value) {
                is Float -> String.format(Locale.getDefault(), "%.1f", value)
                is Double -> String.format(Locale.getDefault(), "%.1f", value)
                is Int -> value.toString()
                is Long -> value.toString()
                else -> ""
            }

            SettingsItem(
                title = meta.title,
                subtitle = subtitle,
                description = meta.description.takeIf { it.isNotBlank() },
                enabled = enabled,
                onClick = onOpenSlider
            )
        }
    }
}