package app.batstats.battery.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import rikka.shizuku.ShizukuProvider
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ShizukuBridge(private val context: Context) {

    companion object {
        private const val TAG = "ShizukuBridge"

        const val PERMISSION_REQUEST_CODE = 1001

        private const val SERVICE_VERSION = 2

        private const val BIND_TIMEOUT_MS = 10_000L
        private const val DEFAULT_CMD_TIMEOUT_MS = 25_000L

        private const val READ_GRACE_MS = 5_000L

        private const val MAX_OUTPUT_BYTES = 12 * 1024 * 1024
        private const val COPY_BUFFER_BYTES = 64 * 1024

        private const val PING_RETRIES = 4
        private const val PING_RETRY_DELAY_MS = 120L
    }

    enum class Failure { NOT_RUNNING, NO_PERMISSION, BIND_FAILED, TRANSPORT }

    sealed class RunResult {
        data class Success(val output: String) : RunResult()
        data class Error(val message: String, val reason: Failure) : RunResult()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binderRef = AtomicReference<IBinder?>(null)
    private val bindMutex = Mutex()
    private val listenersRegistered = AtomicBoolean(false)

    @Volatile
    private var everSeen = false

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _granted = MutableStateFlow(false)
    val granted: StateFlow<Boolean> = _granted.asStateFlow()

    private val args by lazy {
        UserServiceArgs(ComponentName(context.packageName, ShellUserService::class.java.name))
            .daemon(false)
            .processNameSuffix("shz")
            .tag("ShellSvc")
            .version(SERVICE_VERSION)
    }

    private val pendingBind = AtomicReference<CompletableDeferred<IBinder?>?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "UserService connected (alive=${service?.isBinderAlive})")
            binderRef.set(service)
            pendingBind.getAndSet(null)?.complete(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "UserService disconnected")
            binderRef.set(null)
            pendingBind.getAndSet(null)?.complete(null)
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received")
        everSeen = true
        _running.value = true
        binderRef.set(null)
        _granted.value = checkPermissionNow()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder died")
        _running.value = false
        _granted.value = false
        binderRef.set(null)
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == PERMISSION_REQUEST_CODE) {
                _granted.value = grantResult == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "Permission result: ${_granted.value}")
            }
        }

    fun warmUp() {
        if (!listenersRegistered.compareAndSet(false, true)) return
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not register Shizuku listeners: ${t.message}")
            listenersRegistered.set(false)
        }
    }

    fun ping(): Boolean {
        val alive = try {
            Shizuku.pingBinder()
        } catch (t: Throwable) {
            Log.d(TAG, "pingBinder threw: ${t.message}")
            false
        }
        if (alive) {
            everSeen = true
            _running.value = true
        }
        return alive
    }

    suspend fun isRunning(): Boolean {
        if (ping()) return true
        if (!everSeen) return false
        repeat(PING_RETRIES) {
            delay(PING_RETRY_DELAY_MS)
            if (ping()) return true
        }
        Log.w(TAG, "Shizuku stopped responding")
        _running.value = false
        return false
    }

    private fun checkPermissionNow(): Boolean = try {
        if (Shizuku.isPreV11()) {
            ContextCompat.checkSelfPermission(context, ShizukuProvider.PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    } catch (t: Throwable) {
        Log.d(TAG, "checkSelfPermission failed: ${t.message}")
        false
    }

    fun hasPermission(): Boolean {
        if (!ping()) return false
        return checkPermissionNow().also { _granted.value = it }
    }

    suspend fun hasPermissionResilient(): Boolean {
        if (!isRunning()) return false
        return checkPermissionNow().also { _granted.value = it }
    }

    fun isPermanentlyDenied(): Boolean = try {
        ping() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED &&
            !Shizuku.shouldShowRequestPermissionRationale()
    } catch (_: Throwable) {
        false
    }

    fun requestPermission(requestCode: Int = PERMISSION_REQUEST_CODE) {
        if (!ping()) {
            Log.w(TAG, "requestPermission: Shizuku not running, ignoring")
            return
        }
        try {
            Shizuku.requestPermission(requestCode)
        } catch (t: Throwable) {
            Log.w(TAG, "requestPermission failed: ${t.message}")
        }
    }

    suspend fun run(cmd: String, timeoutMs: Long = DEFAULT_CMD_TIMEOUT_MS): RunResult =
        withContext(Dispatchers.IO) {
            if (!isRunning()) {
                return@withContext RunResult.Error("Shizuku is not running", Failure.NOT_RUNNING)
            }
            if (!hasPermissionResilient()) {
                return@withContext RunResult.Error(
                    "Shizuku permission not granted",
                    Failure.NO_PERMISSION
                )
            }

            val binder = ensureBound()
                ?: return@withContext RunResult.Error(
                    "Could not start the Shizuku helper service",
                    Failure.BIND_FAILED
                )

            val first = execute(binder, cmd, timeoutMs)
            if (first !is RunResult.Error || first.reason != Failure.TRANSPORT) {
                return@withContext first
            }

            Log.d(TAG, "Retrying after transport failure: ${first.message}")
            binderRef.set(null)
            val fresh = ensureBound() ?: return@withContext first
            execute(fresh, cmd, timeoutMs)
        }

    suspend fun runOrNull(cmd: String): String? =
        (run(cmd) as? RunResult.Success)?.output

    private suspend fun execute(binder: IBinder, cmd: String, timeoutMs: Long): RunResult {
        if (!binder.isBinderAlive) {
            return RunResult.Error("Helper service is no longer alive", Failure.TRANSPORT)
        }
        return try {
            val piped = runViaPipe(binder, cmd, timeoutMs)
            when {
                piped != null -> RunResult.Success(piped)
                else -> runInline(binder, cmd)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Command transport failed: ${t.message}")
            RunResult.Error(t.message ?: t.javaClass.simpleName, Failure.TRANSPORT)
        }
    }

    private suspend fun runViaPipe(binder: IBinder, cmd: String, timeoutMs: Long): String? {
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        val accepted = try {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeString(cmd)
                data.writeLong(timeoutMs)
                writeSide.writeToParcel(data, 0)
                if (!binder.transact(ShellUserService.TRANSACTION_RUN_PIPE, data, reply, 0)) {
                    false
                } else {
                    reply.readInt() == 1
                }
            } finally {
                data.recycle()
                reply.recycle()
            }
        } catch (t: Throwable) {
            runCatching { readSide.close() }
            throw t
        } finally {
            runCatching { writeSide.close() }
        }

        if (!accepted) {
            runCatching { readSide.close() }
            return null
        }

        val watchdog = scope.launch {
            delay(timeoutMs + READ_GRACE_MS)
            Log.w(TAG, "Pipe read timed out for: $cmd")
            runCatching { readSide.close() }
        }
        return try {
            readAll(readSide)
        } finally {
            watchdog.cancel()
        }
    }

    private fun readAll(pfd: ParcelFileDescriptor): String {
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
            val sink = ByteArrayOutputStream(COPY_BUFFER_BYTES)
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (total + read >= MAX_OUTPUT_BYTES) {
                    sink.write(buffer, 0, MAX_OUTPUT_BYTES - total)
                    Log.w(TAG, "Output truncated at $MAX_OUTPUT_BYTES bytes")
                    break
                }
                sink.write(buffer, 0, read)
                total += read
            }
            return sink.toString(Charsets.UTF_8.name())
        }
    }

    private fun runInline(binder: IBinder, cmd: String): RunResult {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeString(cmd)
            if (!binder.transact(ShellUserService.TRANSACTION_RUN, data, reply, 0)) {
                RunResult.Error("Binder transaction rejected", Failure.TRANSPORT)
            } else {
                val out = reply.readString()
                if (out == null) {
                    RunResult.Error("Empty response from helper service", Failure.TRANSPORT)
                } else {
                    RunResult.Success(out)
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            RunResult.Error(t.message ?: t.javaClass.simpleName, Failure.TRANSPORT)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private suspend fun ensureBound(): IBinder? {
        binderRef.get()?.takeIf { it.isBinderAlive }?.let { return it }

        return bindMutex.withLock {
            binderRef.get()?.takeIf { it.isBinderAlive }?.let { return@withLock it }

            val deferred = CompletableDeferred<IBinder?>()
            pendingBind.set(deferred)

            val started = withContext(Dispatchers.Main) {
                try {
                    Shizuku.bindUserService(args, connection)
                    true
                } catch (t: Throwable) {
                    Log.e(TAG, "bindUserService failed", t)
                    false
                }
            }
            if (!started) {
                pendingBind.compareAndSet(deferred, null)
                return@withLock null
            }

            val startedAt = SystemClock.elapsedRealtime()
            val binder = withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }
            pendingBind.compareAndSet(deferred, null)

            if (binder == null || !binder.isBinderAlive) {
                Log.e(TAG, "UserService bind failed after ${SystemClock.elapsedRealtime() - startedAt} ms")
                binderRef.set(null)
                null
            } else {
                Log.d(TAG, "UserService bound in ${SystemClock.elapsedRealtime() - startedAt} ms")
                binder
            }
        }
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(args, connection, true)
        } catch (t: Throwable) {
            Log.e(TAG, "unbind failed", t)
        }
        binderRef.set(null)
        pendingBind.getAndSet(null)?.complete(null)
    }
}
