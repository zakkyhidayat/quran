package io.zakkyhidayat.quran.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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

    var scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        else -> originalColorScheme(dark, settings.contrast)
    }
    if (dark) scheme = if (settings.amoled) scheme.toAmoled() else scheme.liftedDark()

    val reduce = ReduceMotion.systemAnimationsOff(context)
    SideEffect { ReduceMotion.enabled = reduce }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
