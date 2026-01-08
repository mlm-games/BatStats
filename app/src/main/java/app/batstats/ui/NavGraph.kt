package app.batstats.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import app.batstats.ui.screens.AlarmsScreen
import app.batstats.ui.screens.BatterySettingsScreen
import app.batstats.ui.screens.DashboardScreen
import app.batstats.ui.screens.DataScreen
import app.batstats.ui.screens.DetailedStatsScreen
import app.batstats.ui.screens.DrainStatsScreen
import app.batstats.ui.screens.HistoryScreen
import app.batstats.ui.screens.SessionDetailsScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NavGraph(
    backStack: NavBackStack<NavKey>,
    decorators: List<NavEntryDecorator<Any>>
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeAt(backStack.lastIndex) },
        entryDecorators = decorators,
        entryProvider = entryProvider {

            // Dashboard
            entry<Screen.Dashboard> {
                DashboardScreen(
                    onOpenHistory = { backStack.add(Screen.History) },
                    onOpenAlarms = { backStack.add(Screen.Alarms) },
                    onOpenSettings = { backStack.add(Screen.Settings) },
                    onOpenData = { backStack.add(Screen.Data) },
                    onOpenDetailedStats = { backStack.add(Screen.DetailedStats) },
                    onOpenDrainStats = { backStack.add(Screen.DrainStats) }
                )
            }

            entry<Screen.History> {
                HistoryScreen(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onOpenSession = { id -> backStack.add(Screen.SessionDetails(id)) }
                )
            }

            entry<Screen.SessionDetails> { args ->
                SessionDetailsScreen(
                    sessionId = args.sessionId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    vm = koinViewModel(parameters = { parametersOf(args.sessionId) })
                )
            }

            entry<Screen.Alarms> {
                AlarmsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }

            entry<Screen.Data> {
                DataScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }

            entry<Screen.Settings> {
                BatterySettingsScreen(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onExportData = { backStack.add(Screen.Data) }
                )
            }

            entry<Screen.DetailedStats> {
                DetailedStatsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }

            entry<Screen.DrainStats> {
                DrainStatsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
        }
    )
}

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

    @Serializable
    data object DrainStats : Screen
}