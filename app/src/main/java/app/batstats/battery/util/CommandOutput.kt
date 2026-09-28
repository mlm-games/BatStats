package app.batstats.battery.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A command counts as successful only after a complete, bounded read that finishes inside the
 * deadline, followed by a clean exit.
 */
object CommandOutput {
    const val MAX_BYTES = 32 * 1024 * 1024

    sealed interface Result {
        data class Success(val output: String) : Result
        data class Failure(val reason: String) : Result
    }

    fun run(arguments: List<String>, timeoutMs: Long, maxBytes: Int = MAX_BYTES): Result {
        require(timeoutMs > 0) { "timeoutMs must be positive" }
        require(maxBytes in 1..MAX_BYTES) { "maxBytes out of range" }

        var process: Process? = null
        var reader: FutureTask<ByteArray>? = null
        return try {
            val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
            val child = ProcessBuilder(arguments).redirectErrorStream(true).start()
            process = child
            child.outputStream.close()

            val read = FutureTask { readBounded(child.inputStream, maxBytes) }
            reader = read
            Thread(read, "batstats-command-reader").apply {
                isDaemon = true
                start()
            }

            val bytes = read.get(remainingNanos(deadlineNanos), TimeUnit.NANOSECONDS)
            when {
                !child.waitFor(remainingNanos(deadlineNanos), TimeUnit.NANOSECONDS) ->
                    Result.Failure("Command timed out after ${timeoutMs}ms")
                child.exitValue() != 0 ->
                    Result.Failure("Command exited with status ${child.exitValue()}")
                else ->
                    Result.Success(bytes.toString(Charsets.UTF_8))
            }
        } catch (_: TimeoutException) {
            Result.Failure("Command timed out after ${timeoutMs}ms")
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            Result.Failure("Command interrupted")
        } catch (e: ExecutionException) {
            Result.Failure(e.cause?.message ?: "Command output could not be read")
        } catch (e: Exception) {
            Result.Failure(e.message ?: e.javaClass.simpleName)
        } finally {
            reader?.cancel(true)
            process?.takeIf { it.isAlive }?.let { child ->
                Thread({ runCatching { child.destroyForcibly() } }, "batstats-command-cleanup")
                    .apply {
                        isDaemon = true
                        start()
                    }
            }
        }
    }

    private fun remainingNanos(deadlineNanos: Long): Long =
        (deadlineNanos - System.nanoTime()).coerceAtLeast(1L)

    private fun readBounded(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val bytes = ByteArrayOutputStream()
        input.use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (bytes.size() + count > maxBytes) {
                    throw IOException("Output limit of $maxBytes bytes exceeded")
                }
                bytes.write(buffer, 0, count)
            }
        }
        return bytes.toByteArray()
    }
}
