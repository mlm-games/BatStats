package app.batstats.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.batstats.ui.screens.AlarmsScreen
import app.batstats.ui.screens.BatterySettingsScreen
import app.batstats.ui.screens.DashboardScreen
import app.batstats.ui.screens.DataScreen
import app.batstats.ui.screens.DetailedStatsScreen
import app.batstats.ui.screens.HistoryScreen
import app.batstats.ui.screens.SessionDetailsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NavGraph(nav: NavHostController) {
    NavHost(navController = nav, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onOpenHistory = { nav.navigate("history") },
                onOpenAlarms = { nav.navigate("alarms") },
                onOpenSettings = { nav.navigate("settings") },
                onOpenData = { nav.navigate("data") },
                onOpenDetailedStats = { nav.navigate("detailed_stats") }
            )
        }
        composable("history") {
            HistoryScreen(
                onBack = { nav.popBackStack() },
                onOpenSession = { id -> nav.navigate("session/$id") }
            )
        }
        composable(
            "session/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("id")!!
            SessionDetailsScreen(
                sessionId = id,
                onBack = { nav.popBackStack() },
                vm = koinViewModel(parameters = { parametersOf(id) })
            )
        }
        composable("alarms") {
            AlarmsScreen(onBack = { nav.popBackStack() })
        }
        composable("data") {
            DataScreen(onBack = { nav.popBackStack() })
        }
        composable("settings") {
            BatterySettingsScreen(
                onBack = { nav.popBackStack() },
                onExportData = { nav.navigate("data") }
            )
        }
        composable("detailed_stats") {
            DetailedStatsScreen(onBack = { nav.popBackStack() })
        }
    }
}