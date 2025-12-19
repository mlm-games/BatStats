package app.batstats.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.batstats.settings.AppSettings
import app.batstats.settings.AppSettingsSchema
import io.github.mlmgames.settings.core.SettingsRepository
import io.github.mlmgames.settings.core.backup.ExportResult
import io.github.mlmgames.settings.core.backup.ImportResult
import io.github.mlmgames.settings.core.backup.SettingsBackupManager
import io.github.mlmgames.settings.core.managers.ResetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val context: Context,
    private val repository: SettingsRepository<AppSettings>,
    private val resetManager: ResetManager<AppSettings>,
    private val backupManager: SettingsBackupManager<AppSettings>
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.flow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsSchema.default
        )

    val dynamicColors: Flow<Boolean> = settings.map { it.dynamicColors }
    val themeIndex: Flow<Int> = settings.map { it.themeIndex }

    fun updateSetting(name: String, value: Any) {
        viewModelScope.launch {
            repository.set(name, value)
        }
    }

    fun <V> observeField(fieldName: String): Flow<V> = repository.observeField(fieldName)

    suspend fun resetUISettings(): Int = resetManager.resetUISettings()
    suspend fun resetAll(): Int = resetManager.resetAll()

    suspend fun exportToFile(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            when (val result = backupManager.export()) {
                is ExportResult.Success -> {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(result.json.toByteArray())
                        }
                        "Settings saved to ${uri.path}"
                    } catch (e: Exception) {
                        "Failed to write file: ${e.message}"
                    }
                }
                is ExportResult.Error -> "Export generation failed: ${result.message}"
            }
        }
    }

    suspend fun import(json: String): ImportResult = backupManager.import(json)
}