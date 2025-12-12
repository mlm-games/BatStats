package app.batstats.battery.util

import kotlin.math.roundToLong

/**
 * Comprehensive parser for `dumpsys batterystats --checkin` and related commands.
 * Extracts detailed per-app and system-wide battery statistics.
 */
object BatteryStatsParser {

    data class FullSnapshot(
        val capturedAt: Long = System.currentTimeMillis(),
        val statsSinceCharged: Boolean = true,
        val batteryRealtimeMs: Long = 0L,
        val screenOnTimeMs: Long = 0L,
        val screenOffDischargePercent: Float = 0f,
        val screenOnDischargePercent: Float = 0f,
        val estimatedCapacityMah: Int = 0,
        val apps: List<AppPowerStats> = emptyList(),
        val wakelocks: List<WakelockStats> = emptyList(),
        val kernelWakelocks: List<KernelWakelockStats> = emptyList(),
        val alarms: List<AlarmStats> = emptyList(),
        val jobs: List<JobStats> = emptyList(),
        val syncs: List<SyncStats> = emptyList(),
        val network: List<NetworkStats> = emptyList(),
        val sensors: List<SensorStats> = emptyList(),
        val signalStrength: List<SignalStrengthStats> = emptyList(),
        val wifiSignal: List<WifiSignalStats> = emptyList(),
        val bluetooth: BluetoothStats? = null,
        val doze: DozeStats? = null,
        val cpuFrequency: List<CpuFrequencyStats> = emptyList(),
        val processStats: List<ProcessStats> = emptyList()
    )

    data class AppPowerStats(
        val uid: Int,
        val packageName: String,
        val powerMah: Double,
        val cpuTimeMs: Long = 0L,
        val cpuPowerMah: Double = 0.0,
        val wakeLockTimeMs: Long = 0L,
        val wakeLockPowerMah: Double = 0.0,
        val mobilePowerMah: Double = 0.0,
        val wifiPowerMah: Double = 0.0,
        val gpsPowerMah: Double = 0.0,
        val sensorPowerMah: Double = 0.0,
        val cameraPowerMah: Double = 0.0,
        val flashlightPowerMah: Double = 0.0,
        val audioPowerMah: Double = 0.0,
        val videoPowerMah: Double = 0.0,
        val bluetoothPowerMah: Double = 0.0,
        val screenPowerMah: Double = 0.0,
        val proportionalSmearMah: Double = 0.0,
        val foregroundTimeMs: Long = 0L,
        val foregroundServiceTimeMs: Long = 0L,
        val backgroundTimeMs: Long = 0L,
        val cachedTimeMs: Long = 0L,
        val topTimeMs: Long = 0L,
        val mobileRxBytes: Long = 0L,
        val mobileTxBytes: Long = 0L,
        val wifiRxBytes: Long = 0L,
        val wifiTxBytes: Long = 0L,
        val mobileRxPackets: Long = 0L,
        val mobileTxPackets: Long = 0L,
        val wifiRxPackets: Long = 0L,
        val wifiTxPackets: Long = 0L,
        val gpsTimeMs: Long = 0L,
        val sensorTimeMs: Long = 0L,
        val cameraTimeMs: Long = 0L,
        val flashlightTimeMs: Long = 0L,
        val audioTimeMs: Long = 0L,
        val videoTimeMs: Long = 0L,
        val bluetoothScanTimeMs: Long = 0L,
        val bluetoothUnoptimizedScanTimeMs: Long = 0L
    )

    data class WakelockStats(
        val uid: Int,
        val packageName: String,
        val tag: String,
        val type: WakelockType,
        val count: Int,
        val totalTimeMs: Long,
        val maxTimeMs: Long = 0L,
        val backgroundTimeMs: Long = 0L,
        val backgroundCount: Int = 0
    )

    enum class WakelockType { PARTIAL, FULL, WINDOW, DRAW }

    data class KernelWakelockStats(
        val name: String,
        val count: Int,
        val totalTimeMs: Long,
        val activeCount: Int = 0,
        val maxTimeMs: Long = 0L,
        val lastChangeMs: Long = 0L,
        val preventSuspendTimeMs: Long = 0L
    )

    data class AlarmStats(
        val uid: Int,
        val packageName: String,
        val tag: String,
        val count: Int,
        val wakeups: Int,
        val totalTimeMs: Long,
        val backgroundCount: Int = 0,
        val backgroundTimeMs: Long = 0L
    )

    data class JobStats(
        val uid: Int,
        val packageName: String,
        val jobName: String,
        val count: Int,
        val totalTimeMs: Long,
        val backgroundCount: Int = 0,
        val backgroundTimeMs: Long = 0L
    )

    data class SyncStats(
        val uid: Int,
        val packageName: String,
        val authority: String,
        val count: Int,
        val totalTimeMs: Long,
        val backgroundCount: Int = 0,
        val backgroundTimeMs: Long = 0L
    )

    data class NetworkStats(
        val uid: Int,
        val packageName: String,
        val mobileRxBytes: Long,
        val mobileTxBytes: Long,
        val wifiRxBytes: Long,
        val wifiTxBytes: Long,
        val btRxBytes: Long = 0L,
        val btTxBytes: Long = 0L,
        val mobileActiveTimeMs: Long = 0L,
        val mobileActiveCount: Int = 0
    )

    data class SensorStats(
        val uid: Int,
        val packageName: String,
        val sensorHandle: Int,
        val sensorName: String,
        val count: Int,
        val totalTimeMs: Long,
        val backgroundTimeMs: Long = 0L,
        val backgroundCount: Int = 0
    )

    data class SignalStrengthStats(
        val level: Int, // 0 (none) to 4 (great)
        val durationMs: Long,
        val percentOfTotal: Float
    )

    data class WifiSignalStats(
        val level: Int, // 0 (none) to 4 (great)
        val durationMs: Long,
        val percentOfTotal: Float
    )

    data class BluetoothStats(
        val idleTimeMs: Long,
        val rxTimeMs: Long,
        val txTimeMs: Long,
        val powerMah: Double,
        val scanTimeMs: Long = 0L
    )

    data class DozeStats(
        val idleModeTimeMs: Long,
        val idleModeCount: Int,
        val deepIdleTimeMs: Long,
        val deepIdleCount: Int,
        val lightIdleTimeMs: Long,
        val lightIdleCount: Int,
        val maintenanceTimeMs: Long,
        val maintenanceCount: Int
    )

    data class CpuFrequencyStats(
        val cluster: Int,
        val frequency: Long,
        val timeMs: Long,
        val percentOfTotal: Float
    )

    data class ProcessStats(
        val uid: Int,
        val packageName: String,
        val processName: String,
        val userTimeMs: Long,
        val systemTimeMs: Long,
        val foregroundTimeMs: Long,
        val starts: Int
    )

    fun parseCheckin(raw: String): FullSnapshot {
        val lines = raw.lineSequence()
        val uidToPkg = mutableMapOf<Int, String>()
        val appStats = mutableMapOf<Int, AppPowerStats>()
        val wakelocks = mutableListOf<WakelockStats>()
        val kernelWakelocks = mutableListOf<KernelWakelockStats>()
        val alarms = mutableListOf<AlarmStats>()
        val jobs = mutableListOf<JobStats>()
        val syncs = mutableListOf<SyncStats>()
        val network = mutableListOf<NetworkStats>()
        val sensors = mutableListOf<SensorStats>()
        val signalStrength = mutableListOf<SignalStrengthStats>()
        val wifiSignal = mutableListOf<WifiSignalStats>()
        var bluetooth: BluetoothStats? = null
        var doze: DozeStats? = null
        val cpuFreq = mutableListOf<CpuFrequencyStats>()
        val processStats = mutableListOf<ProcessStats>()

        var batteryRealtimeMs = 0L
        var screenOnTimeMs = 0L
        var screenOffDischarge = 0f
        var screenOnDischarge = 0f
        var estCapacity = 0

        lines.forEach { line ->
            val parts = line.split(',')
            if (parts.size < 4) return@forEach

            try {
                when {
                    // UID mapping: 9,0,i,uid,<uid>,<package>
                    parts.getOrNull(2) == "i" && parts.getOrNull(3) == "uid" && parts.size >= 6 -> {
                        val uid = parts[4].toIntOrNull() ?: return@forEach
                        uidToPkg[uid] = parts[5]
                    }

                    // Power use item: 9,<uid>,l,pwi,<type>,<mAh>,...
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "pwi" -> {
                        parsePowerUseItem(parts, uidToPkg, appStats)
                    }

                    // Wakelock: 9,<uid>,l,wl,<name>,<type>,<count>,<time>...
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "wl" -> {
                        parseWakelock(parts, uidToPkg, wakelocks)
                    }

                    // Kernel wakelock: 9,0,l,kwl,<name>,<count>,<time>...
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "kwl" -> {
                        parseKernelWakelock(parts, kernelWakelocks)
                    }

                    // Alarm: 9,<uid>,l,wua,<tag>,<count>,<time>,<wakeups>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "wua" -> {
                        parseAlarm(parts, uidToPkg, alarms)
                    }

                    // Job: 9,<uid>,l,jb,<job>,<count>,<time>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "jb" -> {
                        parseJob(parts, uidToPkg, jobs)
                    }

                    // Sync: 9,<uid>,l,sy,<authority>,<count>,<time>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "sy" -> {
                        parseSync(parts, uidToPkg, syncs)
                    }

                    // Network: 9,<uid>,l,nt,<rxB>,<txB>,...
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "nt" -> {
                        parseNetwork(parts, uidToPkg, network)
                    }

                    // Sensor: 9,<uid>,l,sr,<handle>,<count>,<time>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "sr" -> {
                        parseSensor(parts, uidToPkg, sensors)
                    }

                    // Signal strength: 9,0,l,sgt,<time0>,<time1>,<time2>,<time3>,<time4>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "sgt" -> {
                        parseSignalStrength(parts, signalStrength)
                    }

                    // WiFi signal: 9,0,l,wsg,<time0>,<time1>,<time2>,<time3>,<time4>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "wsg" -> {
                        parseWifiSignal(parts, wifiSignal)
                    }

                    // Bluetooth controller: 9,0,l,ble,<idle>,<rx>,<tx>,<power>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "ble" -> {
                        bluetooth = parseBluetooth(parts)
                    }

                    // Discharge step: screen on/off discharge
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "dc" -> {
                        val so = parts.getOrNull(5)?.toFloatOrNull()
                        val sof = parts.getOrNull(6)?.toFloatOrNull()
                        if (so != null) screenOnDischarge = so
                        if (sof != null) screenOffDischarge = sof
                    }

                    // Battery core: 9,0,l,bt,startCount,battRealtime,battUptime,...
                    parts[2] == "l" && parts[3] == "bt" -> {
                        batteryRealtimeMs = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                    }

                    // Misc: 9,0,l,m,screenOnTime,phoneOnTime,...
                    parts[2] == "l" && parts[3] == "m" -> {
                        screenOnTimeMs = parts.getOrNull(4)?.toLongOrNull() ?: 0L
                    }

                    // Power summary: 9,0,l,pws,capacity,computed,minDrained,maxDrained
                    parts[2] == "l" && parts[3] == "pws" -> {
                        estCapacity = parts.getOrNull(4)?.toIntOrNull() ?: 0
                    }

                    // Doze/idle: 9,0,l,di,<count>,<idletime>,<maintcount>,<mainttime>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "di" -> {
                        doze = parseDoze(parts)
                    }

                    // Process stats: 9,<uid>,l,pr,<process>,<user>,<sys>,<fg>,<starts>
                    parts.getOrNull(2) == "l" && parts.getOrNull(3) == "pr" -> {
                        parseProcess(parts, uidToPkg, processStats)
                    }
                }
            } catch (_: Exception) {
                // Skip malformed lines
            }
        }

        return FullSnapshot(
            capturedAt = System.currentTimeMillis(),
            batteryRealtimeMs = batteryRealtimeMs,
            screenOnTimeMs = screenOnTimeMs,
            screenOffDischargePercent = screenOffDischarge,
            screenOnDischargePercent = screenOnDischarge,
            estimatedCapacityMah = estCapacity,
            apps = appStats.values.toList().sortedByDescending { it.powerMah },
            wakelocks = wakelocks.sortedByDescending { it.totalTimeMs },
            kernelWakelocks = kernelWakelocks.sortedByDescending { it.totalTimeMs },
            alarms = alarms.sortedByDescending { it.count },
            jobs = jobs.sortedByDescending { it.totalTimeMs },
            syncs = syncs.sortedByDescending { it.totalTimeMs },
            network = network.sortedByDescending { it.mobileRxBytes + it.mobileTxBytes + it.wifiRxBytes + it.wifiTxBytes },
            sensors = sensors.sortedByDescending { it.totalTimeMs },
            signalStrength = signalStrength,
            wifiSignal = wifiSignal,
            bluetooth = bluetooth,
            doze = doze,
            cpuFrequency = cpuFreq,
            processStats = processStats.sortedByDescending { it.userTimeMs + it.systemTimeMs }
        )
    }

    private fun parsePowerUseItem(
        parts: List<String>,
        uidToPkg: Map<Int, String>,
        appStats: MutableMap<Int, AppPowerStats>
    ) {
        val uid = parts[1].toIntOrNull() ?: return
        val type = parts.getOrNull(4) ?: return
        val mah = parts.getOrNull(5)?.toDoubleOrNull() ?: 0.0

        if (type == "uid") {
            val pkg = uidToPkg[uid] ?: "uid:$uid"
            val existing = appStats[uid] ?: AppPowerStats(uid = uid, packageName = pkg, powerMah = 0.0)
            appStats[uid] = existing.copy(powerMah = existing.powerMah + mah)
        }
    }

    private fun parseWakelock(
        parts: List<String>,
        uidToPkg: Map<Int, String>,
        wakelocks: MutableList<WakelockStats>
    ) {
        val uid = parts[1].toIntOrNull() ?: return
        val pkg = uidToPkg[uid] ?: "uid:$uid"
        val tag = parts.getOrNull(4) ?: return

        // Find partial and background-partial blocks in a version‑independent way
        val pIndex = parts.indexOfFirst { it == "p" }
        val bpIndex = parts.indexOfFirst { it == "bp" }

        val partialTimeMs = if (pIndex > 0) {
            parts.getOrNull(pIndex - 1)?.toLongOrNull() ?: 0L
        } else 0L
        val partialCount = if (pIndex >= 0) {
            parts.getOrNull(pIndex + 1)?.toIntOrNull() ?: 0
        } else 0

        val bgPartialTimeMs = if (bpIndex > 0) {
            parts.getOrNull(bpIndex - 1)?.toLongOrNull() ?: 0L
        } else 0L
        val bgPartialCount = if (bpIndex >= 0) {
            parts.getOrNull(bpIndex + 1)?.toIntOrNull() ?: 0
        } else 0

        wakelocks.add(
            WakelockStats(
                uid = uid,
                packageName = pkg,
                tag = tag,
                type = WakelockType.PARTIAL,
                count = partialCount,
                totalTimeMs = partialTimeMs,
                backgroundTimeMs = bgPartialTimeMs,
                backgroundCount = bgPartialCount
            )
        )
    }

    private fun parseKernelWakelock(
        parts: List<String>,
        kernelWakelocks: MutableList<KernelWakelockStats>
    ) {
        val name = parts.getOrNull(4) ?: return
        val timeMs = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        val count = parts.getOrNull(6)?.toIntOrNull() ?: 0

        kernelWakelocks.add(
            KernelWakelockStats(
                name = name,
                count = count,
                totalTimeMs = timeMs
            )
        )
    }

    private fun parseAlarm(
        parts: List<String>,
        uidToPkg: Map<Int, String>,
        alarms: MutableList<AlarmStats>
    ) {
        val uid = parts[1].toIntOrNull() ?: return
        val pkg = uidToPkg[uid] ?: "uid:$uid"
        val tag = parts.getOrNull(4) ?: return
        val count = parts.getOrNull(5)?.toIntOrNull() ?: 0
        val timeMs = parts.getOrNull(6)?.toLongOrNull() ?: 0L
        val wakeups = parts.getOrNull(7)?.toIntOrNull() ?: 0

        alarms.add(
            AlarmStats(
                uid = uid,
                packageName = pkg,
                tag = tag,
                count = count,
                totalTimeMs = timeMs,
                wakeups = wakeups
            )
        )
    }

    private fun parseJob(
        parts: List<String>,
        uidToPkg: Map<Int, String>,
        jobs: MutableList<JobStats>
    ) {
        val uid = parts[1].toIntOrNull() ?: return
        val pkg = uidToPkg[uid] ?: "uid:$uid"
        val jobName = parts.getOrNull(4) ?: return
        val count = parts.getOrNull(5)?.toIntOrNull() ?: 0
        val timeMs = parts.getOrNull(6)?.toLongOrNull() ?: 0L

        jobs.add(
            JobStats(
                uid = uid,
                packageName = pkg,
                jobName = jobName,
                count = count,
                totalTimeMs = timeMs
            )
        )
    }

    private fun parseSync(
        parts: List<String>,
        uidToPkg: Map<Int, String>,
        syncs: MutableList<SyncStats>
    ) {
        val uid = parts[1].toIntOrNull() ?: return
        val pkg = uidToPkg[uid] ?: "uid:$uid"
        val authority = parts.getOrNull(4) ?: return
        val count = parts.getOrNull(5)?.toIntOrNull() ?: 0
        val timeMs = parts.getOrNull(6)?.toLongOrNull() ?: 0L

        syncs.add(
            SyncStats(
                uid = uid,
                packageName = pkg,
                authority = authority,
                count = count,
                totalTimeMs = timeMs
            )
        )
    }

    private fun parseNetwork(
        parts: List<String>,
        uidToPkg: Map<Int, String>,
        network: MutableList<NetworkStats>
    ) {
        val uid = parts[1].toIntOrNull() ?: return
        val pkg = uidToPkg[uid] ?: "uid:$uid"

        val mobileRx = parts.getOrNull(4)?.toLongOrNull() ?: 0L
        val mobileTx = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        val wifiRx = parts.getOrNull(6)?.toLongOrNull() ?: 0L
        val wifiTx = parts.getOrNull(7)?.toLongOrNull() ?: 0L
        val mobileActiveTime = parts.getOrNull(12)?.toLongOrNull() ?: 0L
        val mobileActiveCount = parts.getOrNull(13)?.toIntOrNull() ?: 0

        network.add(
            NetworkStats(
                uid = uid,
                packageName = pkg,
                mobileRxBytes = mobileRx,
                mobileTxBytes = mobileTx,
                wifiRxBytes = wifiRx,
                wifiTxBytes = wifiTx,
                mobileActiveTimeMs = mobileActiveTime,
                mobileActiveCount = mobileActiveCount
            )
        )
    }

    private fun parseSensor(
        parts: List<String>,
        uidToPkg: Map<Int, String>,
        sensors: MutableList<SensorStats>
    ) {
        val uid = parts[1].toIntOrNull() ?: return
        val pkg = uidToPkg[uid] ?: "uid:$uid"
        val handle = parts.getOrNull(4)?.toIntOrNull() ?: return
        val timeMs = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        val count = parts.getOrNull(6)?.toIntOrNull() ?: 0

        sensors.add(
            SensorStats(
                uid = uid,
                packageName = pkg,
                sensorHandle = handle,
                sensorName = getSensorName(handle),
                count = count,
                totalTimeMs = timeMs
            )
        )
    }

    private fun parseSignalStrength(
        parts: List<String>,
        signalStrength: MutableList<SignalStrengthStats>
    ) {
        val times = (4..8).mapNotNull { parts.getOrNull(it)?.toLongOrNull() }
        if (times.size < 5) return
        val total = times.sum().toFloat().coerceAtLeast(1f)
        times.forEachIndexed { level, time ->
            signalStrength.add(
                SignalStrengthStats(
                    level = level,
                    durationMs = time,
                    percentOfTotal = time / total
                )
            )
        }
    }

    private fun parseWifiSignal(
        parts: List<String>,
        wifiSignal: MutableList<WifiSignalStats>
    ) {
        val times = (4..8).mapNotNull { parts.getOrNull(it)?.toLongOrNull() }
        if (times.size < 5) return
        val total = times.sum().toFloat().coerceAtLeast(1f)
        times.forEachIndexed { level, time ->
            wifiSignal.add(
                WifiSignalStats(
                    level = level,
                    durationMs = time,
                    percentOfTotal = time / total
                )
            )
        }
    }

    private fun parseBluetooth(parts: List<String>): BluetoothStats? {
        val idle = parts.getOrNull(4)?.toLongOrNull() ?: return null
        val rx = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        val tx = parts.getOrNull(6)?.toLongOrNull() ?: 0L
        val power = parts.getOrNull(7)?.toDoubleOrNull() ?: 0.0
        return BluetoothStats(
            idleTimeMs = idle,
            rxTimeMs = rx,
            txTimeMs = tx,
            powerMah = power
        )
    }

    private fun parseDoze(parts: List<String>): DozeStats? {
        val deepCount = parts.getOrNull(4)?.toIntOrNull() ?: return null
        val deepTime = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        val lightCount = parts.getOrNull(6)?.toIntOrNull() ?: 0
        val lightTime = parts.getOrNull(7)?.toLongOrNull() ?: 0L
        val maintCount = parts.getOrNull(8)?.toIntOrNull() ?: 0
        val maintTime = parts.getOrNull(9)?.toLongOrNull() ?: 0L
        return DozeStats(
            idleModeTimeMs = deepTime + lightTime,
            idleModeCount = deepCount + lightCount,
            deepIdleTimeMs = deepTime,
            deepIdleCount = deepCount,
            lightIdleTimeMs = lightTime,
            lightIdleCount = lightCount,
            maintenanceTimeMs = maintTime,
            maintenanceCount = maintCount
        )
    }

    private fun parseProcess(
        parts: List<String>,
        uidToPkg: Map<Int, String>,
        processStats: MutableList<ProcessStats>
    ) {
        val uid = parts[1].toIntOrNull() ?: return
        val pkg = uidToPkg[uid] ?: "uid:$uid"
        val process = parts.getOrNull(4) ?: return
        val userMs = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        val sysMs = parts.getOrNull(6)?.toLongOrNull() ?: 0L
        val fgMs = parts.getOrNull(7)?.toLongOrNull() ?: 0L
        val starts = parts.getOrNull(8)?.toIntOrNull() ?: 0

        processStats.add(
            ProcessStats(
                uid = uid,
                packageName = pkg,
                processName = process,
                userTimeMs = userMs,
                systemTimeMs = sysMs,
                foregroundTimeMs = fgMs,
                starts = starts
            )
        )
    }

    private fun getSensorName(handle: Int): String = when (handle) {
        -10000 -> "GPS"
        else -> "Sensor #$handle"
    }

    data class DeviceIdleInfo(
        val currentState: String,
        val lightState: String,
        val deepEnabled: Boolean,
        val lightEnabled: Boolean,
        val screenOnTime: Long,
        val screenOffTime: Long,
        val whitelistedApps: List<String>,
        val tempWhitelistedApps: List<String>
    )

    fun parseDeviceIdle(raw: String): DeviceIdleInfo {
        var currentState = "UNKNOWN"
        var lightState = "UNKNOWN"
        var deepEnabled = true
        var lightEnabled = true
        var screenOnTime = 0L
        var screenOffTime = 0L
        val whitelisted = mutableListOf<String>()
        val tempWhitelisted = mutableListOf<String>()

        var inWhitelist = false
        var inTempWhitelist = false

        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("mState=") -> currentState = trimmed.removePrefix("mState=")
                trimmed.startsWith("mLightState=") -> lightState = trimmed.removePrefix("mLightState=")
                trimmed.startsWith("mDeepEnabled=") -> deepEnabled = trimmed.contains("true")
                trimmed.startsWith("mLightEnabled=") -> lightEnabled = trimmed.contains("true")
                trimmed.startsWith("mScreenOnTime=") -> screenOnTime = trimmed.removePrefix("mScreenOnTime=").toLongOrNull() ?: 0L
                trimmed.startsWith("mScreenOffTime=") -> screenOffTime = trimmed.removePrefix("mScreenOffTime=").toLongOrNull() ?: 0L
                trimmed == "Whitelist system apps:" || trimmed == "Whitelist apps:" -> {
                    inWhitelist = true
                    inTempWhitelist = false
                }
                trimmed.startsWith("Temp whitelist:") -> {
                    inWhitelist = false
                    inTempWhitelist = true
                }
                trimmed.isEmpty() -> {
                    inWhitelist = false
                    inTempWhitelist = false
                }
                inWhitelist && trimmed.isNotBlank() -> whitelisted.add(trimmed)
                inTempWhitelist && trimmed.isNotBlank() -> tempWhitelisted.add(trimmed.substringBefore(":"))
            }
        }

        return DeviceIdleInfo(
            currentState = currentState,
            lightState = lightState,
            deepEnabled = deepEnabled,
            lightEnabled = lightEnabled,
            screenOnTime = screenOnTime,
            screenOffTime = screenOffTime,
            whitelistedApps = whitelisted,
            tempWhitelistedApps = tempWhitelisted
        )
    }

    data class PowerManagerInfo(
        val screenBrightness: Int,
        val isScreenOn: Boolean,
        val holdingWakeLocks: List<String>,
        val suspendBlockers: List<String>,
        val batteryLevel: Int,
        val batteryStatus: String,
        val lowPowerMode: Boolean,
        val deviceIdleMode: String
    )

    fun parsePowerManager(raw: String): PowerManagerInfo {
        var brightness = 0
        var screenOn = false
        var batteryLevel = 0
        var batteryStatus = "UNKNOWN"
        var lowPowerMode = false
        var deviceIdleMode = "UNKNOWN"
        val wakeLocks = mutableListOf<String>()
        val suspendBlockers = mutableListOf<String>()

        var inWakeLocks = false
        var inBlockers = false

        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("mScreenBrightnessSetting=") -> {
                    brightness = trimmed.substringAfter("=").toIntOrNull() ?: 0
                }
                trimmed.startsWith("Display Power: state=") -> {
                    screenOn = trimmed.contains("ON")
                }
                trimmed.startsWith("mBatteryLevel=") -> {
                    batteryLevel = trimmed.substringAfter("=").toIntOrNull() ?: 0
                }
                trimmed.startsWith("mBatteryStatus=") -> {
                    batteryStatus = trimmed.substringAfter("=")
                }
                trimmed.startsWith("mLowPowerModeEnabled=") -> {
                    lowPowerMode = trimmed.contains("true")
                }
                trimmed.startsWith("mDeviceIdleMode=") -> {
                    deviceIdleMode = trimmed.substringAfter("=")
                }
                trimmed == "Wake Locks:" -> {
                    inWakeLocks = true
                    inBlockers = false
                }
                trimmed == "Suspend Blockers:" -> {
                    inWakeLocks = false
                    inBlockers = true
                }
                trimmed.isEmpty() -> {
                    inWakeLocks = false
                    inBlockers = false
                }
                inWakeLocks && trimmed.isNotBlank() -> wakeLocks.add(trimmed)
                inBlockers && trimmed.isNotBlank() -> suspendBlockers.add(trimmed)
            }
        }

        return PowerManagerInfo(
            screenBrightness = brightness,
            isScreenOn = screenOn,
            holdingWakeLocks = wakeLocks,
            suspendBlockers = suspendBlockers,
            batteryLevel = batteryLevel,
            batteryStatus = batteryStatus,
            lowPowerMode = lowPowerMode,
            deviceIdleMode = deviceIdleMode
        )
    }
}