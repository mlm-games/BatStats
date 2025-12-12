package app.batstats.battery.util

import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.battery.shizuku.ShizukuBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Collects comprehensive battery stats using Shizuku.
 * Provides parsed data for the detailed stats screen.
 */
class DetailedStatsCollector(
    private val shizuku: ShizukuBridge,
    private val db: BatteryDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshing = AtomicBoolean(false)

    private val _snapshot = MutableStateFlow<BatteryStatsParser.FullSnapshot?>(null)
    val snapshot: StateFlow<BatteryStatsParser.FullSnapshot?> = _snapshot.asStateFlow()

    private val _deviceIdle = MutableStateFlow<BatteryStatsParser.DeviceIdleInfo?>(null)
    val deviceIdle: StateFlow<BatteryStatsParser.DeviceIdleInfo?> = _deviceIdle.asStateFlow()

    private val _powerManager = MutableStateFlow<BatteryStatsParser.PowerManagerInfo?>(null)
    val powerManager: StateFlow<BatteryStatsParser.PowerManagerInfo?> = _powerManager.asStateFlow()

    private val _lastRefresh = MutableStateFlow(0L)
    val lastRefresh: StateFlow<Long> = _lastRefresh.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun refresh(): Boolean {
        if (!refreshing.compareAndSet(false, true)) return false
        _isRefreshing.value = true
        _error.value = null

        return try {
            if (!shizuku.ping()) {
                _error.value = "Shizuku not running"
                return false
            }
            if (!shizuku.hasPermission()) {
                _error.value = "Shizuku permission not granted"
                return false
            }

            // Fetch battery stats
            val statsRaw = shizuku.run("dumpsys batterystats --checkin")
            if (statsRaw != null && statsRaw.isNotBlank() && !statsRaw.startsWith("ERROR")) {
                _snapshot.value = BatteryStatsParser.parseCheckin(statsRaw)
            }

            // Device idle info
            val idleRaw = shizuku.run("dumpsys deviceidle")
            if (idleRaw != null && idleRaw.isNotBlank()) {
                _deviceIdle.value = BatteryStatsParser.parseDeviceIdle(idleRaw)
            }

            // Power manager info
            val powerRaw = shizuku.run("dumpsys power")
            if (powerRaw != null && powerRaw.isNotBlank()) {
                _powerManager.value = BatteryStatsParser.parsePowerManager(powerRaw)
            }

            _lastRefresh.value = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error"
            false
        } finally {
            _isRefreshing.value = false
            refreshing.set(false)
        }
    }

    suspend fun resetStats(): Boolean {
        if (!shizuku.ping() || !shizuku.hasPermission()) return false
        val result = shizuku.run("dumpsys batterystats --reset")
        return result?.contains("Battery stats reset") == true || result?.isBlank() == true
    }

    fun startAutoRefresh(intervalMs: Long = 60_000L): Job {
        return scope.launch {
            while (isActive) {
                refresh()
                delay(intervalMs)
            }
        }
    }
}