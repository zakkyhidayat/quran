package io.zakkyhidayat.quran.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/** Menjaga layar tetap menyala selama komposabel ini tampil dan [enabled]; dikembalikan saat keluar. */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        if (enabled) view.keepScreenOn = true
        onDispose { if (enabled) view.keepScreenOn = false }
    }
}
