package io.zakkyhidayat.quran.settings

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.ui.AppIcons

/** Nama dan ikon mode tema; dipakai Pengaturan > Tampilan dan tombol tema di bilah atas pembaca. */
@StringRes
internal fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

internal fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.System -> AppIcons.BrightnessAuto
    ThemeMode.Light -> AppIcons.LightMode
    ThemeMode.Dark -> AppIcons.DarkMode
}
