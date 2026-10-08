package io.zakkyhidayat.quran

import io.zakkyhidayat.quran.settings.SettingsPage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import io.zakkyhidayat.quran.explore.ExploreScreen
import io.zakkyhidayat.quran.explore.TopicScreen
import io.zakkyhidayat.quran.info.SurahInfoScreen
import io.zakkyhidayat.quran.index.IndexScreen
import io.zakkyhidayat.quran.reader.ReaderScreen
import io.zakkyhidayat.quran.search.SearchScreen
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.SettingsScreen

// Lebar jendela "expanded" M3: daftar surah tampil permanen di samping pembaca.
private const val EXPANDED_WIDTH_DP = 840

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNav(vm: AppViewModel, settings: AppSettings) {
    val nav = rememberNavController()
    val motion = MaterialTheme.motionScheme
    val expanded = LocalConfiguration.current.screenWidthDp >= EXPANDED_WIDTH_DP
    val forward = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1

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
                    onOpenSettings = { nav.navigate(SettingsPage.Main.route) },
                    onOpenSurahInfo = { nav.navigate("surah/$it") },
                    onOpenExplore = { nav.navigate("explore") },
                )
            }
        }
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.weight(1f),
            // Shared axis X: layar baru masuk dari sisi "maju" (kanan pada LTR, kiri pada RTL), layar lama bergeser ke arah
            // sebaliknya; kembali membalik arah.
            enterTransition = { slideInHorizontally(motion.defaultSpatialSpec()) { forward * it / 8 } + fadeIn(motion.defaultEffectsSpec()) },
            exitTransition = { slideOutHorizontally(motion.defaultSpatialSpec()) { -forward * it / 8 } + fadeOut(motion.fastEffectsSpec()) },
            popEnterTransition = { slideInHorizontally(motion.defaultSpatialSpec()) { -forward * it / 8 } + fadeIn(motion.defaultEffectsSpec()) },
            popExitTransition = { slideOutHorizontally(motion.defaultSpatialSpec()) { forward * it / 8 } + fadeOut(motion.fastEffectsSpec()) },
        ) {
            composable("index") {
                IndexScreen(
                    vm = vm,
                    onOpenReader = openReader,
                    onOpenSettings = { nav.navigate(SettingsPage.Main.route) },
                    onOpenSurahInfo = { nav.navigate("surah/$it") },
                    onOpenExplore = { nav.navigate("explore") },
                )
            }
            composable("reader") {
                ReaderScreen(
                    vm = vm,
                    settings = settings,
                    onBack = if (expanded) null else ({ nav.popBackStack() }),
                    onOpenSearch = { nav.navigate("search") },
                    onOpenSettings = { nav.navigate(SettingsPage.Main.route) },
                    onOpenSurahInfo = { nav.navigate("surah/$it") },
                )
            }
            composable("surah/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                SurahInfoScreen(
                    vm = vm,
                    surahId = entry.arguments?.getInt("id") ?: 1,
                    onBack = { nav.popBackStack() },
                    onOpenAyah = { surah, ayah -> vm.goToAyah(surah, ayah); openReader() },
                    onOpenPage = { page -> vm.clearSelection(); vm.goToPage(page); openReader() },
                )
            }
            composable("explore") {
                ExploreScreen(vm, onBack = { nav.popBackStack() }, onOpenTopic = { nav.navigate("topic/$it") })
            }
            composable("topic/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                TopicScreen(
                    vm = vm,
                    topicId = entry.arguments?.getInt("id") ?: 0,
                    onBack = { nav.popBackStack() },
                    onOpenTopic = { nav.navigate("topic/$it") },
                    onOpenAyah = { surah, ayah -> vm.goToAyah(surah, ayah); openReader() },
                )
            }
            composable("search") { SearchScreen(vm, settings, onBack = { nav.popBackStack() }) }
            SettingsPage.entries.forEach { page ->
                composable(page.route) {
                    SettingsScreen(vm, settings, page = page, onBack = { nav.popBackStack() }, onOpen = { nav.navigate(it.route) })
                }
            }
        }
    }
}
