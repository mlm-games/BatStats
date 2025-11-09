package app.batstats.battery.data.db

import android.content.Context
import androidx.room.*

@TypeConverters(EnumConverters::class)
@Database(
    entities = [BatterySample::class, ChargeSession::class, AlarmRule::class, AppEnergyStat::class],
    version = 2,
    exportSchema = true
)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryDao(): BatteryDao
    abstract fun sessionDao(): SessionDao
    abstract fun alarmDao(): AlarmDao
    abstract fun appEnergyDao(): AppEnergyDao

    companion object {
        @Volatile private var INSTANCE: BatteryDatabase? = null

        fun get(context: Context): BatteryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery.db"
                )
                    // For dev speed; consider real migrations when schema stabilizes
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
    }
}

class EnumConverters {
    @TypeConverter fun fromSessionType(t: SessionType?): String? = t?.name
    @TypeConverter fun toSessionType(s: String?): SessionType? = s?.let { enumValueOf<SessionType>(it) }

    @TypeConverter fun fromAlarmType(t: AlarmType?): String? = t?.name
    @TypeConverter fun toAlarmType(s: String?): AlarmType? = s?.let { enumValueOf<AlarmType>(it) }
}
