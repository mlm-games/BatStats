package app.batstats.viewmodel

import androidx.lifecycle.ViewModel
import app.batstats.battery.data.BatteryRepository
import kotlinx.coroutines.flow.map

class HistoryViewModel(
    repo: BatteryRepository
) : ViewModel() {
    val sessions = repo.sessionDao.sessionsPaged(100, 0).map { it }
}