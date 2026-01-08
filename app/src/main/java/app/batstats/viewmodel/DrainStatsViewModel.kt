package app.batstats.viewmodel

import androidx.lifecycle.ViewModel
import app.batstats.battery.drain.AdvancedDrainTracker
import app.batstats.battery.drain.DrainSnapshot
import app.batstats.battery.drain.DrainState
import kotlinx.coroutines.flow.StateFlow

class DrainStatsViewModel(
    private val drainTracker: AdvancedDrainTracker
) : ViewModel() {
    
    val drainState: StateFlow<DrainState> = drainTracker.drainState
    val snapshots: StateFlow<List<DrainSnapshot>> = drainTracker.snapshots
    val isTracking: StateFlow<Boolean> = drainTracker.isTracking
    
    fun resetSession() {
        drainTracker.resetSession()
    }
    
    fun startTracking() {
        drainTracker.start()
    }
    
    fun stopTracking() {
        drainTracker.stop()
    }
}