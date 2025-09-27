package app.batstats.battery

import android.app.Application
import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.BatterySettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

object BatteryGraph {
    lateinit var app: Application
        private set
    lateinit var db: BatteryDatabase
        private set
    lateinit var repo: BatteryRepository
        private set
    lateinit var settings: BatterySettingsRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob())

    fun init(application: Application) {
        app = application
        db = BatteryDatabase.get(application)
        settings = BatterySettingsRepository(application)
        repo = BatteryRepository(application, db, settings, appScope)
    }
}

class BatteryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BatteryGraph.init(this)
    }
}