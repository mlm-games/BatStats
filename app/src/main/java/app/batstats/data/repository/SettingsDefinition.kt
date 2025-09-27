package app.batstats.data.repository

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@Target(PROPERTY)
@Retention(RUNTIME)
annotation class Setting(
    val title: String,
    val description: String = "",
    val category: SettingCategory,
    val type: SettingType,
    val dependsOn: String = "",
    val min: Float = 0f,
    val max: Float = 100f,
    val step: Float = 1f,
    val options: Array<String> = []
)

enum class SettingCategory { GENERAL, APPEARANCE, ARCHIVES, SYSTEM }
enum class SettingType { TOGGLE, DROPDOWN, SLIDER, BUTTON }

// Extended with IR‑specific preferences
data class AppSettings(
    @Setting(
        title = "Default Sort",
        description = "Default sorting for lists",
        category = SettingCategory.GENERAL,
        type = SettingType.DROPDOWN,
        options = ["Name", "Recently Updated", "Size", "Recently Added"]
    )
    val defaultSort: Int = 0,

    @Setting(
        title = "Theme",
        category = SettingCategory.APPEARANCE,
        type = SettingType.DROPDOWN,
        options = ["System", "Light", "Dark"]
    )
    val themeMode: Int = 2,

    val useAuroraTheme: Boolean = true,

    // IR specific
    @Setting(
        title = "Haptic feedback",
        description = "Vibrate lightly on key press",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val hapticFeedback: Boolean = true,

    @Setting(
        title = "Keep screen on",
        description = "Prevent the screen from sleeping while a remote is open",
        category = SettingCategory.GENERAL,
        type = SettingType.TOGGLE
    )
    val keepScreenOn: Boolean = true
)

class SettingsManager {
    fun getAll(): List<Pair<KProperty1<AppSettings, *>, Setting>> {
        return AppSettings::class.memberProperties.mapNotNull { p ->
            val ann = p.findAnnotation<Setting>()
            if (ann != null) p to ann else null
        }
    }
    fun getByCategory(): Map<SettingCategory, List<Pair<KProperty1<AppSettings, *>, Setting>>> =
        getAll().groupBy { it.second.category }

    fun isEnabled(settings: AppSettings, property: KProperty1<AppSettings, *>, annotation: Setting): Boolean {
        val depends = annotation.dependsOn
        if (depends.isBlank()) return true
        val depProp = AppSettings::class.memberProperties.find { it.name == depends }
        return if (depProp != null) {
            val v = depProp.get(settings)
            (v as? Boolean) ?: true
        } else true
    }
}