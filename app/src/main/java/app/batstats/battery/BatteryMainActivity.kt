package app.batstats.battery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.batstats.settings.AppSettings
import app.batstats.settings.applyAppLocale
import app.batstats.ui.screens.MainScreen
import app.batstats.ui.theme.MainTheme
import io.github.mlmgames.settings.core.SettingsRepository
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class BatteryMainActivity : AppCompatActivity() {
    private val settingsRepository: SettingsRepository<AppSettings> by inject()

    private val notifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            settingsRepository.flow.collect { settings ->
                applyAppLocale(settings.language.languageTag)
            }
        }

        setContent {
            val settings by settingsRepository.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeIndex) {
                1 -> false
                2 -> true
                else -> isSystemDark
            }
            MainTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColors,
                useAuroraTheme = !settings.dynamicColors,
                oledBlack = settings.oledBlack
            ) {
                MainScreen()
            }
        }
    }
}
