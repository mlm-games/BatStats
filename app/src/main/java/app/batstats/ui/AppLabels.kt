package app.batstats.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

val LocalShowAppNames = compositionLocalOf { true }

private val appLabels = HashMap<String, String?>()

private fun resolveAppLabel(context: Context, packageName: String): String? {
    val pkg = packageName.substringBefore(':')
    if (appLabels.containsKey(pkg)) return appLabels[pkg]
    val label = runCatching {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(pkg, 0)
        ).toString()
    }.getOrNull()?.takeIf { it.isNotBlank() && it != pkg }
    appLabels[pkg] = label
    return label
}

@Composable
fun rememberAppLabel(packageName: String): String? {
    val context = LocalContext.current
    val enabled = LocalShowAppNames.current
    return remember(enabled, context, packageName) {
        if (enabled) resolveAppLabel(context, packageName) else null
    }
}

@Composable
fun rememberAppLabelLine(packageName: String): String =
    rememberAppLabel(packageName)?.let { "$it · $packageName" } ?: packageName
