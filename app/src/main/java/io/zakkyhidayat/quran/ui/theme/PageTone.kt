package io.zakkyhidayat.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.luminance

/** Warna halaman mushaf yang dipilih pengguna (true gelap, false terang); null berarti mengikuti tema aplikasi. */
val LocalPageDark = compositionLocalOf<Boolean?> { null }

/** Apakah halaman mushaf ditampilkan gelap: pilihan pengguna, atau gelapnya latar tema bila belum dipilih. */
@Composable
fun mushafPageDark(): Boolean = LocalPageDark.current ?: (MaterialTheme.colorScheme.background.luminance() < 0.5f)
