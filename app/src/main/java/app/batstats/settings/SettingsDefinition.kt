package app.batstats.settings

import app.batstats.R
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
        titleRes = R.string.auto_start_monitoring,
        category = General::class,
        type = Toggle::class,
        key = "auto_start_on_boot"
    )
    val autoStartOnBoot: Boolean = true,

    @Setting(
        title = "Monitoring Interval",
        titleRes = R.string.monitoring_interval,
        category = General::class,
        type = Dropdown::class,
        options = ["5 seconds", "10 seconds", "30 seconds", "1 minute", "5 minutes"],
        optionsRes = R.array.monitoring_interval_options,
        key = "monitoring_interval_index"
    )
    val monitoringIntervalIndex: Int = 2,

    @Setting(
        title = "Show Persistent Notification",
        titleRes = R.string.show_persistent_notification,
        category = General::class,
        type = Toggle::class,
        key = "show_notification"
    )
    val showNotification: Boolean = true,

    @Setting(
        title = "Show Drain Stats Notification",
        titleRes = R.string.show_drain_notification,
        description = "Show detailed drain statistics in notification (requires Shizuku, Root or ADB)",
        descriptionRes = R.string.show_drain_notification_desc,
        category = General::class,
        type = Toggle::class,
        key = "show_drain_notification"
    )
    val showDrainNotification: Boolean = false,

    @Setting(
        title = "Notification Style",
        titleRes = R.string.notification_style,
        category = General::class,
        type = Dropdown::class,
        options = ["Minimal", "Compact", "Detailed"],
        optionsRes = R.array.notification_style_options,
        dependsOn = "showNotification",
        key = "notification_style_index"
    )
    val notificationStyleIndex: Int = 1,

    @Setting(
        title = "Track Foreground Apps",
        titleRes = R.string.track_foreground_apps,
        category = General::class,
        type = Toggle::class,
        key = "track_foreground_apps"
    )
    val trackForegroundApps: Boolean = true,

    @Setting(
        title = "Detailed Stats Interval",
        titleRes = R.string.detailed_stats_interval,
        description = "How often to collect detailed battery stats via Shizuku/Root/ADB.",
        descriptionRes = R.string.detailed_stats_interval_desc,
        category = General::class,
        type = Dropdown::class, // Timepicker might be better later (but does not store in secs)
        options = ["1 minute", "5 minutes", "15 minutes", "30 minutes"],
        optionsRes = R.array.detailed_stats_interval_options,
        key = "detailed_stats_interval_index"
    )
    val detailedStatsIntervalIndex: Int = 1,

    // NOTIFICATIONS & ALARMS
    @Setting(
        title = "Low Battery Alert",
        titleRes = R.string.low_battery_alert,
        category = Notifications::class,
        type = Toggle::class,
        key = "low_battery_alert_enabled"
    )
    val lowBatteryAlertEnabled: Boolean = true,

    @Setting(
        title = "Low Battery Threshold",
        titleRes = R.string.low_battery_threshold,
        category = Notifications::class,
        type = Slider::class,
        min = 5f, max = 50f, step = 5f,
        dependsOn = "lowBatteryAlertEnabled",
        key = "low_battery_threshold"
    )
    val lowBatteryThreshold: Int = 20,

    @Setting(
        title = "High Battery Alert",
        titleRes = R.string.high_battery_alert,
        description = "Notify when charging reaches threshold",
        descriptionRes = R.string.high_battery_alert_desc,
        category = Notifications::class,
        type = Toggle::class,
        key = "high_battery_alert_enabled"
    )
    val highBatteryAlertEnabled: Boolean = false,

    @Setting(
        title = "High Battery Threshold",
        titleRes = R.string.high_battery_threshold,
        category = Notifications::class,
        type = Slider::class,
        min = 50f, max = 100f, step = 5f,
        dependsOn = "highBatteryAlertEnabled",
        key = "high_battery_threshold"
    )
    val highBatteryThreshold: Int = 80,

    @Setting(
        title = "Temperature Warning",
        titleRes = R.string.temperature_warning,
        category = Notifications::class,
        type = Toggle::class,
        key = "temperature_warning_enabled"
    )
    val temperatureWarningEnabled: Boolean = true,

    @Setting(
        title = "Temperature Threshold",
        titleRes = R.string.temperature_threshold,
        description = "Warning temperature in Celsius",
        descriptionRes = R.string.temperature_threshold_desc,
        category = Notifications::class,
        type = Slider::class,
        min = 35f, max = 55f, step = 1f,
        dependsOn = "temperatureWarningEnabled",
        key = "temperature_threshold"
    )
    val temperatureThreshold: Float = 45f,

    @Setting(
        title = "High Discharge Alert",
        titleRes = R.string.high_discharge_alert,
        category = Notifications::class,
        type = Toggle::class,
        key = "discharge_alert_enabled"
    )
    val dischargeAlertEnabled: Boolean = false,

    @Setting(
        title = "Discharge Threshold",
        titleRes = R.string.discharge_threshold,
        description = "Alert when discharge exceeds this (mA)",
        descriptionRes = R.string.discharge_threshold_desc,
        category = Notifications::class,
        type = Slider::class,
        min = 200f, max = 2000f, step = 50f,
        dependsOn = "dischargeAlertEnabled",
        key = "discharge_current_threshold"
    )
    val dischargeCurrentThreshold: Int = 600,

    @Setting(
        title = "Charging Complete Alert",
        titleRes = R.string.charging_complete_alert,
        category = Notifications::class,
        type = Toggle::class,
        key = "charging_complete_alert"
    )
    val chargingCompleteAlert: Boolean = true,

    @Setting(
        title = "Alert Sound",
        titleRes = R.string.alert_sound,
        category = Notifications::class,
        type = Toggle::class,
        key = "alert_sound_enabled"
    )
    val alertSoundEnabled: Boolean = true,

    @Setting(
        title = "Alert Vibration",
        titleRes = R.string.alert_vibration,
        category = Notifications::class,
        type = Toggle::class,
        key = "alert_vibration_enabled"
    )
    val alertVibrationEnabled: Boolean = true,

    // DISPLAY
    @Setting(
        title = "Theme",
        titleRes = R.string.theme,
        category = Display::class,
        type = Dropdown::class,
        options = ["System Default", "Light", "Dark"],
        optionsRes = R.array.theme_options,
        key = "theme_index"
    )
    val themeIndex: Int = 0,

    @Setting(
        title = "Dynamic Colors",
        titleRes = R.string.dynamic_colors,
        category = Display::class,
        type = Toggle::class,
        key = "dynamic_colors"
    )
    val dynamicColors: Boolean = false,

    @Setting(
        title = "Pure black (OLED)",
        titleRes = R.string.pure_black_oled,
        category = Display::class,
        type = Toggle::class,
        key = "oled_black"
    )
    val oledBlack: Boolean = false,

    @Setting(
        title = "Chart Time Range",
        titleRes = R.string.chart_time_range,
        category = Display::class,
        type = Dropdown::class,
        options = ["15 minutes", "1 hour", "6 hours", "24 hours", "7 days"],
        optionsRes = R.array.chart_time_range_options,
        key = "chart_time_range_index"
    )
    val chartTimeRangeIndex: Int = 1,

    @Setting(
        title = "Show Current in mA",
        titleRes = R.string.show_current_ma,
        category = Display::class,
        type = Toggle::class,
        key = "show_current_in_ma"
    )
    val showCurrentInMa: Boolean = true,

    @Setting(
        title = "Temperature Unit",
        titleRes = R.string.temperature_unit,
        category = Display::class,
        type = Dropdown::class,
        options = ["Celsius", "Fahrenheit"],
        optionsRes = R.array.temperature_unit_options,
        key = "temperature_unit_index"
    )
    val temperatureUnitIndex: Int = 0,

    @Setting(
        title = "Compact Stats View",
        titleRes = R.string.compact_stats_view,
        category = Display::class,
        type = Toggle::class,
        key = "compact_stats_view"
    )
    val compactStatsView: Boolean = false,

    // DATA
    @Setting(
        title = "Data Retention",
        titleRes = R.string.data_retention,
        category = Data::class,
        type = Dropdown::class,
        options = ["1 week", "1 month", "3 months", "6 months", "1 year", "Forever"],
        optionsRes = R.array.data_retention_options,
        key = "data_retention_index"
    )
    val dataRetentionIndex: Int = 2,

    @Setting(
        title = "Auto-cleanup Old Data",
        titleRes = R.string.auto_cleanup,
        category = Data::class,
        type = Toggle::class,
        key = "auto_cleanup_enabled"
    )
    val autoCleanupEnabled: Boolean = true,

    @Setting(
        title = "Export Format",
        titleRes = R.string.export_format,
        category = Data::class,
        type = Dropdown::class,
        options = ["CSV", "JSON"],
        key = "export_format_index"
    )
    val exportFormatIndex: Int = 0,

    @Setting(
        title = "Include Raw Samples",
        titleRes = R.string.include_raw_samples,
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

val AppSettings.detailedStatsIntervalMs: Long
    get() = when (detailedStatsIntervalIndex) {
        0 -> 60_000L; 1 -> 300_000L; 2 -> 900_000L; 3 -> 1_800_000L; else -> 300_000L
    }


@CategoryDefinition(order = 0, titleRes = R.string.category_general)
object General

@CategoryDefinition(order = 1, titleRes = R.string.category_notifications)
object Notifications

@CategoryDefinition(order = 2, titleRes = R.string.category_display)
object Display

@CategoryDefinition(order = 3, titleRes = R.string.category_data)
object Data
