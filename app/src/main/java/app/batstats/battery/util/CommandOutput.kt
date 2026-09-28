package app.batstats.battery.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** A command succeeds only after a complete, bounded read and a zero exit code. */
object CommandOutput {
    const val MAX_BYTES = 8 * 1024 * 1024
    data class Result(val output: String = "", val error: String? = null) {
        val successful: Boolean get() = error == null
    }

    fun run(arguments: List<String>, timeoutMs: Long, maxBytes: Int = MAX_BYTES): Result {
        require(timeoutMs > 0 && maxBytes in 1..MAX_BYTES)
        var process: Process? = null
        var reader: FutureTask<ByteArray>? = null
        return try {
            val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
            val child = ProcessBuilder(arguments).redirectErrorStream(true).start()
            process = child
            child.outputStream.close()
            val read = FutureTask {
                val bytes = ByteArrayOutputStream()
                child.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (bytes.size() + count > maxBytes) throw IOException("Output limit exceeded")
                        bytes.write(buffer, 0, count)
                    }
                }
                bytes.toByteArray()
            }
            reader = read
            Thread(read, "batstats-command-reader").apply { isDaemon = true; start() }
            val bytes = read.get((deadline - System.nanoTime()).coerceAtLeast(1), TimeUnit.NANOSECONDS)
            when {
                !child.waitFor((deadline - System.nanoTime()).coerceAtLeast(1), TimeUnit.NANOSECONDS) ->
                    Result(error = "Command timed out")
                child.exitValue() != 0 -> Result(error = "Command exited with status ${child.exitValue()}")
                else -> Result(output = bytes.toString(Charsets.UTF_8))
            }
        } catch (_: TimeoutException) {
            Result(error = "Command timed out")
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            Result(error = "Command interrupted")
        } catch (e: ExecutionException) {
            Result(error = e.cause?.message ?: "Command output could not be read")
        } catch (e: Exception) {
            Result(error = e.javaClass.simpleName)
        } finally {
            reader?.cancel(true)
            process?.let { child ->
                Thread({ runCatching { child.destroyForcibly() } }, "batstats-command-cleanup")
                    .apply { isDaemon = true; start() }
            }
        }
    }
}
