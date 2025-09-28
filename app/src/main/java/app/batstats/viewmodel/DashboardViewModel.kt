package app.batstats.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.batstats.battery.BatteryGraph
import app.batstats.battery.data.db.ChargeSession
import app.batstats.battery.data.db.SessionType
import app.batstats.battery.service.BatteryMonitorService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(private val app: Application) : AndroidViewModel(app) {
    val realtime = BatteryGraph.repo.realtime
    val isMonitoring = BatteryGraph.repo.isSampling
    val activeSession: StateFlow<ChargeSession?> =
        BatteryGraph.db.sessionDao().activeFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentSamples =
        BatteryGraph.repo.samplesBetween(
            System.currentTimeMillis() - 15 * 60 * 1000L,
            System.currentTimeMillis()
        ).map { it }

    private val _liveCurrent = MutableStateFlow<List<Float>>(emptyList())
    val liveCurrent: StateFlow<List<Float>> = _liveCurrent.asStateFlow()

    init {
        viewModelScope.launch {
            BatteryGraph.repo.realtime.collect { rt ->
                val ma = ((rt.sample?.currentNowUa ?: 0L) / 1000f)
                val next = (_liveCurrent.value + ma).takeLast(180)
                _liveCurrent.value = next
            }
        }
    }

    fun startOrBindService() {
        val ctx = getApplication<Application>()
        ctx.startForegroundService(Intent(ctx, BatteryMonitorService::class.java))
    }

    fun stopService() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, BatteryMonitorService::class.java))
    }

    fun toggleMonitoring() {
        if (isMonitoring.value) stopService() else startOrBindService()
    }

    fun startManualSession(type: SessionType) {
        viewModelScope.launch { BatteryGraph.repo.startSessionIfNone(type) }
    }

    fun endSession() {
        viewModelScope.launch { BatteryGraph.repo.completeActiveSession() }
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