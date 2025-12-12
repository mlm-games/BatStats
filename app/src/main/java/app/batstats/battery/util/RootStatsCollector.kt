package app.batstats.battery.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File

/**
 * Collects root-only battery statistics.
 * These require actual root access, not just Shizuku/ADB.
 */
object RootStatsCollector {

    data class KernelBatteryInfo(
        val technology: String?,
        val cycleCount: Int?,
        val chargeFullDesign: Long?, // μAh
        val chargeFull: Long?, // μAh (actual current capacity)
        val chargeNow: Long?, // μAh
        val currentNow: Long?, // μA
        val voltageNow: Int?, // μV
        val tempNow: Int?, // 0.1°C
        val health: String?,
        val status: String?,
        val capacityLevel: String?,
        val timeToEmptyNow: Long?, // secs
        val timeToFullNow: Long?, // secs
        val batteryAge: Double? // percentage of design capacity
    )

    data class KernelWakelockInfo(
        val name: String,
        val count: Int,
        val expireCount: Int,
        val wakeCount: Int,
        val activeCount: Int,
        val totalTime: Long, // nanosecs
        val sleepTime: Long, // nanosecs
        val maxTime: Long, // nanosecs
        val lastChange: Long // nanosecs
    )

    data class CpuInfo(
        val cluster: Int,
        val currentFreq: Long, // kHz
        val minFreq: Long,
        val maxFreq: Long,
        val governor: String,
        val timeInState: Map<Long, Long> // freq -> time in jiffies
    )

    data class ThermalZone(
        val name: String,
        val type: String,
        val tempMilliC: Int,
        val tripPoints: List<TripPoint>
    )

    data class TripPoint(
        val type: String,
        val tempMilliC: Int
    )

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val result = process.inputStream.bufferedReader().readText()
            process.waitFor()
            result.contains("uid=0")
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getKernelBatteryInfo(): KernelBatteryInfo? = withContext(Dispatchers.IO) {
        try {
            val batteryPath = "/sys/class/power_supply/battery"
            if (!File(batteryPath).exists()) return@withContext null

            fun readFile(name: String): String? = try {
                File("$batteryPath/$name").readText().trim()
            } catch (_: Exception) { null }

            val chargeFullDesign = readFile("charge_full_design")?.toLongOrNull()
            val chargeFull = readFile("charge_full")?.toLongOrNull()

            KernelBatteryInfo(
                technology = readFile("technology"),
                cycleCount = readFile("cycle_count")?.toIntOrNull(),
                chargeFullDesign = chargeFullDesign,
                chargeFull = chargeFull,
                chargeNow = readFile("charge_now")?.toLongOrNull(),
                currentNow = readFile("current_now")?.toLongOrNull(),
                voltageNow = readFile("voltage_now")?.toIntOrNull(),
                tempNow = readFile("temp")?.toIntOrNull(),
                health = readFile("health"),
                status = readFile("status"),
                capacityLevel = readFile("capacity_level"),
                timeToEmptyNow = readFile("time_to_empty_now")?.toLongOrNull(),
                timeToFullNow = readFile("time_to_full_now")?.toLongOrNull(),
                batteryAge = if (chargeFullDesign != null && chargeFull != null && chargeFullDesign > 0) {
                    (chargeFull.toDouble() / chargeFullDesign) * 100
                } else null
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getKernelWakelocks(): List<KernelWakelockInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<KernelWakelockInfo>()
        try {
            // Try new path first
            val wakelockPath = when {
                File("/sys/kernel/wakelock_stats").exists() -> "/sys/kernel/wakelock_stats"
                File("/proc/wakelocks").exists() -> "/proc/wakelocks"
                else -> null
            }

            if (wakelockPath != null) {
                File(wakelockPath).bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val parts = line.split(Regex("\\s+"))
                        if (parts.size >= 6) {
                            result.add(
                                KernelWakelockInfo(
                                    name = parts[0].trim('"'),
                                    count = parts.getOrNull(1)?.toIntOrNull() ?: 0,
                                    expireCount = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                                    wakeCount = parts.getOrNull(3)?.toIntOrNull() ?: 0,
                                    activeCount = parts.getOrNull(4)?.toIntOrNull() ?: 0,
                                    totalTime = parts.getOrNull(5)?.toLongOrNull() ?: 0L,
                                    sleepTime = parts.getOrNull(6)?.toLongOrNull() ?: 0L,
                                    maxTime = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
                                    lastChange = parts.getOrNull(8)?.toLongOrNull() ?: 0L
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        result.sortedByDescending { it.totalTime }
    }

    suspend fun getCpuInfo(): List<CpuInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<CpuInfo>()
        try {
            val cpuDir = File("/sys/devices/system/cpu")
            val cpuDirs = cpuDir.listFiles { f -> f.name.matches(Regex("cpu\\d+")) }
                ?.sortedBy { it.name.removePrefix("cpu").toIntOrNull() ?: 0 }
                ?: return@withContext result

            // Group by policy (cluster)
            val clusters = mutableMapOf<Int, MutableList<Int>>()
            cpuDirs.forEach { cpu ->
                val cpuNum = cpu.name.removePrefix("cpu").toIntOrNull() ?: return@forEach
                val policyPath = File("${cpu.absolutePath}/cpufreq/affected_cpus")
                val cluster = if (policyPath.exists()) {
                    policyPath.readText().trim().split(" ").firstOrNull()?.toIntOrNull() ?: cpuNum
                } else cpuNum
                clusters.getOrPut(cluster) { mutableListOf() }.add(cpuNum)
            }

            clusters.forEach { (clusterNum, _) ->
                val cpuPath = "/sys/devices/system/cpu/cpu$clusterNum/cpufreq"
                if (!File(cpuPath).exists()) return@forEach

                fun read(name: String) = try {
                    File("$cpuPath/$name").readText().trim()
                } catch (_: Exception) { null }

                val timeInState = mutableMapOf<Long, Long>()
                try {
                    File("$cpuPath/stats/time_in_state").bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            val parts = line.split(" ")
                            if (parts.size >= 2) {
                                val freq = parts[0].toLongOrNull() ?: return@forEach
                                val time = parts[1].toLongOrNull() ?: 0L
                                timeInState[freq] = time
                            }
                        }
                    }
                } catch (_: Exception) { }

                result.add(
                    CpuInfo(
                        cluster = clusterNum,
                        currentFreq = read("scaling_cur_freq")?.toLongOrNull() ?: 0L,
                        minFreq = read("scaling_min_freq")?.toLongOrNull() ?: 0L,
                        maxFreq = read("scaling_max_freq")?.toLongOrNull() ?: 0L,
                        governor = read("scaling_governor") ?: "unknown",
                        timeInState = timeInState
                    )
                )
            }
        } catch (_: Exception) { }
        result
    }

    suspend fun getThermalZones(): List<ThermalZone> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ThermalZone>()
        try {
            val thermalDir = File("/sys/class/thermal")
            val zones = thermalDir.listFiles { f -> f.name.startsWith("thermal_zone") }
                ?: return@withContext result

            zones.forEach { zone ->
                fun read(name: String) = try {
                    File("${zone.absolutePath}/$name").readText().trim()
                } catch (_: Exception) { null }

                val tripPoints = mutableListOf<TripPoint>()
                var i = 0
                while (true) {
                    val tripType = read("trip_point_${i}_type") ?: break
                    val tripTemp = read("trip_point_${i}_temp")?.toIntOrNull() ?: break
                    tripPoints.add(TripPoint(tripType, tripTemp))
                    i++
                }

                result.add(
                    ThermalZone(
                        name = zone.name,
                        type = read("type") ?: "unknown",
                        tempMilliC = read("temp")?.toIntOrNull() ?: 0,
                        tripPoints = tripPoints
                    )
                )
            }
        } catch (_: Exception) { }
        result.sortedBy { it.name }
    }

    suspend fun runAsRoot(command: String): String? = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val result = process.inputStream.bufferedReader().use(BufferedReader::readText)
            process.waitFor()
            result
        } catch (_: Exception) {
            null
        }
    }
}