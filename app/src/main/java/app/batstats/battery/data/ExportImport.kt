package app.batstats.battery.data

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.db.BatterySample
import app.batstats.battery.data.db.ChargeSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Serializable
data class BatteryExport(
    val samples: List<BatterySample> = emptyList(),
    val sessions: List<ChargeSession> = emptyList()
)

object ExportImport {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportJson(dest: Uri, from: Long, to: Long, includeSamples: Boolean, includeSessions: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val ctx = BatteryGraph.app
                val out = ctx.contentResolver.openOutputStream(dest) ?: return@withContext false
                val samples = if (includeSamples) BatteryGraph.db.batteryDao()
                    .run { samplesBetween(if (from==0L) 0L else from, to).first() } else emptyList()
                val sessions = if (includeSessions) BatteryGraph.db.sessionDao()
                    .run { sessionsPaged(10_000, 0).first() } else emptyList()
                out.use { it.write(json.encodeToString(BatteryExport.serializer(), BatteryExport(samples, sessions)).toByteArray()) }
                true
            }.getOrElse { false }
        }

    suspend fun exportCsvToFolder(tree: Uri, from: Long, to: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = BatteryGraph.app
            val dir = DocumentFile.fromTreeUri(ctx, tree) ?: return@withContext false
            val sdf = ctx.contentResolver
            // Samples
            val f1 = dir.createFile("text/csv", "battery_samples.csv") ?: return@withContext false
            sdf.openOutputStream(f1.uri)?.bufferedWriter()?.use { w ->
                w.appendLine("timestamp,levelPercent,status,plugged,currentNowUa,chargeCounterUah,voltageMv,temperatureDeciC,health,screenOn")
                BatteryGraph.db.batteryDao().samplesBetween(if (from==0L) 0L else from, to).first().forEach { s ->
                    w.appendLine("${s.timestamp},${s.levelPercent},${s.status},${s.plugged},${s.currentNowUa ?: ""},${s.chargeCounterUah ?: ""},${s.voltageMv ?: ""},${s.temperatureDeciC ?: ""},${s.health ?: ""},${s.screenOn}")
                }
            } ?: return@withContext false
            // Sessions
            val f2 = dir.createFile("text/csv", "charge_sessions.csv") ?: return@withContext false
            sdf.openOutputStream(f2.uri)?.bufferedWriter()?.use { w ->
                w.appendLine("sessionId,type,startTime,endTime,startLevel,endLevel,deltaUah,avgCurrentUa,estCapacityMah")
                BatteryGraph.db.sessionDao().sessionsPaged(10_000, 0).first().forEach { s ->
                    w.appendLine("${s.sessionId},${s.type},${s.startTime},${s.endTime ?: ""},${s.startLevel},${s.endLevel ?: ""},${s.deltaUah ?: ""},${s.avgCurrentUa ?: ""},${s.estCapacityMah ?: ""}")
                }
            } ?: return@withContext false
            true
        }.getOrElse { false }
    }

    suspend fun importJson(src: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = BatteryGraph.app
            val input = ctx.contentResolver.openInputStream(src) ?: return@withContext false
            val payload = input.use { json.decodeFromString(BatteryExport.serializer(), it.readBytes().toString(Charsets.UTF_8)) }
            // Insert
            val bd = BatteryGraph.db.batteryDao()
            val sd = BatteryGraph.db.sessionDao()
            payload.samples.forEach { bd.insertSample(it.copy(id = 0)) }
            payload.sessions.forEach { sd.upsert(it) }
            true
        }.getOrElse { false }
    }

    suspend fun importCsv(src: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val ctx = BatteryGraph.app
            val name = DocumentFile.fromSingleUri(ctx, src)?.name ?: ""
            ctx.contentResolver.openInputStream(src)?.use { ins ->
                val reader = BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8))
                val header = reader.readLine() ?: return@use
                if (header.contains("timestamp,levelPercent")) {
                    val bd = BatteryGraph.db.batteryDao()
                    reader.lineSequence().forEach { line ->
                        if (line.isBlank()) return@forEach
                        val p = line.split(',')
                        val s = BatterySample(
                            timestamp = p[0].toLong(),
                            levelPercent = p[1].toInt(),
                            status = p[2].toInt(),
                            plugged = p[3].toInt(),
                            currentNowUa = p[4].ifBlank { null }?.toLong(),
                            chargeCounterUah = p[5].ifBlank { null }?.toLong(),
                            voltageMv = p[6].ifBlank { null }?.toInt(),
                            temperatureDeciC = p[7].ifBlank { null }?.toInt(),
                            health = p[8].ifBlank { null }?.toInt(),
                            screenOn = p[9].toBooleanStrictOrNull() ?: false
                        )
                        bd.insertSample(s)
                    }
                } else if (header.contains("sessionId,type")) {
                    val sd = BatteryGraph.db.sessionDao()
                    reader.lineSequence().forEach { line ->
                        if (line.isBlank()) return@forEach
                        val p = line.split(',')
                        val s = ChargeSession(
                            sessionId = p[0],
                            type = enumValueOf(p[1]),
                            startTime = p[2].toLong(),
                            endTime = p[3].ifBlank { null }?.toLong(),
                            startLevel = p[4].toInt(),
                            endLevel = p[5].ifBlank { null }?.toInt(),
                            deltaUah = p[6].ifBlank { null }?.toLong(),
                            avgCurrentUa = p[7].ifBlank { null }?.toLong(),
                            estCapacityMah = p[8].ifBlank { null }?.toInt()
                        )
                        sd.upsert(s)
                    }
                } else {
                    return@withContext false
                }
            }
            true
        }.getOrElse { false }
    }
}