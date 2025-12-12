package app.batstats.battery.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import java.util.concurrent.atomic.AtomicReference

class ShizukuBridge(private val context: Context) {

    companion object {
        private const val TAG = "ShizukuBridge"
        private const val TRANSACTION_RUN = 1
    }

    private val binderRef = AtomicReference<IBinder?>()
    private var bindingInProgress = false

    private val args by lazy {
        UserServiceArgs(
            ComponentName(context.packageName, ShellUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shz")
            .tag("ShellSvc")
            .version(1)
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "UserService connected: ${service != null}")
            binderRef.set(service)
            bindingInProgress = false
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "UserService disconnected")
            binderRef.set(null)
            bindingInProgress = false
        }
    }

    fun ping(): Boolean {
        val result = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            Log.e(TAG, "ping failed", e)
            false
        }
        Log.d(TAG, "ping: $result")
        return result
    }

    fun hasPermission(): Boolean {
        val result = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.e(TAG, "hasPermission check failed", e)
            false
        }
        Log.d(TAG, "hasPermission: $result")
        return result
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "requestPermission failed", e)
        }
    }

    sealed class RunResult {
        data class Success(val output: String) : RunResult()
        data class Error(val message: String) : RunResult()
    }

    private suspend fun ensureBound(): Boolean = withContext(Dispatchers.Main) {
        Log.d(TAG, "ensureBound called")

        if (!ping()) {
            Log.e(TAG, "Shizuku not running")
            return@withContext false
        }
        if (!hasPermission()) {
            Log.e(TAG, "Shizuku permission not granted")
            return@withContext false
        }

        // Already bound and alive
        if (binderRef.get()?.isBinderAlive == true) {
            Log.d(TAG, "Already bound")
            return@withContext true
        }

        // Avoid duplicate binding attempts
        if (bindingInProgress) {
            Log.d(TAG, "Binding already in progress, waiting...")
            repeat(30) {
                delay(50)
                if (binderRef.get()?.isBinderAlive == true) return@withContext true
            }
            return@withContext binderRef.get()?.isBinderAlive == true
        }

        Log.d(TAG, "Binding UserService...")
        bindingInProgress = true

        try {
            Shizuku.bindUserService(args, conn)
        } catch (e: Exception) {
            Log.e(TAG, "bindUserService failed", e)
            bindingInProgress = false
            return@withContext false
        }

        // Wait for connection
        repeat(40) { // 2 seconds
            delay(50)
            if (binderRef.get()?.isBinderAlive == true) {
                Log.d(TAG, "UserService bound successfully")
                return@withContext true
            }
        }

        Log.e(TAG, "UserService bind timeout")
        bindingInProgress = false
        false
    }

    suspend fun run(cmd: String): RunResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "run: $cmd")

        if (!ensureBound()) {
            return@withContext RunResult.Error("Failed to bind UserService")
        }

        val binder = binderRef.get()
        if (binder == null || !binder.isBinderAlive) {
            return@withContext RunResult.Error("UserService binder not available")
        }

        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeString(cmd)
            val ok = binder.transact(TRANSACTION_RUN, data, reply, 0)
            if (!ok) {
                Log.e(TAG, "transact failed for: $cmd")
                return@withContext RunResult.Error("Binder transact failed")
            }
            val result = reply.readString()
                ?: return@withContext RunResult.Error("Null response from command")
            Log.d(TAG, "Command output length: ${result.length}")
            RunResult.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "run exception", e)
            RunResult.Error("Exception: ${e.message}")
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    // Legacy method for compatibility - returns String?
    suspend fun runOrNull(cmd: String): String? {
        return when (val result = run(cmd)) {
            is RunResult.Success -> result.output
            is RunResult.Error -> null
        }
    }

    fun unbind() {
        try {
            Shizuku.unbindUserService(args, conn, true)
        } catch (e: Exception) {
            Log.e(TAG, "unbind failed", e)
        }
        binderRef.set(null)
        bindingInProgress = false
    }
}