package io.zakkyhidayat.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpToAyahDialog(
    surahs: Map<Int, Surah>,
    initial: AyahRef?,
    onDismiss: () -> Unit,
    onJump: (surah: Int, ayah: Int) -> Unit,
) {
    var surahId by rememberSaveable { mutableIntStateOf(initial?.surah ?: 1) }
    var ayahText by rememberSaveable { mutableStateOf((initial?.ayah ?: 1).toString()) }
    var expanded by remember { mutableStateOf(false) }
    val surah = surahs[surahId]
    val ayah = ayahText.toIntOrNull()
    val valid = surah != null && ayah != null && ayah in 1..surah.ayahCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lompat ke ayat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = surah?.let { "${it.id}. ${it.nameLatin}" }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Surah") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        surahs.values.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.id}. ${s.nameLatin}") },
                                onClick = {
                                    surahId = s.id
                                    expanded = false
                                    if ((ayahText.toIntOrNull() ?: 0) > s.ayahCount) ayahText = "1"
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = ayahText,
                    onValueChange = { ayahText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Ayat") },
                    supportingText = { Text(if (surah != null) "1–${surah.ayahCount}" else "") },
                    isError = ayahText.isNotEmpty() && !valid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (valid) onJump(surahId, ayah!!) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onJump(surahId, ayah!!) }) { Text("Lompat") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
