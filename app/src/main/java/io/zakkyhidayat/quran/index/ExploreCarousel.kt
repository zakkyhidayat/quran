package io.zakkyhidayat.quran.index

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.ExploreCounts
import io.zakkyhidayat.quran.ui.AppIcons

/** Tujuan kartu Jelajahi; rute diatur AppNav. */
enum class ExploreEntry { Topics, Themes, Phrases, Similar, Roots }

private data class EntryUi(val entry: ExploreEntry, val icon: ImageVector, val title: Int, val plural: Int, val count: Int)

/**
 * Bagian "Jelajahi Al-Qur'an" di atas tab layar utama: carousel M3 (uncontained) berisi lima fitur. Kartu disembunyikan bila
 * tabel datanya tidak ada (hitungan null). Lebar kartu tetap sehingga muat di panel sempit 360dp pada layar lebar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreCarousel(counts: ExploreCounts, onOpen: (ExploreEntry) -> Unit, modifier: Modifier = Modifier) {
    val items = listOfNotNull(
        counts.topics?.let { EntryUi(ExploreEntry.Topics, AppIcons.Category, R.string.explore_topics, R.string.entries_topics, it) },
        counts.themes?.let { EntryUi(ExploreEntry.Themes, AppIcons.Lightbulb, R.string.explore_themes, R.string.entries_themes, it) },
        counts.phrases?.let { EntryUi(ExploreEntry.Phrases, AppIcons.FormatQuote, R.string.explore_phrases, R.string.entries_phrases, it) },
        counts.similar?.let { EntryUi(ExploreEntry.Similar, AppIcons.CompareArrows, R.string.explore_similar, R.string.entries_similar, it) },
        counts.roots?.let { EntryUi(ExploreEntry.Roots, AppIcons.AccountTree, R.string.explore_roots, R.string.entries_roots, it) },
    )
    if (items.isEmpty()) return
    Column(modifier) {
        Text(
            stringResource(R.string.explore_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
        )
        val state = rememberCarouselState { items.size }
        HorizontalUncontainedCarousel(
            state = state,
            itemWidth = 152.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(120.dp),
        ) { index ->
            val item = items[index]
            val title = stringResource(item.title)
            val count = stringResource(item.plural, item.count)
            Card(
                onClick = { onOpen(item.entry) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                // Satu kalimat untuk pembaca layar ("Topik & konsep, 2512 topik"); kartu menjadi satu elemen yang bisa diketuk.
                modifier = Modifier.fillMaxSize().maskClip(MaterialTheme.shapes.extraLarge).semantics(mergeDescendants = true) { contentDescription = "$title, $count" },
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(item.icon, contentDescription = null, modifier = Modifier.size(28.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text(count, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
