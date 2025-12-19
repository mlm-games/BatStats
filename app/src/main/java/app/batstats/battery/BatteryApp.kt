package app.batstats.battery

import android.app.Application
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.di.appModule
import app.batstats.insights.ForegroundDrainTracker
import app.batstats.settings.AppSettings
import io.github.mlmgames.settings.core.SettingsRepository
import io.github.mlmgames.settings.core.managers.MigrationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class BatteryApp : Application() {

    private val appScope: CoroutineScope by inject()
    private val migrationManager: MigrationManager by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@BatteryApp)
            modules(appModule)
        }

        // Run migrations
        appScope.launch {
            migrationManager.migrate()
        }
    }
}

/**
 * Legacy accessor for components. Prefer using Koin injection directly.
 */
object BatteryGraph : KoinComponent {
    val db: BatteryDatabase by inject()
    val repo: BatteryRepository by inject()
    val settings: SettingsRepository<AppSettings> by inject()
    val drainTracker: ForegroundDrainTracker by inject()
}