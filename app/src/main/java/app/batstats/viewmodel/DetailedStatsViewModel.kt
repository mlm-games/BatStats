package app.batstats.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.batstats.battery.BatteryGraph
import app.batstats.battery.util.BatteryStatsParser
import app.batstats.battery.util.DetailedStatsCollector
import app.batstats.battery.util.RootStatsCollector
import app.batstats.battery.shizuku.ShizukuBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailedStatsViewModel(
    private val app: Application,
    private val collector: DetailedStatsCollector,
    private val shizuku: ShizukuBridge
) : AndroidViewModel(app) {

    val snapshot: StateFlow<BatteryStatsParser.FullSnapshot?> = collector.snapshot
    val deviceIdle: StateFlow<BatteryStatsParser.DeviceIdleInfo?> = collector.deviceIdle
    val powerManager: StateFlow<BatteryStatsParser.PowerManagerInfo?> = collector.powerManager
    val lastRefresh: StateFlow<Long> = collector.lastRefresh
    val isRefreshing: StateFlow<Boolean> = collector.isRefreshing
    val error: StateFlow<String?> = collector.error

    private val _hasShizuku = MutableStateFlow(false)
    val hasShizuku: StateFlow<Boolean> = _hasShizuku.asStateFlow()

    private val _hasRoot = MutableStateFlow(false)
    val hasRoot: StateFlow<Boolean> = _hasRoot.asStateFlow()

    private val _kernelBattery = MutableStateFlow<RootStatsCollector.KernelBatteryInfo?>(null)
    val kernelBattery: StateFlow<RootStatsCollector.KernelBatteryInfo?> = _kernelBattery.asStateFlow()

    init {
        checkPermissions()
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            _hasShizuku.value = shizuku.ping() && shizuku.hasPermission()
            _hasRoot.value = RootStatsCollector.isRootAvailable()
            
            if (_hasRoot.value) {
                _kernelBattery.value = RootStatsCollector.getKernelBatteryInfo()
            }
        }
    }

    suspend fun refresh(): Boolean {
        checkPermissions()
        val result = collector.refresh()
        if (_hasRoot.value) {
            _kernelBattery.value = RootStatsCollector.getKernelBatteryInfo()
        }
        return result
    }

    suspend fun resetStats(): Boolean {
        return collector.resetStats()
    }

    fun requestShizukuPermission() {
        shizuku.requestPermission()
    }

    fun refreshRootStats() {
        viewModelScope.launch {
            if (_hasRoot.value) {
                _kernelBattery.value = RootStatsCollector.getKernelBatteryInfo()
            }
        }
    }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = BatteryGraph.app
                val shizuku = ShizukuBridge(app)
                val collector = DetailedStatsCollector(shizuku, BatteryGraph.db)
                @Suppress("UNCHECKED_CAST")
                return DetailedStatsViewModel(app, collector, shizuku) as T
            }
        }
    }
}