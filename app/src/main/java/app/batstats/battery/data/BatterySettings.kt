package app.batstats.battery.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

private val Context.batteryDs by preferencesDataStore("battery.settings")

data class BatterySettings(
    val monitorOnBoot: Boolean = true,
    val monitorWhileChargingOnly: Boolean = true,
    val sampleIntervalSec: Int = 15,      // 5..60
    val keepHistoryDays: Int = 45,
    val chargeLimitPercent: Int = 80,
    val tempHighC: Int = 45,
    val dischargeHighMa: Int = 600,       // alert when |current| > threshold
    val autoStartSessionOnPlug: Boolean = true,
    val autoStopSessionOnUnplug: Boolean = true
)

class BatterySettingsRepository(private val context: Context) {
    private object Keys {
        val MON_BOOT = booleanPreferencesKey("mon_boot")
        val MON_CHG_ONLY = booleanPreferencesKey("mon_chg_only")
        val SAMPLE_SEC = intPreferencesKey("sample_sec")
        val KEEP_DAYS = intPreferencesKey("keep_days")
        val LIMIT = intPreferencesKey("limit_pct")
        val TEMP = intPreferencesKey("temp_c")
        val DISCH = intPreferencesKey("discharge_ma")
        val AUTO_START = booleanPreferencesKey("auto_start_session")
        val AUTO_STOP = booleanPreferencesKey("auto_stop_session")
    }

    val flow = context.batteryDs.data.map { p ->
        BatterySettings(
            monitorOnBoot = p[Keys.MON_BOOT] ?: true,
            monitorWhileChargingOnly = p[Keys.MON_CHG_ONLY] ?: true,
            sampleIntervalSec = p[Keys.SAMPLE_SEC] ?: 15,
            keepHistoryDays = p[Keys.KEEP_DAYS] ?: 45,
            chargeLimitPercent = p[Keys.LIMIT] ?: 80,
            tempHighC = p[Keys.TEMP] ?: 45,
            dischargeHighMa = p[Keys.DISCH] ?: 600,
            autoStartSessionOnPlug = p[Keys.AUTO_START] ?: true,
            autoStopSessionOnUnplug = p[Keys.AUTO_STOP] ?: true
        )
    }.distinctUntilChanged()

    suspend fun update(block: (BatterySettings) -> BatterySettings) {
        val cur = flow.first()
        val upd = block(cur)
        context.batteryDs.edit { p ->
            p[Keys.MON_BOOT] = upd.monitorOnBoot
            p[Keys.MON_CHG_ONLY] = upd.monitorWhileChargingOnly
            p[Keys.SAMPLE_SEC] = upd.sampleIntervalSec
            p[Keys.KEEP_DAYS] = upd.keepHistoryDays
            p[Keys.LIMIT] = upd.chargeLimitPercent
            p[Keys.TEMP] = upd.tempHighC
            p[Keys.DISCH] = upd.dischargeHighMa
            p[Keys.AUTO_START] = upd.autoStartSessionOnPlug
            p[Keys.AUTO_STOP] = upd.autoStopSessionOnUnplug
        }
    }
}