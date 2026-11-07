package io.zakkyhidayat.quran.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.ColorMode
import io.zakkyhidayat.quran.settings.ThemeMode
import io.zakkyhidayat.quran.ui.ReduceMotion

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuranTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val dynamic = settings.colorMode == ColorMode.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun schemeFor(isDark: Boolean): ColorScheme {
        val base = when {
            dynamic && isDark -> dynamicDarkColorScheme(context)
            dynamic -> dynamicLightColorScheme(context)
            else -> originalColorScheme(isDark, settings.contrast)
        }
        return if (!isDark) base else if (settings.amoled) base.toAmoled() else base.liftedDark()
    }
    val scheme = schemeFor(dark)
    // Halaman pembaca yang berlawanan dengan tema memakai palet yang sama dalam nada lain; sama dengan tema berarti ikut tema.
    val pageScheme = remember(dark, settings.pageDark, settings.colorMode, settings.contrast, settings.amoled) {
        settings.pageDark?.takeIf { it != dark }?.let { schemeFor(it) }
    }

    val reduce = ReduceMotion.systemAnimationsOff(context)
    SideEffect { ReduceMotion.enabled = reduce }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = {
            CompositionLocalProvider(LocalPageScheme provides pageScheme, content = content)
        },
    )
}
