package app.batstats.battery.data.db

import android.content.Context
import androidx.room.*

@Database(
    entities = [BatterySample::class, ChargeSession::class, AlarmRule::class],
    version = 1,
    exportSchema = true
)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryDao(): BatteryDao
    abstract fun sessionDao(): SessionDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var INSTANCE: BatteryDatabase? = null

        fun get(context: Context): BatteryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery.db"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}