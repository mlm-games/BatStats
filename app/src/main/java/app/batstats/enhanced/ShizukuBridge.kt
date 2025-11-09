import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Parcel
import app.batstats.enhanced.ShellUserService
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

class ShizukuBridge(private val context: Context) {

    companion object {
        // Must match ShellUserService
        private const val TRANSACTION_RUN = 1
    }

    private val binderRef = AtomicReference<IBinder?>()

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
            binderRef.set(service)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            binderRef.set(null)
        }
    }

    fun ping(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean =
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun requestPermission(requestCode: Int = 1001) {
        Shizuku.requestPermission(requestCode)
    }

    private suspend fun ensureBound(): Boolean = withContext(Dispatchers.Main) {
        if (!ping() || !hasPermission()) return@withContext false
        if (binderRef.get()?.isBinderAlive == true) return@withContext true

        val gate = CompletableDeferred<Boolean>()
        Shizuku.bindUserService(args, conn)
        repeat(30) { // wait up to ~1.5s
            if (binderRef.get()?.isBinderAlive == true) {
                gate.complete(true); return@withContext true
            }
            delay(50)
        }
        gate.complete(binderRef.get()?.isBinderAlive == true)
        gate.await()
    }

    suspend fun run(cmd: String): String? = withContext(Dispatchers.IO) {
        if (!ensureBound()) return@withContext null
        val b = binderRef.get() ?: return@withContext null

        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeString(cmd)
            val ok = b.transact(TRANSACTION_RUN, data, reply, 0)
            if (!ok) return@withContext null
            reply.readString()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun unbind() {
        runCatching { Shizuku.unbindUserService(args, conn, true) }
        binderRef.set(null)
    }
}