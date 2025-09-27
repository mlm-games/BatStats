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