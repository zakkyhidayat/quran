package io.zakkyhidayat.quran.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import io.zakkyhidayat.quran.settings.ContrastLevel

internal fun originalColorScheme(dark: Boolean, contrast: ContrastLevel): ColorScheme = when (contrast) {
    ContrastLevel.Standard -> if (dark) OriginalDarkStandard else OriginalLightStandard
    ContrastLevel.Medium -> if (dark) OriginalDarkMedium else OriginalLightMedium
    ContrastLevel.High -> if (dark) OriginalDarkHigh else OriginalLightHigh
}

internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0F0F),
    surfaceContainer = Color(0xFF101615),
    surfaceContainerHigh = Color(0xFF181E1D),
    surfaceContainerHighest = Color(0xFF212827),
)
