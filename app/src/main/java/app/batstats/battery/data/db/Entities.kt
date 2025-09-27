package app.batstats.battery.data.db

import androidx.room.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
@Serializable
@Entity(tableName = "battery_samples",
    indices = [Index("timestamp"), Index("status")])
data class BatterySample(
    @field:PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val levelPercent: Int,                  // 0..100
    val status: Int,                        // BatteryManager status
    val plugged: Int,                       // BatteryManager EXTRA_PLUGGED
    val currentNowUa: Long?,                // microAmps (negative while discharging)
    val chargeCounterUah: Long?,            // microAh
    val voltageMv: Int?,                    // mV
    val temperatureDeciC: Int?,             // tenths of °C
    val health: Int?,                       // BatteryManager EXTRA_HEALTH
    val screenOn: Boolean
)

@Serializable
@Entity(tableName = "charge_sessions",
    indices = [Index("startTime"), Index("type")])
data class ChargeSession(
    @Contextual
    @field:PrimaryKey val sessionId: String,
    val type: SessionType,
    val startTime: Long,
    val endTime: Long?,             // null while active
    val startLevel: Int,
    val endLevel: Int?,
    val deltaUah: Long?,            // integrated charge delta
    val avgCurrentUa: Long?,        // session average
    val estCapacityMah: Int?        // estimated capacity from this session
)

enum class SessionType { CHARGE, DISCHARGE }

@Entity(tableName = "alarm_rules")
@Serializable
data class AlarmRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AlarmType,
    val enabled: Boolean,
    val threshold: Int,    // percent for CHARGE_LIMIT; °C for TEMP; mA for DISCHARGE
    val notifyOnce: Boolean = true
)

enum class AlarmType { CHARGE_LIMIT, TEMP_HIGH, DISCHARGE_HIGH }
