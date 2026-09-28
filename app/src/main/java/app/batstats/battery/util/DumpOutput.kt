package app.batstats.battery.util

/**
 * Rejects dumps that look plausible but are not. Markers are the ones dumpsys itself emits on
 * stderr, plus the security text services print when a permission check fails.
 */
object DumpOutput {
    fun failure(raw: String): String? {
        for (line in raw.lineSequence()) {
            val value = line.trimStart()
            when {
                value.startsWith("Permission Denial", true) ||
                    value.startsWith("java.lang.SecurityException") ->
                    return "Command was refused by Android"
                value.startsWith("Can't find service:", true) -> return "Android service unavailable"
                value.startsWith("Error with service '", true) ||
                    value.startsWith("Failed to create pipe to dump service info", true) ||
                    value.startsWith("Error in poll while dumping service", true) ||
                    value.startsWith("Failed to read while dumping service", true) ||
                    value.startsWith("Failed to write while dumping service", true) ->
                    return "Android service dump failed"
                value.startsWith("***") && value.contains("DUMP TIMEOUT") ->
                    return "Android service dump was incomplete"
                // Pre-Android-14 dumpsys wording.
                value.startsWith("Error dumping service info", true) ->
                    return "Android service dump failed"
            }
        }
        return null
    }
}
