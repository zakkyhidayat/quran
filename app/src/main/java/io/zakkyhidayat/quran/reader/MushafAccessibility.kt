package io.zakkyhidayat.quran.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.AyahRef
import io.zakkyhidayat.quran.data.AyahText
import io.zakkyhidayat.quran.data.LineType
import io.zakkyhidayat.quran.data.PageLine
import io.zakkyhidayat.quran.data.Surah

// Satu simpul aksesibilitas per surah dan per ayat dengan teks Arab Unicode; ketuk dua kali membuka terjemahan.
@Composable
internal fun ScreenReaderLayer(
    lines: List<PageLine>,
    ayahTexts: List<AyahText>,
    surahs: Map<Int, Surah>,
    onAyahClick: (AyahRef) -> Unit,
    onSurahClick: (Int) -> Unit,
) {
    val resources = LocalResources.current
    val byRef = remember(ayahTexts) { ayahTexts.associateBy { AyahRef(it.surah, it.ayah) } }
    val seen = HashSet<AyahRef>()
    Column {
        lines.forEach { line ->
            when (line.type) {
                LineType.SurahName -> surahs[line.surah]?.let { surah ->
                    Box(
                        Modifier.size(1.dp).semantics {
                            heading()
                            contentDescription = resources.getString(R.string.surah_cd, surah.nameLatin)
                            onClick(label = resources.getString(R.string.open_surah_info)) { onSurahClick(surah.id); true }
                        },
                    )
                }
                LineType.Ayah -> line.words.forEach { word ->
                    val ref = AyahRef(word.surah, word.ayah)
                    val text = byRef[ref]
                    if (text != null && seen.add(ref)) {
                        val name = surahs[ref.surah]?.nameLatin.orEmpty()
                        Box(
                            Modifier.size(1.dp).semantics {
                                role = Role.Button
                                contentDescription = resources.getString(R.string.ayah_cd, name, ref.ayah, text.text)
                                onClick(label = resources.getString(R.string.show_translation)) { onAyahClick(ref); true }
                            },
                        )
                    }
                }
                LineType.Basmallah -> Box(Modifier.size(1.dp).semantics { contentDescription = "Bismillahirrahmanirrahim" })
            }
        }
    }
}

/** true selama layanan eksplorasi sentuh (TalkBack dan sejenisnya) aktif; ikut berubah saat dinyalakan/dimatikan. */
@Composable
internal fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(android.view.accessibility.AccessibilityManager::class.java) }
    var enabled by remember { mutableStateOf(manager.isTouchExplorationEnabled) }
    DisposableEffect(manager) {
        val listener = android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager.addTouchExplorationStateChangeListener(listener)
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}
