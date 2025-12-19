package app.batstats.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.batstats.battery.data.ExportImportManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataViewModel(
    private val exportImportManager: ExportImportManager
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun exportJson(
        uri: Uri,
        from: Long,
        to: Long,
        includeSamples: Boolean,
        includeSessions: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val success = exportImportManager.exportJson(uri, from, to, includeSamples, includeSessions)
            _message.value = if (success) "JSON Exported Successfully" else "Export Failed"
            _isBusy.value = false
        }
    }

    fun exportCsv(uri: Uri, from: Long, to: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            val success = exportImportManager.exportCsvToFolder(uri, from, to)
            _message.value = if (success) "CSV Exported Successfully" else "Export Failed"
            _isBusy.value = false
        }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val success = exportImportManager.importJson(uri)
            _message.value = if (success) "JSON Imported Successfully" else "Import Failed"
            _isBusy.value = false
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val success = exportImportManager.importCsv(uri)
            _message.value = if (success) "CSV Imported Successfully" else "Import Failed"
            _isBusy.value = false
        }
    }
}