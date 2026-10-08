package io.zakkyhidayat.quran.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.zakkyhidayat.quran.ui.segmentedItemColors

// Switch M3 dengan ikon centang pada ibu jari saat aktif.
@Composable
internal fun IconSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        thumbContent = if (checked) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else {
            null
        },
    )
}

internal class GroupItem(
    val title: String,
    val subtitle: String?,
    val onClick: (() -> Unit)?,
    val leading: (@Composable () -> Unit)?,
    val trailing: (@Composable () -> Unit)?,
)

internal class GroupScope {
    val items = mutableListOf<GroupItem>()

    fun item(
        title: String,
        subtitle: String? = null,
        onClick: (() -> Unit)? = null,
        leading: (@Composable () -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
    ) {
        items += GroupItem(title, subtitle, onClick, leading, trailing)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Group(content: @Composable GroupScope.() -> Unit) {
    val scope = GroupScope().apply { content() }
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        scope.items.forEachIndexed { index, item ->
            val shapes = ListItemDefaults.segmentedShapes(index, scope.items.size)
            val supporting: (@Composable () -> Unit)? = item.subtitle?.let { sub -> { Text(sub) } }
            val onClick = item.onClick
            if (onClick != null) {
                SegmentedListItem(
                    onClick = onClick,
                    shapes = shapes,
                    colors = segmentedItemColors(),
                    leadingContent = item.leading,
                    trailingContent = item.trailing,
                    supportingContent = supporting,
                ) { Text(item.title) }
            } else {
                // Baris informasi saja: bentuk dan warna sama, tetapi tidak bisa diketuk.
                ListItem(
                    headlineContent = { Text(item.title) },
                    supportingContent = supporting,
                    leadingContent = item.leading,
                    trailingContent = item.trailing,
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.clip(shapes.shape),
                )
            }
        }
    }
}

@Composable
internal fun SectionTitle(icon: ImageVector, text: String) {
    Row(Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        content()
    }
}
