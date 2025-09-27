package app.batstats.battery.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.batstats.ui.components.MyScreenScaffold
import app.batstats.battery.ui.screens.*
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun NavGraph(nav: NavHostController) {
    NavHost(navController = nav, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onOpenHistory = { nav.navigate("history") },
                onOpenAlarms = { nav.navigate("alarms") },
                onOpenSettings = { nav.navigate("settings") }
            )
        }
        composable("history") {
            HistoryScreen(onBack = { nav.popBackStack() })
        }
        composable("alarms") {
            AlarmsScreen(onBack = { nav.popBackStack() })
        }
        composable("settings") {
            BatterySettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}