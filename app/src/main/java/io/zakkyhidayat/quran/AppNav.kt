package io.zakkyhidayat.quran

import io.zakkyhidayat.quran.settings.SettingsPage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import io.zakkyhidayat.quran.ui.StepSlide
import io.zakkyhidayat.quran.ui.PredictiveShift
import io.zakkyhidayat.quran.ui.PredictiveBackFrame
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import io.zakkyhidayat.quran.ui.predictivePopEnter
import io.zakkyhidayat.quran.ui.predictivePopExit
import io.zakkyhidayat.quran.ui.sharedAxisXEnter
import io.zakkyhidayat.quran.ui.sharedAxisXExit
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.zakkyhidayat.quran.data.CollectionKind
import io.zakkyhidayat.quran.explore.ExploreScreen
import io.zakkyhidayat.quran.explore.PhraseScreen
import io.zakkyhidayat.quran.explore.PhrasesScreen
import io.zakkyhidayat.quran.explore.RootScreen
import io.zakkyhidayat.quran.explore.RootsScreen
import io.zakkyhidayat.quran.explore.SimilarDetailScreen
import io.zakkyhidayat.quran.explore.SimilarScreen
import io.zakkyhidayat.quran.explore.SurahThemesScreen
import io.zakkyhidayat.quran.explore.ThemesScreen
import io.zakkyhidayat.quran.explore.TopicScreen
import io.zakkyhidayat.quran.home.CollectionScreen
import io.zakkyhidayat.quran.home.HomeTarget
import io.zakkyhidayat.quran.home.PassageScreen
import io.zakkyhidayat.quran.index.ExploreEntry
import io.zakkyhidayat.quran.info.SurahInfoScreen
import io.zakkyhidayat.quran.index.IndexScreen
import io.zakkyhidayat.quran.reader.ReaderScreen
import io.zakkyhidayat.quran.search.SearchScreen
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.SettingsMainSheet
import io.zakkyhidayat.quran.settings.SettingsScreen

// Lebar jendela "expanded" M3: daftar surah tampil permanen di samping pembaca.
private const val EXPANDED_WIDTH_DP = 840

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNav(vm: AppViewModel, settings: AppSettings) {
    val nav = rememberNavController()
    var settingsSheetVisible by rememberSaveable { mutableStateOf(false) }
    val slidePx = with(LocalDensity.current) { StepSlide.roundToPx() }
    val shiftPx = with(LocalDensity.current) { PredictiveShift.roundToPx() }
    val expanded = LocalConfiguration.current.screenWidthDp >= EXPANDED_WIDTH_DP
    val forward = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
    val openSettings: () -> Unit = { settingsSheetVisible = true }

    // Di ponsel, layar awal adalah daftar bertab; layar baca dibuka dari sana. Di layar lebar daftar tampil permanen di
    // samping, jadi layar baca menjadi layar awal.
    val start = if (expanded) "reader" else "index"
    val openReader: () -> Unit = {
        if (nav.currentDestination?.route != "reader") {
            nav.navigate("reader") {
                popUpTo(start) { inclusive = start == "reader" }
                launchSingleTop = true
            }
        }
    }
    val openHome: (HomeTarget) -> Unit = { target ->
        when (target) {
            is HomeTarget.Passage -> { vm.passage.value = target; nav.navigate("passage") }
            is HomeTarget.Collection -> nav.navigate("collection/${target.kind.name}")
            is HomeTarget.Sunnah -> { vm.startSunnahReading(target.reading); openReader() }
        }
    }
    val onOpenExplore: (ExploreEntry) -> Unit = {
        nav.navigate(
            when (it) {
                ExploreEntry.Topics -> "explore"
                ExploreEntry.Themes -> "themes"
                ExploreEntry.Phrases -> "phrases"
                ExploreEntry.Similar -> "similar"
                ExploreEntry.Roots -> "roots"
            },
        )
    }
    // Ketuk ayat di layar Jelajahi membuka pembaca di ayat itu (sama seperti TopicScreen).
    val openAyah: (Int, Int) -> Unit = { surah, ayah -> vm.goToAyah(surah, ayah, openSheet = false); openReader() }
    LaunchedEffect(Unit) { vm.refreshTafsir() }
    // Lompatan ke halaman dari mana pun (daftar, pencarian, info surah, notifikasi) membuka layar baca.
    LaunchedEffect(Unit) {
        vm.pendingPage.collect { page -> if (page != null) openReader() }
    }

    Row {
        if (expanded) {
            Surface(Modifier.width(360.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                IndexScreen(
                    vm = vm,
                    onOpenReader = openReader,
                    onOpenSettings = openSettings,
                    onOpenSurahInfo = { nav.navigate("surah/$it") },
                    onOpenExplore = onOpenExplore,
                    onOpenHome = openHome,
                )
            }
        }
        // Latar buram di belakang NavHost: pudar antarlayar tidak membuka jendela kosong/hitam.
        Surface(Modifier.weight(1f), color = MaterialTheme.colorScheme.background) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.fillMaxSize(),
            // Shared axis X: layar baru masuk dari sisi "maju" (kanan pada LTR, kiri pada RTL), layar lama bergeser ke arah
            // sebaliknya; kembali membalik arah.
            enterTransition = { sharedAxisXEnter(forward, slidePx) },
            exitTransition = { sharedAxisXExit(forward, slidePx) },
            popEnterTransition = { sharedAxisXEnter(-forward, slidePx) },
            popExitTransition = { sharedAxisXExit(-forward, slidePx) },
            // Gestur back prediktif: eksplisit (default NavHost mengecilkan ke kartu 0.7 lalu hilang mendadak).
            // Layar keluar mengecil ke 0.9 dan bergeser sedikit ke tepi belakang (kanan pada LTR); sudut dibulatkan di screen().
            predictivePopEnterTransition = { predictivePopEnter(forward, slidePx) },
            predictivePopExitTransition = { predictivePopExit(forward, shiftPx) },
        ) {
            screen("index") {
                IndexScreen(
                    vm = vm,
                    onOpenReader = openReader,
                    onOpenSettings = openSettings,
                    onOpenSurahInfo = { nav.navigate("surah/$it") },
                    onOpenExplore = onOpenExplore,
                    onOpenHome = openHome,
                )
            }
            screen("reader") {
                ReaderScreen(
                    vm = vm,
                    settings = settings,
                    onBack = if (expanded) null else ({ nav.popBackStack() }),
                    onOpenSearch = { nav.navigate("search") },
                    onOpenSettings = openSettings,
                    onOpenSurahInfo = { nav.navigate("surah/$it") },
                    onOpenTafsirSettings = { vm.dismissSheet(); nav.navigate(SettingsPage.Tafsir.route) },
                )
            }
            screen("surah/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                SurahInfoScreen(
                        vm = vm,
                        surahId = entry.arguments?.getInt("id") ?: 1,
                        onBack = { nav.popBackStack() },
                        onOpenAyah = openAyah,
                        onOpenPage = { page -> vm.clearSelection(); vm.goToPage(page); openReader() },
                    )
                }
                screen("explore") {
                    ExploreScreen(vm, onBack = { nav.popBackStack() }, onOpenTopic = { nav.navigate("topic/$it") })
                }
                screen("topic/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                    TopicScreen(
                        vm = vm,
                        topicId = entry.arguments?.getInt("id") ?: 0,
                        onBack = { nav.popBackStack() },
                        onOpenTopic = { nav.navigate("topic/$it") },
                        onOpenAyah = openAyah,
                    )
                }
                screen("themes") { ThemesScreen(vm, onBack = { nav.popBackStack() }, onOpenSurah = { nav.navigate("themes/$it") }, onOpenAyah = openAyah) }
                screen("themes/{surah}", arguments = listOf(navArgument("surah") { type = NavType.IntType })) { entry ->
                    SurahThemesScreen(vm, entry.arguments?.getInt("surah") ?: 1, onBack = { nav.popBackStack() }, onOpenAyah = openAyah)
                }
                screen("phrases") { PhrasesScreen(vm, onBack = { nav.popBackStack() }, onOpenPhrase = { nav.navigate("phrases/$it") }) }
                screen("phrases/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                    PhraseScreen(vm, entry.arguments?.getInt("id") ?: 0, onBack = { nav.popBackStack() }, onOpenAyah = openAyah)
                }
                screen("similar") { SimilarScreen(vm, onBack = { nav.popBackStack() }, onOpenSource = { s, a -> nav.navigate("similar/$s/$a") }) }
                screen(
                    "similar/{surah}/{ayah}",
                    arguments = listOf(navArgument("surah") { type = NavType.IntType }, navArgument("ayah") { type = NavType.IntType }),
                ) { entry ->
                    SimilarDetailScreen(vm, entry.arguments?.getInt("surah") ?: 1, entry.arguments?.getInt("ayah") ?: 1, onBack = { nav.popBackStack() }, onOpenAyah = openAyah)
                }
                screen("roots") { RootsScreen(vm, onBack = { nav.popBackStack() }, onOpenRoot = { nav.navigate("roots/$it") }) }
                screen("roots/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                    RootScreen(vm, entry.arguments?.getInt("id") ?: 0, onBack = { nav.popBackStack() }, onOpenAyah = openAyah)
                }
                screen("collection/{kind}") { entry ->
                    val kind = CollectionKind.entries.firstOrNull { it.name == entry.arguments?.getString("kind") } ?: CollectionKind.Dua
                    CollectionScreen(vm, kind, onBack = { nav.popBackStack() }, onOpenPassage = openHome)
                }
                screen("passage") {
                    val target = vm.passage.collectAsState().value
                    if (target != null) PassageScreen(vm, target, onBack = { nav.popBackStack() }, onOpenReader = openReader)
                }
            screen("search") { SearchScreen(vm, settings, onBack = { nav.popBackStack() }) }
            SettingsPage.entries.filter { it != SettingsPage.Main }.forEach { page ->
                screen(page.route) {
                    SettingsScreen(vm, settings, page = page, onBack = { nav.popBackStack() }, onOpen = { nav.navigate(it.route) })
                }
            }
        }
        if (settingsSheetVisible) {
            SettingsMainSheet(
                settings = settings,
                tafsirCount = vm.tafsirInstalled.collectAsState().value.size,
                onDismiss = { settingsSheetVisible = false },
                onOpen = { page ->
                    settingsSheetVisible = false
                    nav.navigate(page.route) { launchSingleTop = true }
                },
            )
        }
        }
    }
}

/** Seperti composable(), tapi dibungkus [PredictiveBackFrame] agar sudut membulat saat layar keluar (back prediktif). */
private fun NavGraphBuilder.screen(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(route, arguments) { entry -> PredictiveBackFrame { content(entry) } }
}
