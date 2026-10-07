package io.zakkyhidayat.quran

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.zakkyhidayat.quran.index.IndexScreen
import io.zakkyhidayat.quran.reader.ReaderScreen
import io.zakkyhidayat.quran.search.SearchScreen
import io.zakkyhidayat.quran.settings.AppSettings
import io.zakkyhidayat.quran.settings.SettingsScreen

@Composable
fun AppNav(vm: AppViewModel, settings: AppSettings) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "reader") {
        composable("reader") {
            ReaderScreen(
                vm = vm,
                settings = settings,
                onOpenIndex = { nav.navigate("index") },
                onOpenSearch = { nav.navigate("search") },
                onOpenSettings = { nav.navigate("settings") },
            )
        }
        composable("index") {
            IndexScreen(vm, onBack = { nav.popBackStack() }, onOpenSettings = { nav.navigate("settings") })
        }
        composable("search") { SearchScreen(vm, settings, onBack = { nav.popBackStack() }) }
        composable("settings") { SettingsScreen(vm, settings, onBack = { nav.popBackStack() }) }
    }
}
