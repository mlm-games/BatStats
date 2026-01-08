package app.batstats.battery.drain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import app.batstats.battery.shizuku.ShizukuBridge
import app.batstats.battery.util.BatteryStatsParser
import app.batstats.settings.AppSettings
import io.github.mlmgames.settings.core.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max

/**
 * Advanced drain tracker that monitors battery drain across different device states.
 */
class AdvancedDrainTracker(
    private val context: Context,
    private val shizukuBridge: ShizukuBridge,
    private val settingsRepository: SettingsRepository<AppSettings>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    companion object {
        private const val TAG = "AdvancedDrainTracker"
        private const val POLL_INTERVAL_MS = 60_000L
        private const val DEEP_SLEEP_THRESHOLD_MS = 30_000L
    }

    private val running = AtomicBoolean(false)
    private var trackingJob: Job? = null
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private val _drainState = MutableStateFlow(DrainState())
    val drainState: StateFlow<DrainState> = _drainState.asStateFlow()
    
    private val _snapshots = MutableStateFlow<List<DrainSnapshot>>(emptyList())
    val snapshots: StateFlow<List<DrainSnapshot>> = _snapshots.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private var lastSnapshot: DrainSnapshot? = null
    private var lastScreenState: Boolean = true
    private var lastScreenChangeTime: Long = System.currentTimeMillis()
    private var estimatedCapacityMah: Double = 4000.0
    
    // Cumulative tracking
    private var cumulativeScreenOnTime: Long = 0L
    private var cumulativeScreenOffTime: Long = 0L
    private var cumulativeDeepSleepTime: Long = 0L
    private var cumulativeAwakeTime: Long = 0L
    private var cumulativeActiveTime: Long = 0L
    private var cumulativeIdleTime: Long = 0L
    
    private var cumulativeScreenOnDrain: Double = 0.0
    private var cumulativeScreenOffDrain: Double = 0.0
    private var cumulativeDeepSleepDrain: Double = 0.0
    private var cumulativeAwakeDrain: Double = 0.0
    private var cumulativeActiveDrain: Double = 0.0
    private var cumulativeIdleDrain: Double = 0.0
    
    private var sessionStartTime: Long = System.currentTimeMillis()
    
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> onScreenStateChanged(true)
                Intent.ACTION_SCREEN_OFF -> onScreenStateChanged(false)
            }
        }
    }
    
    fun isRunning(): Boolean = running.get()
    
    fun start() {
        if (!running.compareAndSet(false, true)) return
        
        Log.i(TAG, "Starting advanced drain tracking")
        _isTracking.value = true
        resetSession()
        registerReceivers()
        
        trackingJob = scope.launch {
            updateCapacityEstimate()
            
            takeSnapshot()?.let { snapshot ->
                lastSnapshot = snapshot
            }
            
            while (isActive && running.get()) {
                try {
                    takeSnapshot()?.let { currentSnapshot ->
                        processSnapshot(currentSnapshot)
                        lastSnapshot = currentSnapshot
                        _snapshots.update { (it + currentSnapshot).takeLast(1000) }
                    }
                    updateDrainState()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in tracking loop", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }
    
    fun stop() {
        if (!running.compareAndSet(true, false)) return
        
        Log.i(TAG, "Stopping advanced drain tracking")
        _isTracking.value = false
        trackingJob?.cancel()
        trackingJob = null
        unregisterReceivers()
    }
    
    fun resetSession() {
        sessionStartTime = System.currentTimeMillis()
        
        cumulativeScreenOnTime = 0L
        cumulativeScreenOffTime = 0L
        cumulativeDeepSleepTime = 0L
        cumulativeAwakeTime = 0L
        cumulativeActiveTime = 0L
        cumulativeIdleTime = 0L
        
        cumulativeScreenOnDrain = 0.0
        cumulativeScreenOffDrain = 0.0
        cumulativeDeepSleepDrain = 0.0
        cumulativeAwakeDrain = 0.0
        cumulativeActiveDrain = 0.0
        cumulativeIdleDrain = 0.0
        
        lastScreenChangeTime = System.currentTimeMillis()
        _snapshots.value = emptyList()
        _drainState.value = DrainState(sessionStartTime = sessionStartTime)
        
        Log.i(TAG, "Session reset")
    }
    
    private fun registerReceivers() {
        try {
            val screenFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(screenReceiver, screenFilter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receivers", e)
        }
    }
    
    private fun unregisterReceivers() {
        try {
            context.unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister receivers", e)
        }
    }
    
    private fun onScreenStateChanged(screenOn: Boolean) {
        val now = System.currentTimeMillis()
        val duration = now - lastScreenChangeTime
        
        if (lastScreenState) {
            cumulativeScreenOnTime += duration
        } else {
            cumulativeScreenOffTime += duration
        }
        
        lastScreenState = screenOn
        lastScreenChangeTime = now
        
        scope.launch {
            takeSnapshot()?.let { snapshot ->
                processSnapshot(snapshot)
                lastSnapshot = snapshot
                updateDrainState()
            }
        }
    }
    
    private suspend fun takeSnapshot(): DrainSnapshot? {
        return try {
            val level = getBatteryLevel()
            val mah = getCurrentBatteryMah()
            val currentMa = getCurrentNowMa()
            val isScreenOn = powerManager.isInteractive
            val isCharging = isCharging()
            val isDozing =
                powerManager.isDeviceIdleMode

            val (cpuAwakeTime, deepSleepTime) = getDeepSleepInfo()
            val isDeepSleep = !isScreenOn && !isDozing && isInDeepSleep()
            
            DrainSnapshot(
                timestamp = System.currentTimeMillis(),
                batteryLevel = level,
                batteryMah = mah,
                currentMa = currentMa,
                isScreenOn = isScreenOn,
                isCharging = isCharging,
                isDeepSleep = isDeepSleep,
                isDozing = isDozing,
                cpuAwakeTimeMs = cpuAwakeTime,
                deepSleepTimeMs = deepSleepTime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take snapshot", e)
            null
        }
    }
    
    private fun processSnapshot(current: DrainSnapshot) {
        val previous = lastSnapshot ?: return
        if (current.isCharging || previous.isCharging) return
        
        val timeDelta = current.timestamp - previous.timestamp
        if (timeDelta <= 0) return
        
        val drainMah = max(0.0, previous.batteryMah - current.batteryMah)
        
        when {
            current.isScreenOn -> {
                cumulativeScreenOnDrain += drainMah
                if (abs(current.currentMa) > 200) {
                    cumulativeActiveDrain += drainMah
                    cumulativeActiveTime += timeDelta
                } else {
                    cumulativeIdleDrain += drainMah
                    cumulativeIdleTime += timeDelta
                }
            }
            current.isDeepSleep -> {
                cumulativeDeepSleepDrain += drainMah
                cumulativeDeepSleepTime += timeDelta
                cumulativeScreenOffDrain += drainMah
            }
            else -> {
                cumulativeAwakeDrain += drainMah
                cumulativeAwakeTime += timeDelta
                cumulativeScreenOffDrain += drainMah
            }
        }
    }
    
    private fun updateDrainState() {
        val now = System.currentTimeMillis()
        
        fun calculateRate(drainMah: Double, timeMs: Long): Double {
            return if (timeMs > 0) drainMah / (timeMs / 3600000.0) else 0.0
        }
        
        _drainState.value = DrainState(
            timestamp = now,
            batteryLevel = getBatteryLevel(),
            batteryLevelMah = getCurrentBatteryMah(),
            isScreenOn = powerManager.isInteractive,
            isCharging = isCharging(),
            isDeepSleep = isInDeepSleep(),
            isDozing = powerManager.isDeviceIdleMode,
            
            screenOnDrainMah = cumulativeScreenOnDrain,
            screenOffDrainMah = cumulativeScreenOffDrain,
            activeDrainMah = cumulativeActiveDrain,
            idleDrainMah = cumulativeIdleDrain,
            deepSleepDrainMah = cumulativeDeepSleepDrain,
            awakeDrainMah = cumulativeAwakeDrain,
            
            screenOnTimeMs = cumulativeScreenOnTime,
            screenOffTimeMs = cumulativeScreenOffTime,
            activeTimeMs = cumulativeActiveTime,
            idleTimeMs = cumulativeIdleTime,
            deepSleepTimeMs = cumulativeDeepSleepTime,
            awakeTimeMs = cumulativeAwakeTime,
            
            screenOnDrainRate = calculateRate(cumulativeScreenOnDrain, cumulativeScreenOnTime),
            screenOffDrainRate = calculateRate(cumulativeScreenOffDrain, cumulativeScreenOffTime),
            activeDrainRate = calculateRate(cumulativeActiveDrain, cumulativeActiveTime),
            idleDrainRate = calculateRate(cumulativeIdleDrain, cumulativeIdleTime),
            deepSleepDrainRate = calculateRate(cumulativeDeepSleepDrain, cumulativeDeepSleepTime),
            awakeDrainRate = calculateRate(cumulativeAwakeDrain, cumulativeAwakeTime),
            
            sessionStartTime = sessionStartTime,
            lastUpdateTime = now
        )
    }
    
    private fun getBatteryLevel(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    private fun getCurrentBatteryMah(): Double {
        val chargeCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        return if (chargeCounter > 0) {
            chargeCounter / 1000.0
        } else {
            (getBatteryLevel() / 100.0) * estimatedCapacityMah
        }
    }
    
    private fun getCurrentNowMa(): Int {
        var current = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (current == 0L || current == Long.MIN_VALUE) {
            current = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        }
        return (current / 1000).toInt()
    }
    
    private fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return plugged != 0
    }
    
    private fun isInDeepSleep(): Boolean {
        if (powerManager.isInteractive) return false
        val uptime = SystemClock.uptimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()
        val sleepTime = elapsedRealtime - uptime
        return sleepTime > DEEP_SLEEP_THRESHOLD_MS
    }
    
    private suspend fun getDeepSleepInfo(): Pair<Long, Long> {
        if (shizukuBridge.ping() && shizukuBridge.hasPermission()) {
            try {
                when (val result = shizukuBridge.run("dumpsys batterystats --checkin")) {
                    is ShizukuBridge.RunResult.Success -> {
                        val snapshot = BatteryStatsParser.parseCheckin(result.output)
                        val awakeTime = snapshot.batteryRealtimeMs - (snapshot.doze?.deepIdleTimeMs ?: 0L)
                        val sleepTime = snapshot.doze?.deepIdleTimeMs ?: 0L
                        return Pair(awakeTime, sleepTime)
                    }
                    is ShizukuBridge.RunResult.Error -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting deep sleep info", e)
            }
        }
        
        val uptime = SystemClock.uptimeMillis()
        val elapsedRealtime = SystemClock.elapsedRealtime()
        return Pair(uptime, elapsedRealtime - uptime)
    }
    
    private suspend fun updateCapacityEstimate() {
        if (shizukuBridge.ping() && shizukuBridge.hasPermission()) {
            try {
                when (val result = shizukuBridge.run("dumpsys batterystats --checkin")) {
                    is ShizukuBridge.RunResult.Success -> {
                        val snapshot = BatteryStatsParser.parseCheckin(result.output)
                        if (snapshot.estimatedCapacityMah > 0) {
                            estimatedCapacityMah = snapshot.estimatedCapacityMah.toDouble()
                        }
                    }
                    is ShizukuBridge.RunResult.Error -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating capacity", e)
            }
        }
    }
}