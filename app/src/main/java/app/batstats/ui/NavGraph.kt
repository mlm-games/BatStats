package app.batstats.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import app.batstats.ui.util.Screen
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
                    onOpenDetailedStats = { backStack.add(Screen.DetailedStats) }
                )
            }

            // History
            entry<Screen.History> {
                HistoryScreen(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onOpenSession = { id -> backStack.add(Screen.SessionDetails(id)) }
                )
            }

            // Session Details (With Arguments)
            entry<Screen.SessionDetails> { args ->
                SessionDetailsScreen(
                    sessionId = args.sessionId,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    // Inject VM with parameters
                    vm = koinViewModel(parameters = { parametersOf(args.sessionId) })
                )
            }

            // Alarms
            entry<Screen.Alarms> {
                AlarmsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }

            // Data (Import/Export)
            entry<Screen.Data> {
                DataScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }

            // Settings
            entry<Screen.Settings> {
                BatterySettingsScreen(
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onExportData = { backStack.add(Screen.Data) }
                )
            }

            // Detailed Stats
            entry<Screen.DetailedStats> {
                DetailedStatsScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
            }
        }
    )
}