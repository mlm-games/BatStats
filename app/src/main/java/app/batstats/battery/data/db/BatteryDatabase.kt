package app.batstats.battery.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(EnumConverters::class)
@Database(
    entities = [BatterySample::class, ChargeSession::class, AlarmRule::class, AppEnergyStat::class],
    version = 4,
    exportSchema = true
)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryDao(): BatteryDao
    abstract fun sessionDao(): SessionDao
    abstract fun alarmDao(): AlarmDao
    abstract fun appEnergyDao(): AppEnergyDao

    companion object {
        @Volatile private var INSTANCE: BatteryDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_energy_stats` (" +
                        "`bucketStart` INTEGER NOT NULL, " +
                        "`packageName` TEXT NOT NULL, " +
                        "`mode` TEXT NOT NULL, " +
                        "`energyMah` REAL NOT NULL, " +
                        "`samples` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`bucketStart`, `packageName`, `mode`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_app_energy_stats_bucketStart` " +
                        "ON `app_energy_stats` (`bucketStart`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_app_energy_stats_packageName` " +
                        "ON `app_energy_stats` (`packageName`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM app_energy_stats")
            }
        }

        // SQLite cannot relax NOT NULL in place, so rebuild the table and carry every row over.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `battery_samples_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "`levelPercent` INTEGER, " +
                        "`status` INTEGER NOT NULL, " +
                        "`plugged` INTEGER NOT NULL, " +
                        "`currentNowUa` INTEGER, " +
                        "`chargeCounterUah` INTEGER, " +
                        "`voltageMv` INTEGER, " +
                        "`temperatureDeciC` INTEGER, " +
                        "`health` INTEGER, " +
                        "`screenOn` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `battery_samples_new` " +
                        "(`id`,`timestamp`,`levelPercent`,`status`,`plugged`,`currentNowUa`," +
                        "`chargeCounterUah`,`voltageMv`,`temperatureDeciC`,`health`,`screenOn`) " +
                        "SELECT `id`,`timestamp`,`levelPercent`,`status`,`plugged`,`currentNowUa`," +
                        "`chargeCounterUah`,`voltageMv`,`temperatureDeciC`,`health`,`screenOn` " +
                        "FROM `battery_samples`"
                )
                db.execSQL("DROP TABLE `battery_samples`")
                db.execSQL("ALTER TABLE `battery_samples_new` RENAME TO `battery_samples`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_battery_samples_timestamp` " +
                        "ON `battery_samples` (`timestamp`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_battery_samples_status` " +
                        "ON `battery_samples` (`status`)"
                )
            }
        }

        fun get(context: Context): BatteryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
