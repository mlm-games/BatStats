package app.batstats.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.unit.dp
import app.batstats.ui.components.TvNavItem
import app.batstats.ui.components.TvNavigationRail

@Composable
fun TvShell(nav: NavHostController) {
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    Row(Modifier.fillMaxSize()) {
        TvNavigationRail(
            selectedRoute = route,
            items = listOf(
                TvNavItem("dashboard", "Dashboard", Icons.Default.BatteryChargingFull),
                TvNavItem("history", "History", Icons.Default.History),
                TvNavItem("alarms", "Alarms", Icons.Default.Notifications),
                TvNavItem("data", "Data", Icons.Default.Settings),
                TvNavItem("settings", "Settings", Icons.Default.Settings)
            ),
            onNavigate = { dest ->
                if (route != dest) nav.navigate(dest)
            },
            modifier = Modifier.width(100.dp)
        )
        Box(Modifier.fillMaxSize()) {
            NavGraph(nav = nav)
        }
    }
}