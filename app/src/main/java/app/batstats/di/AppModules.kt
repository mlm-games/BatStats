package app.batstats.di

import android.os.Build
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.ExportImportManager
import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.battery.drain.AdvancedDrainTracker
import app.batstats.battery.drain.DrainNotificationManager
import app.batstats.battery.shizuku.BstatsCollector
import app.batstats.battery.shizuku.ShizukuBridge
import app.batstats.battery.util.DetailedStatsCollector
import app.batstats.battery.util.ShellRunner
import app.batstats.settings.AppSettings
import app.batstats.settings.AppSettingsSchema
import app.batstats.viewmodel.DashboardViewModel
import app.batstats.viewmodel.DataViewModel
import app.batstats.viewmodel.DetailedStatsViewModel
import app.batstats.viewmodel.DrainStatsViewModel
import app.batstats.viewmodel.HistoryViewModel
import app.batstats.viewmodel.SessionDetailsViewModel
import app.batstats.viewmodel.SettingsViewModel
import io.github.mlmgames.settings.core.SettingsRepository
import io.github.mlmgames.settings.core.backup.DeviceInfo
import io.github.mlmgames.settings.core.backup.SettingsBackupManager
import io.github.mlmgames.settings.core.datastore.createSettingsDataStore
import io.github.mlmgames.settings.core.managers.MigrationManager
import io.github.mlmgames.settings.core.managers.ResetManager
import io.github.mlmgames.settings.core.resources.AndroidStringResourceProvider
import io.github.mlmgames.settings.core.resources.StringResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private const val SCHEMA_VERSION = 2
private const val DATASTORE_NAME = "batstats_settings"

val appModule = module {
    single { CoroutineScope(SupervisorJob()) }
    single { BatteryDatabase.get(androidContext()) }
    single { createSettingsDataStore(androidContext(), DATASTORE_NAME) }

    single { ShizukuBridge(androidContext()) }
    single { ShellRunner(androidContext(), get()) }
    single { DetailedStatsCollector(get(), get(), androidContext(), get()) }
    single { BstatsCollector(get<BatteryDatabase>().appEnergyDao(), get(), get()) }

    single<SettingsRepository<AppSettings>> {
        SettingsRepository(dataStore = get(), schema = AppSettingsSchema)
    }

    single<StringResourceProvider> { AndroidStringResourceProvider(androidContext()) }
    single { ResetManager(get(), AppSettingsSchema) }
    single {
        MigrationManager(dataStore = get(), currentVersion = SCHEMA_VERSION).apply {
            addMigration(object : io.github.mlmgames.settings.core.managers.Migration {
                override val fromVersion = 1
                override val toVersion = 2
                override suspend fun migrate(prefs: androidx.datastore.preferences.core.MutablePreferences) {
                    prefs[androidx.datastore.preferences.core.booleanPreferencesKey("dynamic_colors")] = false
                }
            })
        }
    }

    single {
        val app = androidApplication()
        val appVersion = try {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }

        SettingsBackupManager(
            dataStore = get(),
            schema = AppSettingsSchema,
            appId = "app.batstats",
            schemaVersion = SCHEMA_VERSION,
            deviceInfoProvider = { DeviceInfo("Android", Build.VERSION.RELEASE, appVersion) }
        )
    }

    single { ExportImportManager(androidContext(), get()) }
    single { BatteryRepository(androidContext(), get(), get(), get()) }

    single { AdvancedDrainTracker(androidContext(), get(), get(), get(), get()) }
    single { DrainNotificationManager(androidContext(), get()) }

    viewModel { DashboardViewModel(androidApplication(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get(), get(), get()) }
    viewModel { DetailedStatsViewModel(get(), get(), get(), androidContext()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { DataViewModel(get()) }
    viewModel { DrainStatsViewModel(get()) }

    viewModel { (sessionId: String) -> SessionDetailsViewModel(androidApplication(), get(), get(), sessionId) }
}
