package app.batstats.battery

import android.app.Application
import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.BatterySettingsRepository
import app.batstats.insights.ForegroundDrainTracker
import app.batstats.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

object BatteryGraph {
    lateinit var app: Application
        private set
    lateinit var db: BatteryDatabase
        private set
    lateinit var repo: BatteryRepository
        private set
    lateinit var settings: BatterySettingsRepository
        private set

    lateinit var drainTracker: ForegroundDrainTracker
        private set

    private val appScope = CoroutineScope(SupervisorJob())

    fun init(application: Application) {
        app = application
        db = BatteryDatabase.get(application)
        settings = BatterySettingsRepository(application)
        repo = BatteryRepository(application, db, settings, appScope)
        drainTracker = ForegroundDrainTracker(application, repo, db.appEnergyDao())
    }
}

class BatteryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BatteryGraph.init(this)
        startKoin {
            androidContext(this@BatteryApp)
            modules(appModule)
        }
    }
}