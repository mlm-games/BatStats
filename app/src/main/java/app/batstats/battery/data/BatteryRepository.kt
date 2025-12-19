package app.batstats.battery.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.battery.data.db.BatterySample
import app.batstats.battery.data.db.ChargeSession
import app.batstats.battery.data.db.SessionType
import app.batstats.settings.AppSettings
import app.batstats.settings.chartTimeRangeMs
import app.batstats.settings.monitoringIntervalMs
import io.github.mlmgames.settings.core.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class BatteryRepository(
    private val context: Context,
    private val db: BatteryDatabase,
    private val settingsRepository: SettingsRepository<AppSettings>,
    private val scope: CoroutineScope
) {
    private val batteryDao = db.batteryDao()
    val sessionDao = db.sessionDao()
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    // Settings flows
    val monitoringInterval: Flow<Long> = settingsRepository.flow.map { it.monitoringIntervalMs }
    val showNotification: Flow<Boolean> = settingsRepository.flow.map { it.showNotification }
    val lowBatteryThreshold: Flow<Int> = settingsRepository.flow.map { it.lowBatteryThreshold }
    val highBatteryThreshold: Flow<Int> = settingsRepository.flow.map { it.highBatteryThreshold }
    val temperatureThreshold: Flow<Float> = settingsRepository.flow.map { it.temperatureThreshold }

    // Realtime state
    private val _realtime = MutableStateFlow(Realtime())
    val realtimeFlow: StateFlow<Realtime> = _realtime.asStateFlow()

    // Monitoring state
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoringFlow: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var samplingJob: Job? = null

    // Broadcast receiver for immediate system updates
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            processBatteryState(intent)
        }
    }

    // Active session
    val activeSessionFlow: Flow<ChargeSession?> = sessionDao.activeFlow()

    // Recent samples based on settings
    fun recentSamplesFlow(durationMs: Long): Flow<List<BatterySample>> {
        val since = System.currentTimeMillis() - durationMs
        return batteryDao.samplesBetween(since, Long.MAX_VALUE)
    }

    fun samplesBetween(start: Long, end: Long): Flow<List<BatterySample>> {
        return batteryDao.samplesBetween(start, end)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentSamplesFromSettings: Flow<List<BatterySample>> = settingsRepository.flow
        .flatMapLatest { settings ->
            val since = System.currentTimeMillis() - settings.chartTimeRangeMs
            batteryDao.samplesBetween(since, Long.MAX_VALUE)
        }

    suspend fun getSettings(): AppSettings = settingsRepository.flow.first()

    fun startSampling() {
        if (_isMonitoring.value) return
        _isMonitoring.value = true

        // 1. Register Receiver for system broadcasts (plug/unplug, % change)
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)

        // 2. Start Polling Coroutine for current/voltage fluctuations
        // Android's ACTION_BATTERY_CHANGED is "sticky" but doesn't fire often enough
        // to show live current changes. We poll BatteryManager properties.
        samplingJob = scope.launch {
            settingsRepository.flow.collectLatest { settings ->
                while (isActive) {
                    // Grab the sticky intent to get current voltage/temp
                    val intent = context.registerReceiver(null, filter)
                    if (intent != null) {
                        processBatteryState(intent, persist = true)
                    }
                    delay(settings.monitoringIntervalMs)
                }
            }
        }
    }

    fun stopSampling() {
        if (!_isMonitoring.value) return
        _isMonitoring.value = false

        samplingJob?.cancel()
        samplingJob = null

        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
    }

    suspend fun startSession(type: SessionType) {
        val session = ChargeSession(
            sessionId = java.util.UUID.randomUUID().toString(),
            type = type,
            startTime = System.currentTimeMillis(),
            startLevel = _realtime.value.level,
            endTime = null, endLevel = null, deltaUah = null, avgCurrentUa = null, estCapacityMah = null
        )
        sessionDao.upsert(session)
    }

    suspend fun endCurrentSession() {
        val current = sessionDao.active() ?: return
        val end = System.currentTimeMillis()
        val endLevel = _realtime.value.level

        // Calculate average current for the session if possible
        val samples = batteryDao.samplesBetween(current.startTime, end).first()
        val avgCurrent = if (samples.isNotEmpty()) {
            samples.mapNotNull { it.currentNowUa }.average().toLong()
        } else null

        sessionDao.complete(current.sessionId, end, endLevel, null, avgCurrent, null)
    }

    private fun processBatteryState(intent: Intent, persist: Boolean = false) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val levelPercent = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val pluggedState = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) // mV
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) // tenths of a degree C
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        // Get Instantaneous Current (MicroAmperes)
        // This property is not in the intent, must be queried from manager
        var currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

        // Some devices report average instead of instantaneous
        if (currentNow == 0L || currentNow == Long.MIN_VALUE) {
            currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        }

        // Calculate Power (mW) = (uA * mV) / 1,000,000
        val powerMw = (abs(currentNow) * voltage) / 1_000_000f

        val sample = BatterySample(
            timestamp = System.currentTimeMillis(),
            levelPercent = levelPercent,
            status = status,
            plugged = pluggedState,
            currentNowUa = currentNow,
            chargeCounterUah = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
            voltageMv = voltage,
            temperatureDeciC = temperature,
            health = health,
            screenOn = isScreenOn()
        )

        // Update StateFlow for UI
        _realtime.value = Realtime(
            level = levelPercent,
            plugged = pluggedState,
            currentMa = (currentNow / 1000).toInt(),
            voltageMv = voltage,
            powerMw = powerMw,
            temperatureC = temperature / 10f,
            sample = sample
        )

        // Persist to DB
        if (persist) {
            scope.launch {
                batteryDao.insertSample(sample)
                settingsRepository.update {
                    it.copy(totalSamplesCollected = it.totalSamplesCollected + 1)
                }

                // Auto-session logic could go here (e.g. if plugged != lastPlugged -> start/stop session)
            }
        }
    }

    private fun isScreenOn(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isInteractive
    }

    data class Realtime(
        val level: Int = 0,
        val plugged: Int = 0,
        val currentMa: Int = 0,
        val voltageMv: Int = 0,
        val powerMw: Float = 0f,
        val temperatureC: Float = 0f,
        val sample: BatterySample? = null
    )
}