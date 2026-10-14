package io.zakkyhidayat.quran.info

import io.zakkyhidayat.quran.ui.InfoLabel
import androidx.compose.ui.res.stringResource
import io.zakkyhidayat.quran.R
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import android.text.format.Formatter
import io.zakkyhidayat.quran.data.ContentPack
import io.zakkyhidayat.quran.data.ContentKind
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
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
import io.zakkyhidayat.quran.AppLanguage
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
    val appLang = AppLanguage.effective(androidx.compose.ui.platform.LocalContext.current)
    // Info surah selain Inggris adalah paket unduhan. Bahasa aplikasi yang paketnya terpasang: pilih antara bahasa itu dan
    // Inggris. Bahasa aplikasi yang paketnya ada di katalog tapi belum terpasang: tawarkan unduhan. Bahasa lain: Inggris.
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val available by produceState<List<String>?>(null, refresh) { value = vm.mushaf.surahInfoLanguages() }
    val offer by produceState<ContentPack?>(null, available) {
        value = if (appLang != "en" && available?.contains(appLang) == false) {
            runCatching { vm.mushaf.contentCatalog(ContentKind.SurahInfo) }.getOrNull()?.firstOrNull { it.lang == appLang }
        } else null
    }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var failed by remember { mutableStateOf(false) }
    val choices = if (appLang != "en" && available?.contains(appLang) == true) listOf(appLang, "en") else listOf("en")
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val lang = picked?.takeIf { it in choices } ?: choices.first()
    val details by produceState<SurahDetails?>(null, surahId, lang, available) { if (available != null) value = vm.mushaf.surahDetails(surahId, lang) }
    val font = remember { surahNameFontFamily(vm.getApplication()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surah?.nameLatin.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        d.place?.let { InfoLabel(stringResource(if (it == "makki") R.string.place_makki else R.string.place_madani)) }
                        InfoLabel(stringResource(R.string.ayah_count, d.ayahCount))
                        InfoLabel(if (d.firstPage == d.lastPage) stringResource(R.string.page_short, d.firstPage) else stringResource(R.string.pages_range, d.firstPage, d.lastPage))
                        InfoLabel(if (d.juzFrom == d.juzTo) stringResource(R.string.juz_n, d.juzFrom) else stringResource(R.string.juz_range, d.juzFrom, d.juzTo))
                        InfoLabel(stringResource(R.string.ruku_count, d.rukuCount))
                        d.revelationOrder?.let { InfoLabel(stringResource(R.string.revelation_order, it)) }
                    }
                    FilledTonalButton(
                        onClick = { onOpenPage(d.firstPage) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Icon(AppIcons.MenuBook, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.read_surah))
                    }
                }
                // Nama bahasa dari Locale (dalam bahasa UI), jadi bahasa info baru tidak butuh string tambahan.
                val uiLocale = java.util.Locale.forLanguageTag(appLang)
                if (choices.size > 1) {
                    FlowRow(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        choices.forEach { code ->
                            FilterChip(
                                selected = lang == code,
                                onClick = { picked = code },
                                label = { Text(java.util.Locale.forLanguageTag(code).getDisplayLanguage(uiLocale).replaceFirstChar { it.titlecase(uiLocale) }) },
                            )
                        }
                    }
                } else if (offer != null) {
                    val pack = offer!!
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        OutlinedButton(
                            enabled = !downloading,
                            onClick = {
                                downloading = true
                                failed = false
                                progress = 0f
                                scope.launch {
                                    try {
                                        vm.mushaf.installContent(ContentKind.SurahInfo, pack) { done, total ->
                                            if (total > 0) progress = done.toFloat() / total
                                        }
                                        picked = pack.lang
                                        refresh++
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        throw e
                                    } catch (_: Exception) {
                                        failed = true
                                    } finally {
                                        downloading = false
                                    }
                                }
                            },
                        ) {
                            Text(stringResource(R.string.surah_info_download, java.util.Locale.forLanguageTag(pack.lang).getDisplayLanguage(uiLocale), Formatter.formatShortFileSize(context, pack.bytes)))
                        }
                        if (downloading) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        if (failed) Text(stringResource(R.string.surah_info_download_failed), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                } else {
                    Spacer(Modifier.size(12.dp))
                }
                details?.let { d ->
                    if (d.infoHtml.isBlank()) {
                        Text(stringResource(R.string.no_surah_info), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        // Info Urdu (dan bahasa RTL lain) harus rata kanan dengan arah teks kanan-ke-kiri.
                        val rtl = TextUtilsCompat.getLayoutDirectionFromLocale(java.util.Locale.forLanguageTag(lang)) == ViewCompat.LAYOUT_DIRECTION_RTL
                        CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                            HtmlContent(d.infoHtml) { href ->
                                parseAyahLink(href)?.let { (s, a) -> onOpenAyah(s, a) }
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.source_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                )
            }
        }
    }
}
