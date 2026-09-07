package app.batstats.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.batstats.battery.shizuku.ShizukuBridge
import app.batstats.battery.util.DetailedStatsCollector
import app.batstats.battery.util.PrivilegeChecker
import app.batstats.battery.util.RootStatsCollector
import app.batstats.battery.util.ShellRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DetailedStatsViewModel(
    private val collector: DetailedStatsCollector,
    private val shizukuBridge: ShizukuBridge,
    private val shellRunner: ShellRunner,
    private val context: Context
) : ViewModel() {

    // Forward flows from collector
    val snapshot = collector.snapshot
    val deviceIdle = collector.deviceIdle
    val powerManager = collector.powerManager
    val lastRefresh = collector.lastRefresh
    val isRefreshing = collector.isRefreshing
    val error = collector.error

    private val _hasShizuku = MutableStateFlow(false)
    val hasShizuku: StateFlow<Boolean> = _hasShizuku.asStateFlow()

    private val _shizukuRunning = MutableStateFlow(false)
    val shizukuRunning: StateFlow<Boolean> = _shizukuRunning.asStateFlow()

    private val _shizukuDenied = MutableStateFlow(false)
    val shizukuDenied: StateFlow<Boolean> = _shizukuDenied.asStateFlow()

    private val _hasRoot = MutableStateFlow(false)
    val hasRoot: StateFlow<Boolean> = _hasRoot.asStateFlow()

    private val _hasAdb = MutableStateFlow(false)
    val hasAdb: StateFlow<Boolean> = _hasAdb.asStateFlow()

    private val _hasAdvanced = MutableStateFlow(false)
    val hasAdvanced: StateFlow<Boolean> = _hasAdvanced.asStateFlow()

    private val _advMode = MutableStateFlow<ShellRunner.Mode>(ShellRunner.Mode.NONE)
    val advMode: StateFlow<ShellRunner.Mode> = _advMode.asStateFlow()

    private val _kernelBattery = MutableStateFlow<RootStatsCollector.KernelBatteryInfo?>(null)
    val kernelBattery: StateFlow<RootStatsCollector.KernelBatteryInfo?> = _kernelBattery.asStateFlow()

    init {
        viewModelScope.launch {
            shizukuBridge.granted.collectLatest { granted ->
                if (granted && !_hasShizuku.value) {
                    shellRunner.invalidateMode()
                    refresh()
                }
                _hasShizuku.value = granted
            }
        }
        viewModelScope.launch {
            shizukuBridge.running.collectLatest { _shizukuRunning.value = it }
        }
        refresh(forceRefresh = true)
    }

    fun recheck() {
        refresh(forceRefresh = true)
    }

    fun requestShizukuPermission() {
        shizukuBridge.requestPermission()
    }

    fun clearError() = collector.clearError()

    fun refresh(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) shellRunner.invalidateMode()

            _hasAdb.value = PrivilegeChecker.hasAdvancedViaAdb(context)
            _hasShizuku.value = shizukuBridge.hasPermissionResilient()
            _shizukuRunning.value = shizukuBridge.ping()
            _shizukuDenied.value = shizukuBridge.isPermanentlyDenied()
            _hasRoot.value = RootStatsCollector.isRootAvailable()

            val mode = shellRunner.detectMode(forceRefresh)
            _advMode.value = mode
            _hasAdvanced.value = mode != ShellRunner.Mode.NONE ||
                _hasShizuku.value || _hasRoot.value || _hasAdb.value

            if (_hasAdvanced.value) {
                collector.refresh()
            }
            if (_hasRoot.value) {
                _kernelBattery.value = RootStatsCollector.getKernelBatteryInfo()
            }
        }
    }

    fun refreshRootStats() {
        viewModelScope.launch {
            if (_hasRoot.value) {
                _kernelBattery.value = RootStatsCollector.getKernelBatteryInfo()
            }
        }
    }

    suspend fun resetStats(): Boolean {
        return try {
            if (_hasAdvanced.value) collector.resetStats() else false
        } catch (_: Exception) {
            false
        }
    }
}
