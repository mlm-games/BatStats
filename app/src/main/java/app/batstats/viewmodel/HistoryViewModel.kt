package app.batstats.battery.viewmodel

import androidx.lifecycle.*
import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.db.ChargeSession
import kotlinx.coroutines.flow.map

class HistoryViewModel : ViewModel() {
    val sessions = BatteryGraph.repo.sessionsPaged(100, 0).map { it }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryViewModel() as T
        }
    }
}
