package app.batstats.settings

/**
 * Stable keys for the localised text of every setting and category.
 *
 * The generated schema stores these strings, and
 * [batStatsStringResourceProvider] maps each one to an Android resource, so the
 * key path is the only one that survives KSP. Resource ids written directly in
 * the annotation do not: KSP hands back a class reference for `R.string.x`
 * rather than the integer the field holds, so the generated meta records 0 and
 * the English literal in [AppSettings] is shown in every locale.
 */
object BatStatsSettingsKeys {
    const val CATEGORY_GENERAL = "batstats.settings.category.general"
    const val CATEGORY_NOTIFICATIONS = "batstats.settings.category.notifications"
    const val CATEGORY_DISPLAY = "batstats.settings.category.display"
    const val CATEGORY_DATA = "batstats.settings.category.data"

    const val AUTO_START_MONITORING = "batstats.settings.auto_start_monitoring"
    const val MONITORING_INTERVAL = "batstats.settings.monitoring_interval"
    const val SCREEN_OFF_SAMPLING = "batstats.settings.screen_off_sampling"
    const val UPDATE_WIDGETS = "batstats.settings.update_widgets"
    const val SHOW_DRAIN_NOTIFICATION = "batstats.settings.show_drain_notification"
    const val SHOW_DRAIN_NOTIFICATION_DESCRIPTION =
        "batstats.settings.show_drain_notification.description"
    const val TRACK_FOREGROUND_APPS = "batstats.settings.track_foreground_apps"
    const val DETAILED_STATS_INTERVAL = "batstats.settings.detailed_stats_interval"
    const val DETAILED_STATS_INTERVAL_DESCRIPTION =
        "batstats.settings.detailed_stats_interval.description"

    const val LOW_BATTERY_ALERT = "batstats.settings.low_battery_alert"
    const val LOW_BATTERY_THRESHOLD = "batstats.settings.low_battery_threshold"
    const val HIGH_BATTERY_ALERT = "batstats.settings.high_battery_alert"
    const val HIGH_BATTERY_ALERT_DESCRIPTION =
        "batstats.settings.high_battery_alert.description"
    const val HIGH_BATTERY_THRESHOLD = "batstats.settings.high_battery_threshold"
    const val TEMPERATURE_WARNING = "batstats.settings.temperature_warning"
    const val TEMPERATURE_THRESHOLD = "batstats.settings.temperature_threshold"
    const val TEMPERATURE_THRESHOLD_DESCRIPTION =
        "batstats.settings.temperature_threshold.description"
    const val HIGH_DISCHARGE_ALERT = "batstats.settings.high_discharge_alert"
    const val DISCHARGE_THRESHOLD = "batstats.settings.discharge_threshold"
    const val DISCHARGE_THRESHOLD_DESCRIPTION =
        "batstats.settings.discharge_threshold.description"
    const val CHARGING_COMPLETE_ALERT = "batstats.settings.charging_complete_alert"
    const val ALERT_SOUND = "batstats.settings.alert_sound"
    const val ALERT_VIBRATION = "batstats.settings.alert_vibration"

    const val THEME = "batstats.settings.theme"
    const val DYNAMIC_COLORS = "batstats.settings.dynamic_colors"
    const val PURE_BLACK_OLED = "batstats.settings.pure_black_oled"
    const val CHART_TIME_RANGE = "batstats.settings.chart_time_range"
    const val SHOW_CURRENT_MA = "batstats.settings.show_current_ma"
    const val TEMPERATURE_UNIT = "batstats.settings.temperature_unit"
    const val COMPACT_STATS_VIEW = "batstats.settings.compact_stats_view"

    const val DATA_RETENTION = "batstats.settings.data_retention"
    const val AUTO_CLEANUP = "batstats.settings.auto_cleanup"
    const val EXPORT_FORMAT = "batstats.settings.export_format"
    const val INCLUDE_RAW_SAMPLES = "batstats.settings.include_raw_samples"

    const val MONITORING_INTERVAL_OPTIONS = "batstats.settings.monitoring_interval.options"
    const val SCREEN_OFF_SAMPLING_OPTIONS = "batstats.settings.screen_off_sampling.options"
    const val DETAILED_STATS_INTERVAL_OPTIONS =
        "batstats.settings.detailed_stats_interval.options"
    const val THEME_OPTIONS = "batstats.settings.theme.options"
    const val CHART_TIME_RANGE_OPTIONS = "batstats.settings.chart_time_range.options"
    const val TEMPERATURE_UNIT_OPTIONS = "batstats.settings.temperature_unit.options"
    const val DATA_RETENTION_OPTIONS = "batstats.settings.data_retention.options"
}
