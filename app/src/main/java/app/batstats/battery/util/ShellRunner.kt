package app.batstats.battery.util

import android.content.Context
import android.os.SystemClock
import android.util.Log
import app.batstats.battery.shizuku.ShizukuBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ShellRunner(
    private val context: Context,
    private val shizuku: ShizukuBridge
) {
    companion object {
        private const val TAG = "ShellRunner"
        private const val CMD_TIMEOUT_SEC = 25L

        private const val MODE_CACHE_MS = 10_000L
    }

    enum class Mode { ROOT, SHIZUKU, ADB, NONE }

    data class ShellResult(
        val output: String,
        val mode: Mode
    )

    sealed class Outcome {
        data class Success(val output: String, val mode: Mode) : Outcome()

        data class Failure(val mode: Mode, val message: String) : Outcome()
    }

    private val modeLock = Mutex()

    @Volatile
    private var cachedMode: Mode? = null

    @Volatile
    private var cachedModeAt = 0L

    suspend fun run(cmd: String): ShellResult? =
        (exec(cmd) as? Outcome.Success)?.let { ShellResult(it.output, it.mode) }

    suspend fun exec(cmd: String, allowEmpty: Boolean = false): Outcome = withContext(Dispatchers.IO) {
        var lastFailure: Outcome.Failure? = null

        fun usable(out: String?): Boolean =
            out != null && (allowEmpty || out.isNotBlank()) && !isErrorOutput(out)

        if (RootStatsCollector.isRootAvailable()) {
            val out = RootStatsCollector.runAsRoot(cmd)
            if (usable(out)) {
                Log.d(TAG, "run via ROOT: $cmd (${out!!.length} chars)")
                return@withContext Outcome.Success(out, Mode.ROOT)
            }
            Log.w(TAG, "Root run failed for: $cmd")
            lastFailure = Outcome.Failure(Mode.ROOT, "Root command produced no usable output")
        }

        when (val r = shizuku.run(cmd, TimeUnit.SECONDS.toMillis(CMD_TIMEOUT_SEC))) {
            is ShizukuBridge.RunResult.Success -> {
                val out = r.output
                if (usable(out)) {
                    Log.d(TAG, "run via SHIZUKU: $cmd (${out.length} chars)")
                    return@withContext Outcome.Success(out, Mode.SHIZUKU)
                }
                Log.w(TAG, "Shizuku returned empty/error output for: $cmd")
                lastFailure = Outcome.Failure(
                    Mode.SHIZUKU,
                    if (out.isBlank()) "Shizuku returned no output" else out.take(200).trim()
                )
            }

            is ShizukuBridge.RunResult.Error -> {
                Log.w(TAG, "Shizuku run error (${r.reason}): ${r.message}")
                if (r.reason != ShizukuBridge.Failure.NOT_RUNNING) {
                    lastFailure = Outcome.Failure(Mode.SHIZUKU, r.message)
                }
            }
        }

        if (PrivilegeChecker.hasAdvancedViaAdb(context)) {
            val out = runDirect(cmd)
            if (usable(out)) {
                Log.d(TAG, "run via ADB: $cmd (${out!!.length} chars)")
                return@withContext Outcome.Success(out, Mode.ADB)
            }
            Log.w(TAG, "Direct run empty or error: ${out?.take(200)}")
            lastFailure = Outcome.Failure(
                Mode.ADB,
                "Granted DUMP/BATTERY_STATS but the command was refused"
            )
        }

        Log.w(TAG, "All runners failed for: $cmd")
        lastFailure ?: Outcome.Failure(Mode.NONE, "No privileged backend available")
    }

    suspend fun runDirectOnly(cmd: String): String? = withContext(Dispatchers.IO) {
        if (!PrivilegeChecker.hasAdvancedViaAdb(context)) return@withContext null
        runDirect(cmd)?.takeIf { it.isNotBlank() && !isErrorOutput(it) }
    }

    private fun runDirect(cmd: String): String? {
        var process: Process? = null
        var watchdog: Thread? = null
        val timedOut = AtomicBoolean(false)
        return try {
            val p = ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            process = p
            runCatching { p.outputStream.close() }

            watchdog = Thread {
                try {
                    if (!p.waitFor(CMD_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                        timedOut.set(true)
                        p.destroyForcibly()
                    }
                } catch (_: InterruptedException) {
                }
            }.apply {
                isDaemon = true
                start()
            }

            val out = p.inputStream.bufferedReader().use { it.readText() }
            when {
                timedOut.get() -> null
                out.contains("Permission Denial", ignoreCase = true) -> null
                else -> out
            }
        } catch (e: Exception) {
            Log.e(TAG, "runDirect exception", e)
            null
        } finally {
            watchdog?.interrupt()
            runCatching { process?.destroy() }
        }
    }

    private fun isErrorOutput(out: String): Boolean =
        out.startsWith("ERROR") || out.startsWith("Permission Denial", ignoreCase = true)

    suspend fun detectMode(forceRefresh: Boolean = false): Mode {
        if (!forceRefresh) {
            cachedMode?.let {
                if (SystemClock.elapsedRealtime() - cachedModeAt < MODE_CACHE_MS) return it
            }
        }
        return modeLock.withLock {
            if (!forceRefresh) {
                cachedMode?.let {
                    if (SystemClock.elapsedRealtime() - cachedModeAt < MODE_CACHE_MS) {
                        return@withLock it
                    }
                }
            }
            val mode = probeMode()
            cachedMode = mode
            cachedModeAt = SystemClock.elapsedRealtime()
            mode
        }
    }

    private suspend fun probeMode(): Mode = withContext(Dispatchers.IO) {
        if (RootStatsCollector.isRootAvailable()) return@withContext Mode.ROOT
        if (shizuku.hasPermissionResilient()) return@withContext Mode.SHIZUKU
        if (PrivilegeChecker.hasAdvancedViaAdb(context)) {
            if (runDirectOnly("dumpsys battery") != null) return@withContext Mode.ADB
        }
        Mode.NONE
    }

    fun invalidateMode() {
        cachedMode = null
        cachedModeAt = 0L
        RootStatsCollector.invalidateRootCache()
    }

    suspend fun hasAnyPrivilegedAccess(): Boolean = detectMode() != Mode.NONE
}
