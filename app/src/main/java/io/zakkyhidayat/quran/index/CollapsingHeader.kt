package io.zakkyhidayat.quran.index

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * Keadaan header yang mengecil saat daftar di bawahnya digulir (pola exitUntilCollapsed): header menyusut lebih dulu,
 * baru daftar bergulir; header hanya muncul lagi bila daftar sudah di paling atas dan masih ditarik ke bawah.
 */
class CollapsingHeaderState {
    /** Tinggi alami header (px), diisi saat pengukuran. */
    var height by mutableFloatStateOf(0f)
        internal set

    /** Geseran header, 0 (penuh) sampai -[height] (tersembunyi). */
    var offset by mutableFloatStateOf(0f)
        private set

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y >= 0f) return Offset.Zero
            val new = (offset + available.y).coerceIn(-height, 0f)
            val consumed = new - offset
            offset = new
            return Offset(0f, consumed)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (available.y <= 0f) return Offset.Zero
            val new = (offset + available.y).coerceIn(-height, 0f)
            val used = new - offset
            offset = new
            return Offset(0f, used)
        }
    }
}

/** Mengukur isi dengan tinggi penuh, tetapi melaporkan tinggi yang menyusut mengikuti [state]; geseran dibaca di fase layout. */
fun Modifier.collapsingHeader(state: CollapsingHeaderState): Modifier = this
    .clipToBounds()
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        state.height = placeable.height.toFloat()
        val visible = (placeable.height + state.offset).roundToInt().coerceAtLeast(0)
        layout(placeable.width, visible) { placeable.place(0, state.offset.roundToInt()) }
    }
