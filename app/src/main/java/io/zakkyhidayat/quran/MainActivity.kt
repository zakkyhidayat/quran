package io.zakkyhidayat.quran

import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.remember
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.ThemeMode
import io.zakkyhidayat.quran.ui.theme.QuranTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra("page")) vm.goToPage(intent.getIntExtra("page", 1))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.hasExtra("page")) vm.goToPage(intent.getIntExtra("page", 1))
        setContent {
            // Posisi baca berubah setiap ganti halaman; UI tidak memakainya setelah layar dibuka, jadi perubahan yang hanya
            // menyangkut posisi tidak memicu render ulang seluruh aplikasi.
            val loaded by remember {
                vm.settingsRepository.settings.distinctUntilChanged { old, new ->
                    old.copy(lastPage = 0, lastSurah = 0, lastAyah = 0) == new.copy(lastPage = 0, lastSurah = 0, lastAyah = 0)
                }
            }.collectAsStateWithLifecycle(null)
            val settings: AppSettings = loaded ?: return@setContent
            val dark = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            QuranTheme(settings) {
              androidx.compose.runtime.CompositionLocalProvider(
                io.zakkyhidayat.quran.ui.LocalReadingTextScale provides io.zakkyhidayat.quran.ui.ReadingTextScale(
                    arabic = settings.arabicTextPercent / 100f,
                    translation = settings.translationTextPercent / 100f,
                ),
              ) {
                if (settings.onboardingDone || BuildConfig.LITE) {
                    AppNav(vm, settings)
                    io.zakkyhidayat.quran.ui.UpdateDialog(vm)
                } else {
                    io.zakkyhidayat.quran.onboarding.OnboardingScreen(vm, settings)
                }
              }
            }
        }
    }
}
