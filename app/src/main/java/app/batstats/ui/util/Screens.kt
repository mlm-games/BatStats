package app.batstats.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 Keys.
 */
@Serializable
sealed interface Screen: NavKey {
    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object History : Screen

    @Serializable
    data class SessionDetails(val sessionId: String) : Screen

    @Serializable
    data object Alarms : Screen

    @Serializable
    data object Data : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object DetailedStats : Screen
}

enum class AppDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    ALARMS("Alarms", Icons.Filled.Alarm, Icons.Outlined.Alarm),
    DETAILED_STATS("Stats", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}