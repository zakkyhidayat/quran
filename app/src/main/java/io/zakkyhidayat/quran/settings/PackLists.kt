package io.zakkyhidayat.quran.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import io.zakkyhidayat.quran.ui.reducedMotion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.AppViewModel
import io.zakkyhidayat.quran.DELETE_UNDO_MS
import io.zakkyhidayat.quran.R
import io.zakkyhidayat.quran.data.CatalogPack
import io.zakkyhidayat.quran.ui.AppIcons
import io.zakkyhidayat.quran.ui.segmentedItemColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Baris yang dihapus dengan menggeser ke arah ujung (akhir) baris. Geser menyembunyikan baris lewat [onDelete];
 * penghapusan sebenarnya tertunda sampai jeda Batalkan habis (lihat AppViewModel.scheduleDeletePack). Pembaca layar
 * mendapat aksi "Hapus" sebagai gantinya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeToDeleteRow(
    deleteLabel: String,
    shape: Shape,
    visible: Boolean,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    // Baris yang kembali (Batalkan) harus mulai dalam keadaan belum tergeser, bukan tetap terhapus.
    LaunchedEffect(visible) { if (visible) state.snapTo(SwipeToDismissBoxValue.Settled) }
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(AppIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) { content() }
}

/** Snackbar "X dihapus" dengan tombol Batalkan selama [DELETE_UNDO_MS]; Batalkan memulihkan baris. */
internal fun showUndoDelete(
    scope: CoroutineScope,
    snackbar: SnackbarHostState,
    vm: AppViewModel,
    id: String,
    message: String,
    undoLabel: String,
) {
    scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        val result = withTimeoutOrNull(DELETE_UNDO_MS) {
            snackbar.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Indefinite)
        }
        if (result == SnackbarResult.ActionPerformed) vm.undoDeletePack(id)
    }
}

/** Pembaruan satu paket terpasang pada satu waktu, dengan progres dan pesan galat. */
internal class PackUpdater(
    private val vm: AppViewModel,
    private val scope: CoroutineScope,
    private val logTag: String,
) {
    var updating by mutableStateOf<String?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var failed by mutableStateOf<String?>(null)
        private set
    private var job: Job? = null

    fun start(pack: CatalogPack, id: String, name: String) {
        job = scope.launch {
            updating = id
            progress = 0f
            failed = null
            try {
                vm.updatePack(pack) { done, total -> progress = if (total > 0) done.toFloat() / total else 0f }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w(logTag, "Gagal memperbarui ${pack.id}", e)
                failed = name
            } finally {
                updating = null
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}

@Composable
internal fun rememberPackUpdater(vm: AppViewModel, logTag: String): PackUpdater {
    val scope = rememberCoroutineScope()
    return remember(vm) { PackUpdater(vm, scope, logTag) }
}

/**
 * Satu baris paket terunduh di halaman Terjemahan atau Tafsir: nama, penanda pembaruan, tombol perbarui, dan
 * [control] (sakelar atau radio). Paket yang bisa dihapus dibungkus [SwipeToDeleteRow].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DownloadedPackRow(
    title: String,
    index: Int,
    count: Int,
    deletable: Boolean,
    visible: Boolean,
    packId: String,
    updatePack: CatalogPack?,
    updater: PackUpdater,
    updateDescription: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    control: @Composable () -> Unit,
) {
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val motion = MaterialTheme.motionScheme
    val row: @Composable () -> Unit = {
        SegmentedListItem(
            onClick = onClick,
            shapes = shapes,
            colors = segmentedItemColors(),
            supportingContent = if (updatePack != null) {
                { Text(stringResource(R.string.translation_update_available)) }
            } else {
                null
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (updatePack != null) {
                        DownloadButton(
                            downloading = updater.updating == packId,
                            progress = updater.progress,
                            onStart = { updater.start(updatePack, packId, title) },
                            onCancel = { updater.cancel() },
                            startDescription = updateDescription,
                            cancelDescription = stringResource(R.string.cancel),
                            enabled = updater.updating == null,
                            icon = AppIcons.Refresh,
                        )
                    }
                    control()
                }
            },
        ) { Text(title) }
    }
    // Hapus: baris menyusut dan memudar dengan spesifikasi M3 (cepat, ikut pengurangan gerak). Batalkan: kebalikannya.
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(reducedMotion(motion.fastSpatialSpec())) +
            fadeIn(reducedMotion(motion.fastEffectsSpec())),
        exit = shrinkVertically(reducedMotion(motion.fastSpatialSpec())) +
            fadeOut(reducedMotion(motion.fastEffectsSpec())),
    ) {
        if (deletable) {
            SwipeToDeleteRow(
                deleteLabel = stringResource(R.string.delete),
                shape = shapes.shape,
                visible = visible,
                onDelete = onDelete,
                content = row,
            )
        } else {
            row()
        }
    }
}

/** Bagian bawah daftar terunduh: petunjuk geser, galat pembaruan, dan baris menuju halaman Tambah. */
@Composable
internal fun PackListFooter(showSwipeHint: Boolean, updateFailed: String?, addTitle: String, onAdd: () -> Unit) {
    if (showSwipeHint) {
        Text(
            stringResource(R.string.swipe_delete_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )
    }
    updateFailed?.let {
        Text(
            stringResource(R.string.translation_update_failed, it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    Group {
        item(
            title = addTitle,
            onClick = onAdd,
            leading = { Icon(AppIcons.Add, contentDescription = null) },
            trailing = { Icon(AppIcons.KeyboardArrowRight, contentDescription = null) },
        )
    }
}
