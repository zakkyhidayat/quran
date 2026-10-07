package io.zakkyhidayat.quran.ui

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

// Ikon Material yang tidak ada di paket icons-core; ditulis langsung supaya tidak menarik paket extended (besar).
object AppIcons {
    val Bookmark: ImageVector by lazy {
        materialIcon("Bookmark") {
            materialPath {
                moveTo(17f, 3f); horizontalLineTo(7f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(5f, 21f); lineToRelative(7f, -3f); lineToRelative(7f, 3f)
                verticalLineTo(5f); curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f); close()
            }
        }
    }

    val BookmarkBorder: ImageVector by lazy {
        materialIcon("BookmarkBorder") {
            materialPath {
                moveTo(17f, 3f); lineTo(7f, 3f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(5f, 21f); lineToRelative(7f, -3f); lineToRelative(7f, 3f)
                lineTo(19f, 5f); curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f); close()
                moveTo(17f, 18f); lineToRelative(-5f, -2.18f); lineTo(7f, 18f); lineTo(7f, 5f); horizontalLineToRelative(10f); verticalLineToRelative(13f); close()
            }
        }
    }

    val ContentCopy: ImageVector by lazy {
        materialIcon("ContentCopy") {
            materialPath {
                moveTo(16f, 1f); lineTo(4f, 1f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f); horizontalLineToRelative(2f); lineTo(4f, 3f); horizontalLineToRelative(12f); lineTo(16f, 1f); close()
                moveTo(19f, 5f); lineTo(8f, 5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(11f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                lineTo(21f, 7f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f); close()
                moveTo(19f, 21f); lineTo(8f, 21f); lineTo(8f, 7f); horizontalLineToRelative(11f); verticalLineToRelative(14f); close()
            }
        }
    }

    val Shuffle: ImageVector by lazy {
        materialIcon("Shuffle") {
            materialPath {
                moveTo(10.59f, 9.17f); lineTo(5.41f, 4f); lineTo(4f, 5.41f); lineToRelative(5.17f, 5.17f); lineToRelative(1.42f, -1.41f); close()
                moveTo(14.5f, 4f); lineToRelative(2.04f, 2.04f); lineTo(4f, 18.59f); lineTo(5.41f, 20f); lineTo(17.96f, 7.46f); lineTo(20f, 9.5f); lineTo(20f, 4f); horizontalLineToRelative(-5.5f); close()
                moveTo(14.83f, 13.41f); lineToRelative(-1.41f, 1.41f); lineToRelative(3.13f, 3.13f); lineTo(14.5f, 20f); lineTo(20f, 20f); verticalLineToRelative(-5.5f); lineToRelative(-2.04f, 2.04f); lineToRelative(-3.13f, -3.13f); close()
            }
        }
    }
}
