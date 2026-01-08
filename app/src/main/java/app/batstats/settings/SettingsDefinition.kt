package app.batstats.settings

import io.github.mlmgames.settings.core.annotations.CategoryDefinition
import io.github.mlmgames.settings.core.annotations.NoReset
import io.github.mlmgames.settings.core.annotations.Persisted
import io.github.mlmgames.settings.core.annotations.Setting
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    // GENERAL
    @Setting(
        title = "Auto-start Monitoring",
        category = General::class,
        type = Toggle::class,
        key = "auto_start_on_boot"
    )
    val autoStartOnBoot: Boolean = true,

    @Setting(
        title = "Monitoring Interval",
        category = General::class,
        type = Dropdown::class,
        options = ["5 seconds", "10 seconds", "30 seconds", "1 minute", "5 minutes"],
        key = "monitoring_interval_index"
    )
    val monitoringIntervalIndex: Int = 2,

    @Setting(
        title = "Show Persistent Notification",
        category = General::class,
        type = Toggle::class,
        key = "show_notification"
    )
    val showNotification: Boolean = true,

    @Setting(
        title = "Show Drain Stats Notification",
        description = "Show detailed drain statistics in notification (requires Shizuku)",
        category = General::class,
        type = Toggle::class,
        key = "show_drain_notification"
    )
    val showDrainNotification: Boolean = false,

    @Setting(
        title = "Notification Style",
        category = General::class,
        type = Dropdown::class,
        options = ["Minimal", "Compact", "Detailed"],
        dependsOn = "showNotification",
        key = "notification_style_index"
    )
    val notificationStyleIndex: Int = 1,

    @Setting(
        title = "Track Foreground Apps",
        category = General::class,
        type = Toggle::class,
        key = "track_foreground_apps"
    )
    val trackForegroundApps: Boolean = true,

    // NOTIFICATIONS & ALARMS
    @Setting(
        title = "Low Battery Alert",
        category = Notifications::class,
        type = Toggle::class,
        key = "low_battery_alert_enabled"
    )
    val lowBatteryAlertEnabled: Boolean = true,

    @Setting(
        title = "Low Battery Threshold",
        category = Notifications::class,
        type = Slider::class,
        min = 5f, max = 50f, step = 5f,
        dependsOn = "lowBatteryAlertEnabled",
        key = "low_battery_threshold"
    )
    val lowBatteryThreshold: Int = 20,

    @Setting(
        title = "High Battery Alert",
        description = "Notify when charging reaches threshold",
        category = Notifications::class,
        type = Toggle::class,
        key = "high_battery_alert_enabled"
    )
    val highBatteryAlertEnabled: Boolean = false,

    @Setting(
        title = "High Battery Threshold",
        category = Notifications::class,
        type = Slider::class,
        min = 50f, max = 100f, step = 5f,
        dependsOn = "highBatteryAlertEnabled",
        key = "high_battery_threshold"
    )
    val highBatteryThreshold: Int = 80,

    @Setting(
        title = "Temperature Warning",
        category = Notifications::class,
        type = Toggle::class,
        key = "temperature_warning_enabled"
    )
    val temperatureWarningEnabled: Boolean = true,

    @Setting(
        title = "Temperature Threshold",
        description = "Warning temperature in Celsius",
        category = Notifications::class,
        type = Slider::class,
        min = 35f, max = 55f, step = 1f,
        dependsOn = "temperatureWarningEnabled",
        key = "temperature_threshold"
    )
    val temperatureThreshold: Float = 45f,

    @Setting(
        title = "High Discharge Alert",
        category = Notifications::class,
        type = Toggle::class,
        key = "discharge_alert_enabled"
    )
    val dischargeAlertEnabled: Boolean = false,

    @Setting(
        title = "Discharge Threshold",
        description = "Alert when discharge exceeds this (mA)",
        category = Notifications::class,
        type = Slider::class,
        min = 200f, max = 2000f, step = 50f,
        dependsOn = "dischargeAlertEnabled",
        key = "discharge_current_threshold"
    )
    val dischargeCurrentThreshold: Int = 600,

    @Setting(
        title = "Charging Complete Alert",
        category = Notifications::class,
        type = Toggle::class,
        key = "charging_complete_alert"
    )
    val chargingCompleteAlert: Boolean = true,

    @Setting(
        title = "Alert Sound",
        category = Notifications::class,
        type = Toggle::class,
        key = "alert_sound_enabled"
    )
    val alertSoundEnabled: Boolean = true,

    @Setting(
        title = "Alert Vibration",
        category = Notifications::class,
        type = Toggle::class,
        key = "alert_vibration_enabled"
    )
    val alertVibrationEnabled: Boolean = true,

    // DISPLAY
    @Setting(
        title = "Theme",
        category = Display::class,
        type = Dropdown::class,
        options = ["System Default", "Light", "Dark"],
        key = "theme_index"
    )
    val themeIndex: Int = 0,

    @Setting(
        title = "Dynamic Colors",
        category = Display::class,
        type = Toggle::class,
        key = "dynamic_colors"
    )
    val dynamicColors: Boolean = true,

    @Setting(
        title = "Chart Time Range",
        category = Display::class,
        type = Dropdown::class,
        options = ["15 minutes", "1 hour", "6 hours", "24 hours", "7 days"],
        key = "chart_time_range_index"
    )
    val chartTimeRangeIndex: Int = 1,

    @Setting(
        title = "Show Current in mA",
        category = Display::class,
        type = Toggle::class,
        key = "show_current_in_ma"
    )
    val showCurrentInMa: Boolean = true,

    @Setting(
        title = "Temperature Unit",
        category = Display::class,
        type = Dropdown::class,
        options = ["Celsius", "Fahrenheit"],
        key = "temperature_unit_index"
    )
    val temperatureUnitIndex: Int = 0,

    @Setting(
        title = "Compact Stats View",
        category = Display::class,
        type = Toggle::class,
        key = "compact_stats_view"
    )
    val compactStatsView: Boolean = false,

    // DATA
    @Setting(
        title = "Data Retention",
        category = Data::class,
        type = Dropdown::class,
        options = ["1 week", "1 month", "3 months", "6 months", "1 year", "Forever"],
        key = "data_retention_index"
    )
    val dataRetentionIndex: Int = 2,

    @Setting(
        title = "Auto-cleanup Old Data",
        category = Data::class,
        type = Toggle::class,
        key = "auto_cleanup_enabled"
    )
    val autoCleanupEnabled: Boolean = true,

    @Setting(
        title = "Export Format",
        category = Data::class,
        type = Dropdown::class,
        options = ["CSV", "JSON"],
        key = "export_format_index"
    )
    val exportFormatIndex: Int = 0,

    @Setting(
        title = "Include Raw Samples",
        category = Data::class,
        type = Toggle::class,
        key = "export_include_raw_samples"
    )
    val exportIncludeRawSamples: Boolean = false,

    // PERSISTED STATE
    @Persisted(key = "last_data_cleanup") val lastDataCleanup: Long = 0L,
    @Persisted(key = "last_export_time") val lastExportTime: Long = 0L,
    @Persisted(key = "total_samples_collected") val totalSamplesCollected: Long = 0L,
    @Persisted(key = "first_launch_time") @NoReset val firstLaunchTime: Long = 0L,
    @Persisted(key = "has_seen_onboarding") @NoReset val hasSeenOnboarding: Boolean = false,
)

val AppSettings.monitoringIntervalMs: Long
    get() = when (monitoringIntervalIndex) {
        0 -> 5_000L; 1 -> 10_000L; 2 -> 30_000L; 3 -> 60_000L; 4 -> 300_000L; else -> 30_000L
    }

val AppSettings.chartTimeRangeMs: Long
    get() = when (chartTimeRangeIndex) {
        0 -> 15 * 60 * 1000L; 1 -> 60 * 60 * 1000L; 2 -> 6 * 60 * 60 * 1000L
        3 -> 24 * 60 * 60 * 1000L; 4 -> 7 * 24 * 60 * 60 * 1000L; else -> 60 * 60 * 1000L
    }

val AppSettings.useFahrenheit: Boolean get() = temperatureUnitIndex == 1


@CategoryDefinition(order = 0)
object General

@CategoryDefinition(order = 1)
object Notifications

@CategoryDefinition(order = 2)
object Display

@CategoryDefinition(order = 3)
object Data
