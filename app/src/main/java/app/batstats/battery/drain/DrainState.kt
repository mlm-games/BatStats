package app.batstats.battery.drain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Tracks drain rates across different device states.
 */
@Serializable
@Parcelize
data class DrainState(
    val timestamp: Long = System.currentTimeMillis(),
    
    // Current battery level
    val batteryLevel: Int = 0,
    val batteryLevelMah: Double = 0.0,
    
    // Device state
    val isScreenOn: Boolean = false,
    val isCharging: Boolean = false,
    val isDeepSleep: Boolean = false,
    val isDozing: Boolean = false,
    
    // Cumulative drain by state (mAh)
    val screenOnDrainMah: Double = 0.0,
    val screenOffDrainMah: Double = 0.0,
    val activeDrainMah: Double = 0.0,
    val idleDrainMah: Double = 0.0,
    val deepSleepDrainMah: Double = 0.0,
    val awakeDrainMah: Double = 0.0,
    
    // Time spent in each state (ms)
    val screenOnTimeMs: Long = 0L,
    val screenOffTimeMs: Long = 0L,
    val activeTimeMs: Long = 0L,
    val idleTimeMs: Long = 0L,
    val deepSleepTimeMs: Long = 0L,
    val awakeTimeMs: Long = 0L,
    
    // Drain rates (mA) - calculated averages
    val screenOnDrainRate: Double = 0.0,
    val screenOffDrainRate: Double = 0.0,
    val activeDrainRate: Double = 0.0,
    val idleDrainRate: Double = 0.0,
    val deepSleepDrainRate: Double = 0.0,
    val awakeDrainRate: Double = 0.0,
    
    // Session tracking
    val sessionStartTime: Long = System.currentTimeMillis(),
    val lastUpdateTime: Long = System.currentTimeMillis()
) : Parcelable {
    
    val totalDrainMah: Double
        get() = screenOnDrainMah + screenOffDrainMah
    
    val totalTimeMs: Long
        get() = System.currentTimeMillis() - sessionStartTime
    
    val averageDrainRate: Double
        get() = if (totalTimeMs > 0) {
            (totalDrainMah / (totalTimeMs / 3600000.0))
        } else 0.0
    
    val screenOnPercentage: Float
        get() = if (totalTimeMs > 0) {
            (screenOnTimeMs.toFloat() / totalTimeMs * 100f)
        } else 0f
    
    val deepSleepPercentage: Float
        get() = if (screenOffTimeMs > 0) {
            (deepSleepTimeMs.toFloat() / screenOffTimeMs * 100f)
        } else 0f
}

/**
 * Snapshot of drain metrics at a point in time
 */
@Serializable
data class DrainSnapshot(
    val timestamp: Long,
    val batteryLevel: Int,
    val batteryMah: Double,
    val currentMa: Int,
    val isScreenOn: Boolean,
    val isCharging: Boolean,
    val isDeepSleep: Boolean,
    val isDozing: Boolean,
    val cpuAwakeTimeMs: Long,
    val deepSleepTimeMs: Long
)

enum class DeviceState {
    SCREEN_ON_ACTIVE,
    SCREEN_ON_IDLE,
    SCREEN_OFF_AWAKE,
    SCREEN_OFF_DOZE,
    SCREEN_OFF_DEEP_SLEEP,
    CHARGING
}

fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

fun formatDrainRate(rate: Double): String {
    return when {
        rate < 0.1 -> "< 0.1 mA"
        rate < 10 -> String.format(java.util.Locale.getDefault(), "%.1f mA", rate)
        else -> String.format(java.util.Locale.getDefault(), "%.0f mA", rate)
    }
}