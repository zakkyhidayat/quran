package io.zakkyhidayat.quran.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Skema warna untuk halaman pembaca bila pengguna memilih nada halaman yang berlawanan dengan tema aplikasi (palet,
 * kontras, dan AMOLED yang sama, hanya terang atau gelapnya yang lain). Null berarti halaman mengikuti tema.
 */
val LocalPageScheme = compositionLocalOf<ColorScheme?> { null }

/**
 * Menjalankan [content] di bawah skema warna halaman bila dipilih, sehingga seluruh pembaca (halaman mushaf, mode daftar,
 * bilah atas, indikator memuat) sama-sama memakai nada yang dipilih tanpa tepi atau persegi yang tak serasi. Bentuk,
 * tipografi, dan gerak tetap dari tema aplikasi.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageThemeScope(content: @Composable () -> Unit) {
    val scheme = LocalPageScheme.current
    if (scheme == null) {
        content()
        return
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MaterialTheme.motionScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}
