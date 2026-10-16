package io.zakkyhidayat.quran.home

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.R

/**
 * Pilihan saat pengguna menekan atau menggeser Back di pembaca yang dibuka dari kumpulan ayat (doa, solusi, adab, dosa
 * besar): kembali ke kumpulan yang sedang dipilih, atau ke halaman utama. Menutup dialog (ketuk di luar) tetap di pembaca.
 */
@Composable
fun DoaBackDialog(
    passageTitle: String,
    onBackToCollection: () -> Unit,
    onBackToMain: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.doa_back_title)) },
        text = { Text(stringResource(R.string.doa_back_text, passageTitle)) },
        confirmButton = { TextButton(onClick = onBackToCollection) { Text(stringResource(R.string.doa_back_collection)) } },
        dismissButton = { TextButton(onClick = onBackToMain) { Text(stringResource(R.string.doa_back_main)) } },
    )
}
