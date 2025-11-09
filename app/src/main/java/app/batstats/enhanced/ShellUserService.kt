package app.batstats.enhanced

import android.os.Binder
import android.os.Parcel
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader

/**
 * Shizuku UserService binder that executes shell commands as shell (ADB) identity.
 */
class ShellUserService : Binder() {

    companion object {
        const val TRANSACTION_RUN = 1
        // Shizuku will call this when destroying the service
        const val TRANSACTION_DESTROY = 16777114
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return when (code) {
            TRANSACTION_RUN -> {
                val cmd = data.readString() ?: ""
                val out = runBlocking { runCommand(cmd) }
                reply?.writeString(out)
                true
            }
            TRANSACTION_DESTROY -> {
                // clean up if needed
                true
            }
            else -> super.onTransact(code, data, reply, flags)
        }
    }

    private fun runCommand(cmd: String): String {
        return try {
            val p = ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val out = p.inputStream.bufferedReader().use(BufferedReader::readText)
            p.waitFor()
            out
        } catch (t: Throwable) {
            "ERROR:${t.message ?: t::class.java.simpleName}"
        }
    }
}