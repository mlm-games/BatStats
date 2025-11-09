package app.batstats.battery.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSample(sample: BatterySample): Long

    @Query("SELECT * FROM battery_samples ORDER BY timestamp DESC LIMIT 1")
    suspend fun lastSample(): BatterySample?

    @Query("SELECT * FROM battery_samples WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    fun samplesBetween(from: Long, to: Long): Flow<List<BatterySample>>

    @Query("DELETE FROM battery_samples WHERE timestamp < :olderThan")
    suspend fun purge(olderThan: Long)
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ChargeSession)

    @Query("SELECT * FROM charge_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun active(): ChargeSession?

    @Query("SELECT * FROM charge_sessions WHERE endTime IS NULL LIMIT 1")
    fun activeFlow(): Flow<ChargeSession?>

    @Query("SELECT * FROM charge_sessions ORDER BY startTime DESC LIMIT :limit OFFSET :offset")
    fun sessionsPaged(limit: Int, offset: Int): Flow<List<ChargeSession>>

    @Query("SELECT * FROM charge_sessions WHERE sessionId = :id")
    fun session(id: String): Flow<ChargeSession?>

    @Query("UPDATE charge_sessions SET endTime=:end, endLevel=:endLevel, deltaUah=:delta, avgCurrentUa=:avg, estCapacityMah=:cap WHERE sessionId=:id")
    suspend fun complete(id: String, end: Long, endLevel: Int, delta: Long?, avg: Long?, cap: Int?)
}

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AlarmRule)

    @Query("SELECT * FROM alarm_rules")
    fun rules(): Flow<List<AlarmRule>>

    @Query("DELETE FROM alarm_rules WHERE id=:id")
    suspend fun delete(id: Long)
}

/**
 * App energy aggregation DAO (heuristic mode).
 */
@Dao
interface AppEnergyDao {
    @Transaction
    suspend fun incrementHour(packageName: String, atMillis: Long, deltaMah: Double, addSamples: Int, mode: String = "HEURISTIC") {
        val bucket = hourBucketStart(atMillis)
        val changed = updateIncrement(bucket, packageName, mode, deltaMah, addSamples)
        if (changed == 0) {
            insert(AppEnergyStat(bucketStart = bucket, packageName = packageName, mode = mode, energyMah = deltaMah, samples = addSamples))
        }
    }

    @Query("UPDATE app_energy_stats SET energyMah = energyMah + :deltaMah, samples = samples + :addSamples WHERE bucketStart = :bucket AND packageName = :pkg AND mode = :mode")
    suspend fun updateIncrement(bucket: Long, pkg: String, mode: String, deltaMah: Double, addSamples: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stat: AppEnergyStat)

    @Query("""
        SELECT packageName AS packageName, SUM(energyMah) AS energyMah, SUM(samples) AS samples
        FROM app_energy_stats
        WHERE bucketStart BETWEEN :from AND :to AND mode = :mode
        GROUP BY packageName
        ORDER BY energyMah DESC
        LIMIT :limit
    """)
    fun topDrainers(from: Long, to: Long, mode: String = "HEURISTIC", limit: Int = 10): Flow<List<AppDrainAggregate>>

    @Query("DELETE FROM app_energy_stats WHERE bucketStart < :olderThan")
    suspend fun purgeOlderThan(olderThan: Long)
}

private fun hourBucketStart(ms: Long): Long {
    val hourMs = 60 * 60 * 1000L
    return (ms / hourMs) * hourMs
}
