package app.batstats.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*

private val Context.ds by preferencesDataStore("batstats.settings")

sealed class SettingDefinition<T> {
    abstract val key: Preferences.Key<T>
    abstract val getValue: (AppSettings) -> T
    abstract val propertyName: String

    data class BooleanSetting(
        override val propertyName: String,
        override val key: Preferences.Key<Boolean>,
        override val getValue: (AppSettings) -> Boolean
    ) : SettingDefinition<Boolean>()

    data class IntSetting(
        override val propertyName: String,
        override val key: Preferences.Key<Int>,
        override val getValue: (AppSettings) -> Int
    ) : SettingDefinition<Int>()
}

class SettingsRepository(private val context: Context) {

    companion object {
        // General
        val DEFAULT_SORT = intPreferencesKey("default_sort")
        val THEME_MODE = intPreferencesKey("theme_mode")

        // IR
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    private val definitions: Map<String, SettingDefinition<*>> = mapOf(
        "defaultSort" to SettingDefinition.IntSetting("defaultSort", DEFAULT_SORT) { it.defaultSort },
        "themeMode" to SettingDefinition.IntSetting("themeMode", THEME_MODE) { it.themeMode },
        "hapticFeedback" to SettingDefinition.BooleanSetting("hapticFeedback", HAPTIC_FEEDBACK) { it.hapticFeedback },
        "keepScreenOn" to SettingDefinition.BooleanSetting("keepScreenOn", KEEP_SCREEN_ON) { it.keepScreenOn },
    )

    val settingsFlow: Flow<AppSettings> = context.ds.data.map { p ->
        AppSettings(
            defaultSort = p[DEFAULT_SORT] ?: 0,
            themeMode = p[THEME_MODE] ?: 2,
            hapticFeedback = p[HAPTIC_FEEDBACK] ?: true,
            keepScreenOn = p[KEEP_SCREEN_ON] ?: true
        )
    }.distinctUntilChanged()

    suspend fun updateSetting(propertyName: String, value: Any) {
        val def = definitions[propertyName] ?: return
        context.ds.edit { prefs ->
            when (def) {
                is SettingDefinition.BooleanSetting -> prefs[def.key] = (value as? Boolean) ?: return@edit
                is SettingDefinition.IntSetting -> prefs[def.key] = (value as? Int) ?: return@edit
            }
        }
    }

    suspend fun updateSettings(update: (AppSettings) -> AppSettings) {
        val current = settingsFlow.first()
        val updated = update(current)
        context.ds.edit { prefs ->
            definitions.values.forEach { def ->
                when (def) {
                    is SettingDefinition.BooleanSetting -> {
                        val old = def.getValue(current)
                        val new = def.getValue(updated)
                        if (old != new) prefs[def.key] = new
                    }
                    is SettingDefinition.IntSetting -> {
                        val old = def.getValue(current)
                        val new = def.getValue(updated)
                        if (old != new) prefs[def.key] = new
                    }
                }
            }
        }
    }
}
