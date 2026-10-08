package io.zakkyhidayat.quran.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.data.SurahDetails
import io.zakkyhidayat.quran.reader.surahNameFontFamily
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.HtmlContent
import io.zakkyhidayat.quran.ui.parseAyahLink

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SurahInfoScreen(vm: AppViewModel, surahId: Int, onBack: () -> Unit, onOpenAyah: (Int, Int) -> Unit, onOpenPage: (Int) -> Unit) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val surah = surahs[surahId]
    var lang by rememberSaveable { mutableStateOf("id") }
    val details by produceState<SurahDetails?>(null, surahId, lang) { value = vm.mushaf.surahDetails(surahId, lang) }
    val font = remember { surahNameFontFamily(vm.getApplication()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surah?.nameLatin.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") } },
            )
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (surah != null) {
                    Text(
                        surah.nameGlyph.toString(),
                        style = TextStyle(fontFamily = font, fontSize = 40.sp, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
                    )
                }
                details?.let { d ->
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        d.place?.let { AssistChip(onClick = {}, label = { Text(if (it == "makki") "Makkiyah" else "Madaniyah") }) }
                        AssistChip(onClick = {}, label = { Text("${d.ayahCount} ayat") })
                        AssistChip(onClick = {}, label = { Text(if (d.firstPage == d.lastPage) "Hal. ${d.firstPage}" else "Hal. ${d.firstPage}–${d.lastPage}") })
                        AssistChip(onClick = {}, label = { Text(if (d.juzFrom == d.juzTo) "Juz ${d.juzFrom}" else "Juz ${d.juzFrom}–${d.juzTo}") })
                        AssistChip(onClick = {}, label = { Text("${d.rukuCount} ruku") })
                        d.revelationOrder?.let { AssistChip(onClick = {}, label = { Text("Turun ke-$it") }) }
                    }
                    FilledTonalButton(
                        onClick = { onOpenPage(d.firstPage) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Icon(AppIcons.MenuBook, contentDescription = null)
                        Text("  Baca surah ini")
                    }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    listOf("id" to "Indonesia", "en" to "English").forEachIndexed { i, (code, label) ->
                        SegmentedButton(
                            selected = lang == code,
                            onClick = { lang = code },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            icon = { Icon(AppIcons.Translate, contentDescription = null, modifier = Modifier.padding(0.dp)) },
                        ) { Text(label) }
                    }
                }
                details?.let { d ->
                    if (d.infoHtml.isBlank()) {
                        Text("Belum ada informasi untuk surah ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        HtmlContent(d.infoHtml) { href ->
                            parseAyahLink(href)?.let { (s, a) -> onOpenAyah(s, a) }
                        }
                    }
                }
                Text(
                    "Sumber: Quranic Universal Library (Tarteel).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                )
            }
        }
    }
}
