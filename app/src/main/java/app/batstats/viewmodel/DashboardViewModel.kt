package app.batstats.battery.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.*
import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.db.SessionType
import app.batstats.battery.service.BatteryMonitorService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(private val app: Application) : AndroidViewModel(app) {
    val realtime = BatteryGraph.repo.realtime
    val activeSession = liveData {
        emitSource(BatteryGraph.db.sessionDao().sessionDaoActiveAsLive())
    }

    val recentSamples = BatteryGraph.repo.samplesBetween(
        System.currentTimeMillis() - 15*60*1000L, System.currentTimeMillis()
    ).map { it }

    fun startOrBindService() {
        app.startForegroundService(Intent(app, BatteryMonitorService::class.java))
    }

    fun stopService() {
        app.stopService(Intent(app, BatteryMonitorService::class.java))
    }

    fun startManualSession() = viewModelScope.launch {
        BatteryGraph.repo.startSessionIfNone(SessionType.DISCHARGE)
    }

    fun endSession() = viewModelScope.launch {
        BatteryGraph.repo.completeActiveSession()
    }

    companion object {
        fun factory() = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = BatteryGraph.app
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(app) as T
            }
        }
    }
}

// Helper extension to expose active session as LiveData
private fun app.batstats.battery.data.db.SessionDao.sessionDaoActiveAsLive() =
    androidx.lifecycle.liveData {
        emit(this@sessionDaoActiveAsLive.active())
    }
