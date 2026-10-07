package io.zakkyhidayat.quran.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val OriginalLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF74F8E5),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A6460),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E3),
    onSecondaryContainer = Color(0xFF05201D),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF),
    onTertiaryContainer = Color(0xFF001E31),
    background = Color(0xFFF4FBF9),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF4FBF9),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F3),
    surfaceContainer = Color(0xFFE9EFED),
    surfaceContainerHigh = Color(0xFFE3EAE8),
    surfaceContainerHighest = Color(0xFFDDE4E2),
)

internal val OriginalDark: ColorScheme = darkColorScheme(
    primary = Color(0xFF53DBC9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF74F8E5),
    secondary = Color(0xFFB1CCC7),
    onSecondary = Color(0xFF1C3532),
    secondaryContainer = Color(0xFF334B48),
    onSecondaryContainer = Color(0xFFCCE8E3),
    tertiary = Color(0xFFADCAE6),
    onTertiary = Color(0xFF133349),
    tertiaryContainer = Color(0xFF2D4961),
    onTertiaryContainer = Color(0xFFCCE5FF),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDDE4E2),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color(0xFF090F0F),
    surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120),
    surfaceContainerHigh = Color(0xFF252B2B),
    surfaceContainerHighest = Color(0xFF2F3636),
)

internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0F0F),
    surfaceContainer = Color(0xFF101615),
    surfaceContainerHigh = Color(0xFF181E1D),
    surfaceContainerHighest = Color(0xFF212827),
)
