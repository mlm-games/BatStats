package app.batstats.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.batstats.battery.data.BatteryRepository
import app.batstats.battery.data.db.BatterySample
import app.batstats.battery.data.db.ChargeSession
import app.batstats.battery.data.db.SessionType
import app.batstats.settings.AppSettings
import app.batstats.settings.AppSettingsSchema
import app.batstats.settings.chartTimeRangeMs
import io.github.mlmgames.settings.core.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repo: BatteryRepository,
    settingsRepository: SettingsRepository<AppSettings>
) : ViewModel() {

    private val settings: StateFlow<AppSettings> = settingsRepository.flow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsSchema.default
        )

    val realtime: StateFlow<BatteryRepository.Realtime> = repo.realtimeFlow
    val isMonitoring: StateFlow<Boolean> = repo.isMonitoringFlow

    val activeSession: StateFlow<ChargeSession?> = repo.activeSessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Recent samples based on settings chart time range
    @OptIn(ExperimentalCoroutinesApi::class)
    val recentSamples: Flow<List<BatterySample>> = settings
        .flatMapLatest { s ->
            repo.recentSamplesFlow(s.chartTimeRangeMs)
        }

    // Rolling buffer for the live chart (last 100 points)
    private val _liveCurrent = MutableStateFlow<List<Float>>(emptyList())
    val liveCurrent: StateFlow<List<Float>> = _liveCurrent.asStateFlow()

    init {
        // Collect realtime data to update the live chart
        viewModelScope.launch {
            realtime.collectLatest { rt ->
                val current = rt.currentMa.toFloat()
                _liveCurrent.update { history ->
                    (history + current).takeLast(100)
                }
            }
        }
    }

    fun toggleMonitoring() {
        if (isMonitoring.value) {
            repo.stopSampling()
        } else {
            repo.startSampling()
        }
    }

    fun startManualSession(type: SessionType) {
        viewModelScope.launch {
            repo.startSession(type)
        }
    }

    fun endSession() {
        viewModelScope.launch {
            repo.endCurrentSession()
        }
    }
}