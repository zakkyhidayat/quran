package io.zakkyhidayat.quran.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

// Mode gelap non-AMOLED: permukaan tidak sehitam skema bawaan (sekitar #31 39 38 untuk latar), supaya teks Arab berwarna
// tidak terlalu kontras dan mata tidak lelah. AMOLED tetap hitam pekat lewat toAmoled().
internal fun ColorScheme.liftedDark(): ColorScheme {
    fun lift(base: Color, amount: Float) = lerp(base, onSurface, amount)
    return copy(
        background = lift(background, 0.17f),
        surface = lift(surface, 0.17f),
        surfaceDim = lift(surfaceDim, 0.14f),
        surfaceBright = lift(surfaceBright, 0.18f),
        surfaceContainerLowest = lift(surfaceContainerLowest, 0.14f),
        surfaceContainerLow = lift(surfaceContainerLow, 0.19f),
        surfaceContainer = lift(surfaceContainer, 0.21f),
        surfaceContainerHigh = lift(surfaceContainerHigh, 0.25f),
        surfaceContainerHighest = lift(surfaceContainerHighest, 0.29f),
    )
}
