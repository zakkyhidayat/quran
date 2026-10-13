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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.data.QuranDatabaseInstaller
import io.zakkyhidayat.quran.onboarding.DatabaseSetupScreen
import io.zakkyhidayat.quran.onboarding.OnboardingScreen
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.ThemeMode
import io.zakkyhidayat.quran.ui.theme.QuranTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private var databaseReady = false
    private var pendingPage: Int? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasExtra("page")) {
            val page = intent.getIntExtra("page", 1)
            if (databaseReady) vm.goToPage(page) else pendingPage = page
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.hasExtra("page")) pendingPage = intent.getIntExtra("page", 1)
        setContent {
            // Posisi baca berubah setiap ganti halaman; UI tidak memakainya setelah layar dibuka, jadi perubahan yang hanya
            // menyangkut posisi tidak memicu render ulang seluruh aplikasi.
            val appSettings = remember { (application as QuranApp).settings }
            val loaded by remember {
                appSettings.settings.distinctUntilChanged { old, new ->
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
                val installer = remember { QuranDatabaseInstaller(applicationContext) }
                var installed by remember { mutableStateOf<Boolean?>(null) }
                androidx.compose.runtime.LaunchedEffect(installer) {
                    installed = try {
                        installer.hasInstalledDatabase()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        android.util.Log.e("QuranDatabase", "Gagal memeriksa database lokal", e)
                        false
                    }
                }
                androidx.compose.runtime.LaunchedEffect(installed) {
                    if (installed == true) {
                        databaseReady = true
                        pendingPage?.let { page ->
                            vm.goToPage(page)
                            pendingPage = null
                        }
                    }
                }
                when {
                    installed == null -> androidx.compose.material3.Surface(
                        Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator()
                        }
                    }
                    installed == false -> DatabaseSetupScreen(installer) { installed = true }
                    !settings.onboardingDone -> OnboardingScreen(vm, settings)
                    else -> AppNav(vm, settings)
                }
              }
            }
        }
    }
}
