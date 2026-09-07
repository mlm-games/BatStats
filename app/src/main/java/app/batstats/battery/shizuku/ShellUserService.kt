package app.batstats.battery.shizuku

import android.os.Binder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import java.util.concurrent.TimeUnit

class ShellUserService : Binder() {

    companion object {
        const val TRANSACTION_RUN = 1

        const val TRANSACTION_RUN_PIPE = 2

        const val TRANSACTION_DESTROY = 16777114

        private const val MAX_INLINE_OUTPUT_CHARS = 192 * 1024

        private const val DEFAULT_TIMEOUT_MS = 30_000L
        private const val COPY_BUFFER_BYTES = 64 * 1024
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return when (code) {
            TRANSACTION_RUN -> {
                val cmd = data.readString().orEmpty()
                val out = runCommand(cmd, DEFAULT_TIMEOUT_MS)
                reply?.writeString(
                    if (out.length > MAX_INLINE_OUTPUT_CHARS) out.take(MAX_INLINE_OUTPUT_CHARS) else out
                )
                true
            }

            TRANSACTION_RUN_PIPE -> {
                val cmd = data.readString().orEmpty()
                val timeoutMs = data.readLong().takeIf { it > 0 } ?: DEFAULT_TIMEOUT_MS
                val pfd = try {
                    ParcelFileDescriptor.CREATOR.createFromParcel(data)
                } catch (_: Throwable) {
                    null
                }
                if (pfd == null) {
                    reply?.writeInt(0)
                } else {
                    reply?.writeInt(1)
                    streamCommand(cmd, pfd, timeoutMs)
                }
                true
            }

            TRANSACTION_DESTROY -> {
                destroy()
                true
            }

            else -> super.onTransact(code, data, reply, flags)
        }
    }

    private fun streamCommand(cmd: String, pfd: ParcelFileDescriptor, timeoutMs: Long) {
        val worker = Thread {
            val sink = ParcelFileDescriptor.AutoCloseOutputStream(pfd)
            var process: Process? = null
            var watchdog: Thread? = null
            try {
                val p = ProcessBuilder("sh", "-c", cmd)
                    .redirectErrorStream(true)
                    .start()
                process = p
                runCatching { p.outputStream.close() }

                watchdog = Thread {
                    try {
                        if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) p.destroyForcibly()
                    } catch (_: InterruptedException) {
                    }
                }.apply {
                    isDaemon = true
                    start()
                }

                val buffer = ByteArray(COPY_BUFFER_BYTES)
                val source = p.inputStream
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    sink.write(buffer, 0, read)
                }
                sink.flush()
            } catch (t: Throwable) {
                runCatching {
                    sink.write("ERROR:${t.message ?: t.javaClass.simpleName}".toByteArray())
                }
            } finally {
                watchdog?.interrupt()
                runCatching { process?.destroy() }
                runCatching { sink.close() }
            }
        }
        worker.isDaemon = true
        worker.name = "batstats-shell"
        worker.start()
    }

    private fun runCommand(cmd: String, timeoutMs: Long): String {
        var process: Process? = null
        return try {
            val p = ProcessBuilder("sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            process = p
            runCatching { p.outputStream.close() }
            val out = p.inputStream.bufferedReader().use { it.readText() }
            if (!p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) p.destroyForcibly()
            out
        } catch (t: Throwable) {
            "ERROR:${t.message ?: t.javaClass.simpleName}"
        } finally {
            runCatching { process?.destroy() }
        }
    }

    private fun destroy() {
        Thread {
            runCatching { Thread.sleep(100) }
            Runtime.getRuntime().halt(0)
        }.apply { isDaemon = true }.start()
    }
}
