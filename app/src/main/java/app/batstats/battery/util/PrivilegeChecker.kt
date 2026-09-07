package app.batstats.battery.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.content.ContextCompat
import app.batstats.battery.shizuku.ShizukuBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PrivilegeChecker {

    fun hasDump(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED

    fun hasBatteryStats(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.BATTERY_STATS) == PackageManager.PERMISSION_GRANTED

    fun hasInteractAcrossUsers(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, "android.permission.INTERACT_ACROSS_USERS") == PackageManager.PERMISSION_GRANTED

    fun hasUsageStats(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = try {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } catch (_: Throwable) {
            return false
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasAdvancedViaAdb(context: Context): Boolean = hasDump(context) || hasBatteryStats(context)

    suspend fun hasRoot(): Boolean = withContext(Dispatchers.IO) {
        RootStatsCollector.isRootAvailable()
    }

    suspend fun hasShizuku(shizuku: ShizukuBridge): Boolean = withContext(Dispatchers.IO) {
        try { shizuku.hasPermissionResilient() } catch (_: Exception) { false }
    }

    suspend fun hasAdvancedAccess(context: Context, shizuku: ShizukuBridge): Boolean {
        if (hasAdvancedViaAdb(context)) return true
        if (hasShizuku(shizuku)) return true
        if (hasRoot()) return true
        return false
    }

    fun describeGrants(context: Context): Map<String, Boolean> = mapOf(
        "DUMP" to hasDump(context),
        "BATTERY_STATS" to hasBatteryStats(context),
        "PACKAGE_USAGE_STATS" to hasUsageStats(context),
        "INTERACT_ACROSS_USERS" to hasInteractAcrossUsers(context)
    )
}
