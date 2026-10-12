package io.zakkyhidayat.quran.onboarding

import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.PageLine
import io.zakkyhidayat.quran.reader.MushafPage
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.AppearanceControls
import io.zakkyhidayat.quran.settings.LanguageSection
import io.zakkyhidayat.quran.settings.TajweedToggle
import io.zakkyhidayat.quran.settings.TranslationControls
import io.zakkyhidayat.quran.ui.CenteredContent
import io.zakkyhidayat.quran.ui.FADE_IN_MS
import io.zakkyhidayat.quran.ui.FADE_OUT_MS
import io.zakkyhidayat.quran.ui.STEP_MS
import io.zakkyhidayat.quran.ui.StepEasing
import io.zakkyhidayat.quran.ui.StepSlide
import io.zakkyhidayat.quran.ui.sharedAxisXEnter
import io.zakkyhidayat.quran.ui.sharedAxisXExit
import kotlinx.coroutines.launch

private const val STEPS = 4

/**
 * Onboarding untuk pengguna baru: bahasa, tentang mushaf ini, terjemahan, tampilan. Setiap pilihan langsung tersimpan,
 * jadi "Lewati" aman kapan saja. Langkah disimpan dengan rememberSaveable karena ganti bahasa membuat ulang Activity.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(vm: AppViewModel, settings: AppSettings) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val slidePx = with(LocalDensity.current) { StepSlide.roundToPx() }
    val forward = if (androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl) -1 else 1
    val finish: () -> Unit = { scope.launch { vm.settingsRepository.setOnboardingDone() } }

    // Surface agar warna teks bawaan mengikuti tema (onBackground), bukan hitam.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        CenteredContent(Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Label langkah berganti sinkron dengan konten: pudar keluar 90 ms, pudar masuk tertunda.
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearEasing)) togetherWith
                                fadeOut(tween(FADE_OUT_MS, easing = LinearEasing)) using SizeTransform(clip = false) { _, _ -> tween(STEP_MS, easing = StepEasing) }
                        },
                        modifier = Modifier.padding(start = 16.dp).weight(1f),
                        label = "onboardingStepLabel",
                    ) { n ->
                        Text(
                            stringResource(R.string.onb_step, n + 1, STEPS),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        // Shared axis X tanpa tumpang tindih: keluar geser 30dp + pudar cepat (90 ms pertama),
                        // masuk geser dari 30dp + pudar baru setelah itu (tertunda 90 ms). Durasi/easing sama di kedua arah.
                        val dir = (if (targetState > initialState) 1 else -1) * forward
                        sharedAxisXEnter(dir, slidePx) togetherWith sharedAxisXExit(dir, slidePx)
                    },
                    modifier = Modifier.weight(1f),
                    label = "onboarding",
                ) { current ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
                        when (current) {
                            0 -> WelcomeStep()
                            1 -> MushafStep(vm, settings)
                            2 -> TranslationStep(vm, settings)
                            else -> AppearanceStep(vm, settings)
                        }
                    }
                }
                StepDots(step)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FadingButton(visible = step > 0, onClick = { step-- }) { Text(stringResource(R.string.back)) }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { if (step < STEPS - 1) step++ else finish() }) {
                        // Label tombol berganti dengan crossfade, bukan langsung.
                        AnimatedContent(
                            targetState = step < STEPS - 1,
                            transitionSpec = {
                                fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS, easing = LinearEasing)) togetherWith
                                    fadeOut(tween(FADE_OUT_MS, easing = LinearEasing)) using SizeTransform(clip = false) { _, _ -> tween(STEP_MS, easing = StepEasing) }
                            },
                            label = "onboardingNext",
                        ) { notLast -> Text(stringResource(if (notLast) R.string.onb_next else R.string.onb_start)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepTitle(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun ColumnScope.WelcomeStep() {
    Spacer(Modifier.height(24.dp))
    // Ikon aplikasi: latar hijau ikon adaptif + kaligrafi latar depan.
    Box(
        Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF176B4D)).align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(144.dp))
    }
    Spacer(Modifier.height(24.dp))
    // Salam di tengah dan tebal; penjelasan di bawahnya.
    Text(
        stringResource(R.string.onb_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
    )
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.onb_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(24.dp))
    LanguageSection(showTitle = false)
}

@Composable
private fun MushafStep(vm: AppViewModel, settings: AppSettings) {
    StepTitle(stringResource(R.string.onb_mushaf_title), stringResource(R.string.onb_mushaf_body))
    val lines by produceState<List<PageLine>?>(null) { value = vm.mushaf.page(1) }
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    // Pratinjau Al-Fatihah; ikut berubah saat sakelar tajwid di bawahnya diubah.
    // Proporsi halaman B5 seperti di layar baca, agar baris tidak berdesakan.
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth(0.8f).aspectRatio(176f / 250f)) {
            val previewSize = androidx.compose.ui.unit.DpSize(maxWidth, maxHeight)
            lines?.let {
                MushafPage(
                    pageSize = previewSize,
                    page = 1,
                    lines = it,
                    ayahTexts = emptyList(),
                    surahs = surahs,
                    selected = null,
                    onAyahClick = {},
                    tajweed = settings.tajweed,
                    onSurahClick = {},
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    TajweedToggle(vm, settings)
}

@Composable
private fun TranslationStep(vm: AppViewModel, settings: AppSettings) {
    StepTitle(stringResource(R.string.onb_translation_title), stringResource(R.string.onb_translation_body))
    TranslationControls(vm, settings, inlineCatalog = true)
}

@Composable
private fun AppearanceStep(vm: AppViewModel, settings: AppSettings) {
    StepTitle(stringResource(R.string.onb_appearance_title), stringResource(R.string.onb_appearance_body))
    AppearanceControls(vm, settings)
}

/** Tombol teks yang memudar masuk/keluar dengan slot tetap; saat tidak terlihat tidak bisa difokus, diklik, atau dibaca TalkBack. */
@Composable
private fun FadingButton(visible: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(STEP_MS, easing = StepEasing), label = "fadingButton")
    TextButton(
        onClick = { if (visible) onClick() },
        modifier = Modifier
            .alpha(alpha)
            .focusProperties { canFocus = visible }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics { }),
    ) { content() }
}

@Composable
private fun StepDots(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        repeat(STEPS) { i ->
            val active = i == step
            val width by animateDpAsState(if (active) 24.dp else 8.dp, tween(STEP_MS, easing = StepEasing), label = "dotWidth")
            val color by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                tween(STEP_MS, easing = StepEasing), label = "dotColor",
            )
            Box(Modifier.size(width = width, height = 8.dp).clip(CircleShape).background(color))
        }
    }
}
