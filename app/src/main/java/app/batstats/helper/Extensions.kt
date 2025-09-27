package app.batstats.battery.util

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.Context

fun Int.daysInMs(): Long = this * 24L * 3600000L

fun batteryIntentFilter() = IntentFilter(Intent.ACTION_BATTERY_CHANGED)