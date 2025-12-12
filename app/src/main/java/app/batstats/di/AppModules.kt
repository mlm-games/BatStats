package app.batstats.di

import app.batstats.battery.shizuku.ShizukuBridge
import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.BatterySettingsRepository
import app.batstats.battery.data.db.AppEnergyDao
import app.batstats.battery.data.db.BatteryDatabase
import app.batstats.battery.util.DetailedStatsCollector
import app.batstats.insights.ForegroundDrainTracker
import app.batstats.battery.shizuku.BstatsCollector
import org.koin.dsl.module

val appModule = module {
    single<BatteryDatabase> { BatteryGraph.db }
    single<AppEnergyDao> { BatteryGraph.db.appEnergyDao() }
    single<BatteryRepository> { BatteryGraph.repo }
    single<BatterySettingsRepository> { BatteryGraph.settings }

    single { ForegroundDrainTracker(get(), get(), get()) }
    single { ShizukuBridge(get()) }
    single { BstatsCollector(get(), get()) }
    single { DetailedStatsCollector(get(), get()) }
}