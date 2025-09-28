package app.batstats.battery.data

import android.app.Application
import android.content.*
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.getSystemService
import app.batstats.battery.data.db.*
import app.batstats.battery.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class BatteryRepository(
    private val app: Application,
    private val db: BatteryDatabase,
    private val settingsRepo: BatterySettingsRepository,
    private val appScope: CoroutineScope
) {
    private val battery = app.getSystemService<BatteryManager>()!!
    private val power = app.getSystemService<PowerManager>()!!

    private val _realtime = MutableStateFlow(Realtime(null))
    val realtime: StateFlow<Realtime> = _realtime.asStateFlow()

    private val _isSampling = MutableStateFlow(false)
    val isSampling: StateFlow<Boolean> = _isSampling.asStateFlow()

    data class Realtime(
        val sample: BatterySample?
    ) {
        val level get() = sample?.levelPercent ?: 0
        val status get() = sample?.status ?: 0
        val plugged get() = sample?.plugged ?: 0
        val currentMa get() = ((sample?.currentNowUa ?: 0L) / 1000.0).roundToInt()
        val voltageMv get() = sample?.voltageMv ?: 0
        val temperatureC get() = (sample?.temperatureDeciC ?: 0) / 10.0
        val powerMw get() = (currentMa * voltageMv / 1000.0)
    }

    private val sessionDao = db.sessionDao()
    private val sampleDao = db.batteryDao()
    private var sampleJob: Job? = null

    fun startSampling() {
        if (sampleJob?.isActive == true) return
        sampleJob = appScope.launch(Dispatchers.Default) {
            try {
                _isSampling.value = true
                settingsRepo.flow.collectLatest { settings ->
                    while (currentCoroutineContext().isActive) {
                        val s = captureSample()
                        _realtime.value = Realtime(s)

                        // If user wants monitoring while charging only, keep UI live but avoid heavy work.
                        if (settings.monitorWhileChargingOnly && s.plugged == 0) {
                            delay(settings.sampleIntervalSec.coerceIn(5, 60) * 1000L)
                            continue
                        }

                        emitToDb(s, settings.keepHistoryDays)
                        autoManageSession(settings, s)
                        checkAlarms(settings, s)

                        val interval = settings.sampleIntervalSec.coerceIn(5, 60)
                        delay(interval * 1000L)
                    }
                }
            } finally {
                _isSampling.value = false
            }
        }
    }

    fun stopSampling() {
        sampleJob?.cancel()
        sampleJob = null
        _isSampling.value = false
    }

    private suspend fun emitToDb(s: BatterySample, keepDays: Int) {
        sampleDao.insertSample(s)
        sampleDao.purge(System.currentTimeMillis() - keepDays.daysInMs())
    }

    suspend fun startSessionIfNone(type: SessionType, now: Long = System.currentTimeMillis()) {
        if (sessionDao.active() != null) return
        val last = sampleDao.lastSample()
        val startLvl = last?.levelPercent ?: 0
        sessionDao.upsert(
            ChargeSession(
                sessionId = UUID.randomUUID().toString(),
                type = type,
                startTime = now,
                endTime = null,
                startLevel = startLvl,
                endLevel = null,
                deltaUah = null,
                avgCurrentUa = null,
                estCapacityMah = null
            )
        )
    }

    suspend fun completeActiveSession(now: Long = System.currentTimeMillis()) {
        val active = sessionDao.active() ?: return
        val endSample = sampleDao.lastSample() ?: return
        val (deltaUah, avgUa, estMah) = estimateSession(active)
        sessionDao.complete(
            id = active.sessionId,
            end = now,
            endLevel = endSample.levelPercent,
            delta = deltaUah,
            avg = avgUa,
            cap = estMah
        )
    }

    fun sessionsPaged(limit: Int, offset: Int) = sessionDao.sessionsPaged(limit, offset)

    fun samplesBetween(from: Long, to: Long) = sampleDao.samplesBetween(from, to)

    private suspend fun estimateSession(s: ChargeSession): Triple<Long?, Long?, Int?> {
        // Estimate by integrating delta chargeCounter over timestamps between start..end
        // If chargeCounter not available, fallback to avg(|currentNow|) * duration
        val end = System.currentTimeMillis()
        val data = samplesBetween(s.startTime, end).first()
        if (data.isEmpty()) return Triple(null, null, null)

        val hasChargeCounter = data.any { it.chargeCounterUah != null }
        val deltaUah = if (hasChargeCounter) {
            val first = data.firstOrNull { it.chargeCounterUah != null }?.chargeCounterUah
            val last = data.lastOrNull { it.chargeCounterUah != null }?.chargeCounterUah
            if (first != null && last != null) abs(last - first) else null
        } else null

        val avgUa = run {
            val currents = data.mapNotNull { it.currentNowUa }
            if (currents.isEmpty()) null else currents.average().roundToInt().toLong()
        }

        val durationH = (data.last().timestamp - data.first().timestamp) / 3600000.0
        val estUah = when {
            deltaUah != null -> deltaUah
            avgUa != null -> (abs(avgUa) * durationH).roundToInt().toLong()
            else -> null
        }

        val deltaPct = abs((data.last().levelPercent - data.first().levelPercent).toDouble())
        val estCapacityMah = if (estUah != null && deltaPct > 1.0) {
            // capacity ≈ (ΔQ in mAh) * (100 / Δ%)
            ((estUah / 1000.0) * (100.0 / deltaPct)).roundToInt()
        } else null

        return Triple(estUah, avgUa, estCapacityMah)
    }

    private suspend fun autoManageSession(settings: BatterySettings, s: BatterySample) {
        if (settings.autoStartSessionOnPlug && s.plugged != 0) {
            startSessionIfNone(SessionType.CHARGE)
        }
        if (settings.autoStopSessionOnUnplug && s.plugged == 0) {
            completeActiveSession()
        }
    }

    private fun captureSample(): BatterySample {
        val now = System.currentTimeMillis()
        val sticky = app.registerReceiver(null, batteryIntentFilter())
        val level = sticky?.getIntExtra("level", -1) ?: -1
        val scale = sticky?.getIntExtra("scale", -1) ?: 100
        val pct = (level * 100f / scale).toInt().coerceIn(0, 100)

        val status = sticky?.getIntExtra("status", 0) ?: 0
        val plugged = sticky?.getIntExtra("plugged", 0) ?: 0
        val health = sticky?.getIntExtra("health", 0)
        val voltage = sticky?.getIntExtra("voltage", 0) // mV
        val tempDeci = sticky?.getIntExtra("temperature", 0) // 1/10 °C

        val currentNowUa = battery.getLongPropertySafe(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val chargeCounterUah = battery.getLongPropertySafe(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        val screenOn = power.isInteractive

        return BatterySample(
            timestamp = now,
            levelPercent = pct,
            status = status,
            plugged = plugged,
            currentNowUa = currentNowUa,
            chargeCounterUah = chargeCounterUah,
            voltageMv = voltage,
            temperatureDeciC = tempDeci,
            health = health,
            screenOn = screenOn
        )
    }

    private fun BatteryManager.getLongPropertySafe(id: Int): Long? = try {
        val v = if (Build.VERSION.SDK_INT >= 21) getLongProperty(id) else Long.MIN_VALUE
        if (v == Long.MIN_VALUE || v == 0L) null else v
    } catch (_: Throwable) { null }


    private data class AlarmState(
        var limitNotified: Boolean = false,
        var tempNotified: Boolean = false,
        var dischargeNotified: Boolean = false
    )
    private val alarmState = AlarmState()

    private suspend fun checkAlarms(settings: BatterySettings, s: BatterySample) {
        // Charge limit
        if (s.plugged != 0 && s.levelPercent >= settings.chargeLimitPercent) {
            if (!alarmState.limitNotified) {
                Notifier.notifyChargeLimit(app, settings.chargeLimitPercent)
                alarmState.limitNotified = true
            }
        } else {
            alarmState.limitNotified = false
        }

        // High temp
        val temp = (s.temperatureDeciC ?: 0) / 10
        if (temp >= settings.tempHighC) {
            if (!alarmState.tempNotified) {
                Notifier.notifyTempHigh(app, temp)
                alarmState.tempNotified = true
            }
        } else {
            alarmState.tempNotified = false
        }

        // High discharge
        val ma = ((s.currentNowUa ?: 0L) / 1000).toInt()
        if (s.plugged == 0 && ma < 0 && abs(ma) >= settings.dischargeHighMa) {
            if (!alarmState.dischargeNotified) {
                Notifier.notifyDischargeHigh(app, abs(ma))
                alarmState.dischargeNotified = true
            }
        } else {
            alarmState.dischargeNotified = false
        }
    }
}


