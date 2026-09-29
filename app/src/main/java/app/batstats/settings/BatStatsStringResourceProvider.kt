package app.batstats.settings

import android.content.Context
import app.batstats.R
import io.github.mlmgames.settings.core.resources.AndroidStringResourceProvider
import io.github.mlmgames.settings.core.resources.StringResourceProvider

/**
 * Maps [BatStatsSettingsKeys] to Android resources.
 *
 * `AndroidStringResourceProvider` resolves a key through `keyResolver` before
 * falling back to its own built-in key set, so one lookup table serves both the
 * `getString` and `getStringArray` paths and the generated schema never has to
 * carry a resource id.
 */
private val settingsResources: Map<String, Int> = mapOf(
    BatStatsSettingsKeys.CATEGORY_GENERAL to R.string.category_general,
    BatStatsSettingsKeys.CATEGORY_NOTIFICATIONS to R.string.category_notifications,
    BatStatsSettingsKeys.CATEGORY_DISPLAY to R.string.category_display,
    BatStatsSettingsKeys.CATEGORY_DATA to R.string.category_data,

    BatStatsSettingsKeys.AUTO_START_MONITORING to R.string.auto_start_monitoring,
    BatStatsSettingsKeys.MONITORING_INTERVAL to R.string.monitoring_interval,
    BatStatsSettingsKeys.SCREEN_OFF_SAMPLING to R.string.screen_off_sampling,
    BatStatsSettingsKeys.UPDATE_WIDGETS to R.string.update_widgets,
    BatStatsSettingsKeys.SHOW_DRAIN_NOTIFICATION to R.string.show_drain_notification,
    BatStatsSettingsKeys.SHOW_DRAIN_NOTIFICATION_DESCRIPTION to
        R.string.show_drain_notification_desc,
    BatStatsSettingsKeys.TRACK_FOREGROUND_APPS to R.string.track_foreground_apps,
    BatStatsSettingsKeys.DETAILED_STATS_INTERVAL to R.string.detailed_stats_interval,
    BatStatsSettingsKeys.DETAILED_STATS_INTERVAL_DESCRIPTION to
        R.string.detailed_stats_interval_desc,

    BatStatsSettingsKeys.LOW_BATTERY_ALERT to R.string.low_battery_alert,
    BatStatsSettingsKeys.LOW_BATTERY_THRESHOLD to R.string.low_battery_threshold,
    BatStatsSettingsKeys.HIGH_BATTERY_ALERT to R.string.high_battery_alert,
    BatStatsSettingsKeys.HIGH_BATTERY_ALERT_DESCRIPTION to R.string.high_battery_alert_desc,
    BatStatsSettingsKeys.HIGH_BATTERY_THRESHOLD to R.string.high_battery_threshold,
    BatStatsSettingsKeys.TEMPERATURE_WARNING to R.string.temperature_warning,
    BatStatsSettingsKeys.TEMPERATURE_THRESHOLD to R.string.temperature_threshold,
    BatStatsSettingsKeys.TEMPERATURE_THRESHOLD_DESCRIPTION to
        R.string.temperature_threshold_desc,
    BatStatsSettingsKeys.HIGH_DISCHARGE_ALERT to R.string.high_discharge_alert,
    BatStatsSettingsKeys.DISCHARGE_THRESHOLD to R.string.discharge_threshold,
    BatStatsSettingsKeys.DISCHARGE_THRESHOLD_DESCRIPTION to R.string.discharge_threshold_desc,
    BatStatsSettingsKeys.CHARGING_COMPLETE_ALERT to R.string.charging_complete_alert,
    BatStatsSettingsKeys.ALERT_SOUND to R.string.alert_sound,
    BatStatsSettingsKeys.ALERT_VIBRATION to R.string.alert_vibration,

    BatStatsSettingsKeys.THEME to R.string.theme,
    BatStatsSettingsKeys.DYNAMIC_COLORS to R.string.dynamic_colors,
    BatStatsSettingsKeys.PURE_BLACK_OLED to R.string.pure_black_oled,
    BatStatsSettingsKeys.CHART_TIME_RANGE to R.string.chart_time_range,
    BatStatsSettingsKeys.SHOW_CURRENT_MA to R.string.show_current_ma,
    BatStatsSettingsKeys.TEMPERATURE_UNIT to R.string.temperature_unit,
    BatStatsSettingsKeys.COMPACT_STATS_VIEW to R.string.compact_stats_view,

    BatStatsSettingsKeys.DATA_RETENTION to R.string.data_retention,
    BatStatsSettingsKeys.AUTO_CLEANUP to R.string.auto_cleanup,
    BatStatsSettingsKeys.EXPORT_FORMAT to R.string.export_format,
    BatStatsSettingsKeys.INCLUDE_RAW_SAMPLES to R.string.include_raw_samples,

    BatStatsSettingsKeys.MONITORING_INTERVAL_OPTIONS to R.array.monitoring_interval_options,
    BatStatsSettingsKeys.SCREEN_OFF_SAMPLING_OPTIONS to R.array.screen_off_sampling_options,
    BatStatsSettingsKeys.DETAILED_STATS_INTERVAL_OPTIONS to
        R.array.detailed_stats_interval_options,
    BatStatsSettingsKeys.THEME_OPTIONS to R.array.theme_options,
    BatStatsSettingsKeys.CHART_TIME_RANGE_OPTIONS to R.array.chart_time_range_options,
    BatStatsSettingsKeys.TEMPERATURE_UNIT_OPTIONS to R.array.temperature_unit_options,
    BatStatsSettingsKeys.DATA_RETENTION_OPTIONS to R.array.data_retention_options,
)

fun batStatsStringResourceProvider(context: Context): StringResourceProvider =
    AndroidStringResourceProvider(context) { key -> settingsResources[key] ?: 0 }
