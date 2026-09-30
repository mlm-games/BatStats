package app.batstats.settings

import io.github.mlmgames.settings.core.annotations.CategoryDefinition
import io.github.mlmgames.settings.core.annotations.NoReset
import io.github.mlmgames.settings.core.annotations.Persisted
import io.github.mlmgames.settings.core.annotations.Setting
import io.github.mlmgames.settings.core.locale.AppLanguage
import io.github.mlmgames.settings.core.resources.SettingsTextKeys
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    // GENERAL
    @Setting(
        title = "Auto-start Monitoring",
        titleKey = BatStatsSettingsKeys.AUTO_START_MONITORING,
        category = General::class,
        type = Toggle::class,
        key = "auto_start_on_boot"
    )
    val autoStartOnBoot: Boolean = true,

    @Setting(
        title = "Monitoring Interval",
        titleKey = BatStatsSettingsKeys.MONITORING_INTERVAL,
        category = General::class,
        type = Dropdown::class,
        options = ["5 seconds", "10 seconds", "30 seconds", "1 minute", "5 minutes"],
        optionsKey = BatStatsSettingsKeys.MONITORING_INTERVAL_OPTIONS,
        key = "monitoring_interval_index"
    )
    val monitoringIntervalIndex: Int = 2,

    @Setting(
        title = "When Screen Is Off",
        titleKey = BatStatsSettingsKeys.SCREEN_OFF_SAMPLING,
        category = General::class,
        type = Dropdown::class,
        options = ["Follow interval", "Slow (5 minutes)", "Pause"],
        optionsKey = BatStatsSettingsKeys.SCREEN_OFF_SAMPLING_OPTIONS,
        key = "screen_off_sampling_index"
    )
    val screenOffSamplingIndex: Int = 0,

    @Setting(
        title = "Update Widgets",
        titleKey = BatStatsSettingsKeys.UPDATE_WIDGETS,
        category = General::class,
        type = Toggle::class,
        key = "update_widgets"
    )
    val updateWidgets: Boolean = true,

    @Setting(
        title = "Show Drain Stats Notification",
        titleKey = BatStatsSettingsKeys.SHOW_DRAIN_NOTIFICATION,
        description = "Show detailed drain statistics in notification (requires Shizuku, Root or ADB)",
        descriptionKey = BatStatsSettingsKeys.SHOW_DRAIN_NOTIFICATION_DESCRIPTION,
        category = General::class,
        type = Toggle::class,
        key = "show_drain_notification"
    )
    val showDrainNotification: Boolean = false,

    @Setting(
        title = "Track Foreground Apps",
        titleKey = BatStatsSettingsKeys.TRACK_FOREGROUND_APPS,
        category = General::class,
        type = Toggle::class,
        key = "track_foreground_apps"
    )
    val trackForegroundApps: Boolean = true,

    @Setting(
        title = "Detailed Stats Interval",
        titleKey = BatStatsSettingsKeys.DETAILED_STATS_INTERVAL,
        description = "How often to collect detailed battery stats via Shizuku/Root/ADB.",
        descriptionKey = BatStatsSettingsKeys.DETAILED_STATS_INTERVAL_DESCRIPTION,
        category = General::class,
        type = Dropdown::class, // Timepicker might be better later (but does not store in secs)
        options = ["1 minute", "5 minutes", "15 minutes", "30 minutes"],
        optionsKey = BatStatsSettingsKeys.DETAILED_STATS_INTERVAL_OPTIONS,
        key = "detailed_stats_interval_index"
    )
    val detailedStatsIntervalIndex: Int = 1,

    // NOTIFICATIONS & ALARMS
    @Setting(
        title = "Low Battery Alert",
        titleKey = BatStatsSettingsKeys.LOW_BATTERY_ALERT,
        category = Notifications::class,
        type = Toggle::class,
        key = "low_battery_alert_enabled"
    )
    val lowBatteryAlertEnabled: Boolean = true,

    @Setting(
        title = "Low Battery Threshold",
        titleKey = BatStatsSettingsKeys.LOW_BATTERY_THRESHOLD,
        category = Notifications::class,
        type = Slider::class,
        min = 5f, max = 50f, step = 5f,
        dependsOn = "lowBatteryAlertEnabled",
        key = "low_battery_threshold"
    )
    val lowBatteryThreshold: Int = 20,

    @Setting(
        title = "High Battery Alert",
        titleKey = BatStatsSettingsKeys.HIGH_BATTERY_ALERT,
        description = "Notify when charging reaches threshold",
        descriptionKey = BatStatsSettingsKeys.HIGH_BATTERY_ALERT_DESCRIPTION,
        category = Notifications::class,
        type = Toggle::class,
        key = "high_battery_alert_enabled"
    )
    val highBatteryAlertEnabled: Boolean = false,

    @Setting(
        title = "High Battery Threshold",
        titleKey = BatStatsSettingsKeys.HIGH_BATTERY_THRESHOLD,
        category = Notifications::class,
        type = Slider::class,
        min = 50f, max = 100f, step = 5f,
        dependsOn = "highBatteryAlertEnabled",
        key = "high_battery_threshold"
    )
    val highBatteryThreshold: Int = 80,

    @Setting(
        title = "Temperature Warning",
        titleKey = BatStatsSettingsKeys.TEMPERATURE_WARNING,
        category = Notifications::class,
        type = Toggle::class,
        key = "temperature_warning_enabled"
    )
    val temperatureWarningEnabled: Boolean = true,

    @Setting(
        title = "Temperature Threshold",
        titleKey = BatStatsSettingsKeys.TEMPERATURE_THRESHOLD,
        description = "Warning temperature in Celsius",
        descriptionKey = BatStatsSettingsKeys.TEMPERATURE_THRESHOLD_DESCRIPTION,
        category = Notifications::class,
        type = Slider::class,
        min = 35f, max = 55f, step = 1f,
        dependsOn = "temperatureWarningEnabled",
        key = "temperature_threshold"
    )
    val temperatureThreshold: Float = 45f,

    @Setting(
        title = "High Discharge Alert",
        titleKey = BatStatsSettingsKeys.HIGH_DISCHARGE_ALERT,
        category = Notifications::class,
        type = Toggle::class,
        key = "discharge_alert_enabled"
    )
    val dischargeAlertEnabled: Boolean = false,

    @Setting(
        title = "Discharge Threshold",
        titleKey = BatStatsSettingsKeys.DISCHARGE_THRESHOLD,
        description = "Alert when discharge exceeds this (mA)",
        descriptionKey = BatStatsSettingsKeys.DISCHARGE_THRESHOLD_DESCRIPTION,
        category = Notifications::class,
        type = Slider::class,
        min = 200f, max = 2000f, step = 50f,
        dependsOn = "dischargeAlertEnabled",
        key = "discharge_current_threshold"
    )
    val dischargeCurrentThreshold: Int = 600,

    @Setting(
        title = "Charging Complete Alert",
        titleKey = BatStatsSettingsKeys.CHARGING_COMPLETE_ALERT,
        category = Notifications::class,
        type = Toggle::class,
        key = "charging_complete_alert"
    )
    val chargingCompleteAlert: Boolean = true,

    @Setting(
        title = "Alert Sound",
        titleKey = BatStatsSettingsKeys.ALERT_SOUND,
        category = Notifications::class,
        type = Toggle::class,
        key = "alert_sound_enabled"
    )
    val alertSoundEnabled: Boolean = true,

    @Setting(
        title = "Alert Vibration",
        titleKey = BatStatsSettingsKeys.ALERT_VIBRATION,
        category = Notifications::class,
        type = Toggle::class,
        key = "alert_vibration_enabled"
    )
    val alertVibrationEnabled: Boolean = true,

    // DISPLAY
    @Setting(
        title = "Theme",
        titleKey = BatStatsSettingsKeys.THEME,
        category = Display::class,
        type = Dropdown::class,
        options = ["System Default", "Light", "Dark"],
        optionsKey = BatStatsSettingsKeys.THEME_OPTIONS,
        key = "theme_index"
    )
    val themeIndex: Int = 0,

    @Setting(
        title = "Language",
        titleKey = SettingsTextKeys.LANGUAGE,
        category = Display::class,
        type = Dropdown::class,
        key = "language",
        languages = ["en", "ar", "cs", "de", "el", "es", "fa", "fi", "fr", "he", "hr", "hu", "id", "it", "ja", "ko", "nl", "pl", "pt", "ru", "sv", "tr", "uk", "vi", "zh-CN", "zh-TW"]
    )
    val language: AppLanguage = AppLanguage.System,

    @Setting(
        title = "Dynamic Colors",
        titleKey = BatStatsSettingsKeys.DYNAMIC_COLORS,
        category = Display::class,
        type = Toggle::class,
        key = "dynamic_colors"
    )
    val dynamicColors: Boolean = false,

    @Setting(
        title = "Pure black (OLED)",
        titleKey = BatStatsSettingsKeys.PURE_BLACK_OLED,
        category = Display::class,
        type = Toggle::class,
        key = "oled_black"
    )
    val oledBlack: Boolean = false,

    @Setting(
        title = "Chart Time Range",
        titleKey = BatStatsSettingsKeys.CHART_TIME_RANGE,
        category = Display::class,
        type = Dropdown::class,
        options = ["15 minutes", "1 hour", "6 hours", "24 hours", "7 days"],
        optionsKey = BatStatsSettingsKeys.CHART_TIME_RANGE_OPTIONS,
        key = "chart_time_range_index"
    )
    val chartTimeRangeIndex: Int = 1,

    @Setting(
        title = "Show Current in mA",
        titleKey = BatStatsSettingsKeys.SHOW_CURRENT_MA,
        category = Display::class,
        type = Toggle::class,
        key = "show_current_in_ma"
    )
    val showCurrentInMa: Boolean = true,

    @Setting(
        title = "Temperature Unit",
        titleKey = BatStatsSettingsKeys.TEMPERATURE_UNIT,
        category = Display::class,
        type = Dropdown::class,
        options = ["Celsius", "Fahrenheit"],
        optionsKey = BatStatsSettingsKeys.TEMPERATURE_UNIT_OPTIONS,
        key = "temperature_unit_index"
    )
    val temperatureUnitIndex: Int = 0,

    @Setting(
        title = "Compact Stats View",
        titleKey = BatStatsSettingsKeys.COMPACT_STATS_VIEW,
        category = Display::class,
        type = Toggle::class,
        key = "compact_stats_view"
    )
    val compactStatsView: Boolean = false,

    @Setting(
        title = "Bottom Action Bar",
        titleKey = BatStatsSettingsKeys.BOTTOM_ACTION_BUTTONS,
        description = "Show the home screen action buttons in a bottom bar",
        descriptionKey = BatStatsSettingsKeys.BOTTOM_ACTION_BUTTONS_DESCRIPTION,
        category = Display::class,
        type = Toggle::class,
        key = "bottom_action_buttons"
    )
    val bottomActionButtons: Boolean = false,

    @Setting(
        title = "Show App Names",
        titleKey = BatStatsSettingsKeys.SHOW_APP_NAMES,
        description = "Show app names alongside package names in detailed statistics",
        descriptionKey = BatStatsSettingsKeys.SHOW_APP_NAMES_DESCRIPTION,
        category = Display::class,
        type = Toggle::class,
        key = "show_app_names"
    )
    val showAppNames: Boolean = true,

    // DATA
    @Setting(
        title = "Data Retention",
        titleKey = BatStatsSettingsKeys.DATA_RETENTION,
        category = Data::class,
        type = Dropdown::class,
        options = ["1 week", "1 month", "3 months", "6 months", "1 year", "Forever"],
        optionsKey = BatStatsSettingsKeys.DATA_RETENTION_OPTIONS,
        key = "data_retention_index"
    )
    val dataRetentionIndex: Int = 2,

    @Setting(
        title = "Auto-cleanup Old Data",
        titleKey = BatStatsSettingsKeys.AUTO_CLEANUP,
        category = Data::class,
        type = Toggle::class,
        key = "auto_cleanup_enabled"
    )
    val autoCleanupEnabled: Boolean = true,

    @Setting(
        title = "Export Format",
        titleKey = BatStatsSettingsKeys.EXPORT_FORMAT,
        category = Data::class,
        type = Dropdown::class,
        options = ["CSV", "JSON"],
        key = "export_format_index"
    )
    val exportFormatIndex: Int = 0,

    @Setting(
        title = "Include Raw Samples",
        titleKey = BatStatsSettingsKeys.INCLUDE_RAW_SAMPLES,
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

enum class ScreenOffMode { FOLLOW, SLOW, PAUSE }

val AppSettings.screenOffMode: ScreenOffMode
    get() = when (screenOffSamplingIndex) {
        1 -> ScreenOffMode.SLOW
        2 -> ScreenOffMode.PAUSE
        else -> ScreenOffMode.FOLLOW
    }

private const val DAY_MS = 24 * 60 * 60 * 1000L

val AppSettings.dataRetentionMs: Long?
    get() = when (dataRetentionIndex) {
        0 -> 7 * DAY_MS
        1 -> 30 * DAY_MS
        2 -> 90 * DAY_MS
        3 -> 180 * DAY_MS
        4 -> 365 * DAY_MS
        else -> null
    }

val AppSettings.detailedStatsIntervalMs: Long
    get() = when (detailedStatsIntervalIndex) {
        0 -> 60_000L; 1 -> 300_000L; 2 -> 900_000L; 3 -> 1_800_000L; else -> 300_000L
    }


@CategoryDefinition(order = 0, titleKey = BatStatsSettingsKeys.CATEGORY_GENERAL)
object General

@CategoryDefinition(order = 1, titleKey = BatStatsSettingsKeys.CATEGORY_NOTIFICATIONS)
object Notifications

@CategoryDefinition(order = 2, titleKey = BatStatsSettingsKeys.CATEGORY_DISPLAY)
object Display

@CategoryDefinition(order = 3, titleKey = BatStatsSettingsKeys.CATEGORY_DATA)
object Data
