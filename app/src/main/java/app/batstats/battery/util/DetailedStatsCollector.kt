package app.batstats.battery.util

import android.util.Log
import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.battery.shizuku.ShizukuBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Collects comprehensive battery stats using Shizuku.
 * Provides parsed data for the detailed stats screen.
 */
class DetailedStatsCollector(
    private val shizuku: ShizukuBridge,
    private val db: BatteryDatabase
) {
    companion object {
        private const val TAG = "DetailedStatsCollector"
    }

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
        if (!refreshing.compareAndSet(false, true)) {
            Log.d(TAG, "Refresh already in progress")
            return false
        }

        _isRefreshing.value = true
        _error.value = null
        Log.d(TAG, "Starting refresh...")

        return try {
            if (!shizuku.ping()) {
                _error.value = "Shizuku not running. Please start Shizuku app."
                Log.e(TAG, "Shizuku not running")
                return false
            }

            if (!shizuku.hasPermission()) {
                _error.value = "Shizuku permission not granted. Please grant permission."
                Log.e(TAG, "Shizuku permission not granted")
                return false
            }

            var hasData = false

            // Fetch battery stats
            Log.d(TAG, "Fetching batterystats...")
            when (val statsResult = shizuku.run("dumpsys batterystats --checkin")) {
                is ShizukuBridge.RunResult.Success -> {
                    val statsRaw = statsResult.output
                    if (statsRaw.isNotBlank() && !statsRaw.startsWith("ERROR")) {
                        Log.d(TAG, "Parsing batterystats (${statsRaw.length} chars)...")
                        val parsed = BatteryStatsParser.parseCheckin(statsRaw)
                        _snapshot.value = parsed
                        hasData = true
                        Log.d(TAG, "Parsed ${parsed.apps.size} apps, ${parsed.wakelocks.size} wakelocks")
                    } else {
                        Log.w(TAG, "Empty or error batterystats output: ${statsRaw.take(100)}")
                    }
                }
                is ShizukuBridge.RunResult.Error -> {
                    Log.e(TAG, "batterystats command failed: ${statsResult.message}")
                    _error.value = "Failed to get battery stats: ${statsResult.message}"
                }
            }

            // Device idle info
            Log.d(TAG, "Fetching deviceidle...")
            when (val idleResult = shizuku.run("dumpsys deviceidle")) {
                is ShizukuBridge.RunResult.Success -> {
                    val idleRaw = idleResult.output
                    if (idleRaw.isNotBlank()) {
                        _deviceIdle.value = BatteryStatsParser.parseDeviceIdle(idleRaw)
                        hasData = true
                    }
                }
                is ShizukuBridge.RunResult.Error -> {
                    Log.w(TAG, "deviceidle command failed: ${idleResult.message}")
                }
            }

            // Power manager info
            Log.d(TAG, "Fetching power manager...")
            when (val powerResult = shizuku.run("dumpsys power")) {
                is ShizukuBridge.RunResult.Success -> {
                    val powerRaw = powerResult.output
                    if (powerRaw.isNotBlank()) {
                        _powerManager.value = BatteryStatsParser.parsePowerManager(powerRaw)
                        hasData = true
                    }
                }
                is ShizukuBridge.RunResult.Error -> {
                    Log.w(TAG, "power command failed: ${powerResult.message}")
                }
            }

            if (hasData) {
                _lastRefresh.value = System.currentTimeMillis()
                Log.d(TAG, "Refresh completed successfully")
            } else if (_error.value == null) {
                _error.value = "No data received from any command"
            }

            hasData
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed with exception", e)
            _error.value = "Error: ${e.message}"
            false
        } finally {
            _isRefreshing.value = false
            refreshing.set(false)
        }
    }

    suspend fun resetStats(): Boolean {
        if (!shizuku.ping() || !shizuku.hasPermission()) return false
        return when (val result = shizuku.run("dumpsys batterystats --reset")) {
            is ShizukuBridge.RunResult.Success -> {
                result.output.contains("Battery stats reset") || result.output.isBlank()
            }
            is ShizukuBridge.RunResult.Error -> false
        }
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