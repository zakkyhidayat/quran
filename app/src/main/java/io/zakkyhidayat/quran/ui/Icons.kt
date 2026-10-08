package io.zakkyhidayat.quran.ui

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

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

    val LightMode = svg("LightMode", "M12,7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5 5,-2.24 5,-5 -5,-2.24 -5,-5zM2,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1L2,11c-0.55,0 -1,0.45 -1,1s0.45,1 1,1zM20,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1h-2c-0.55,0 -1,0.45 -1,1s0.45,1 1,1zM11,2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1L13,2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1zM11,20v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1zM5.99,4.58c-0.39,-0.39 -1.03,-0.39 -1.41,0 -0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0s0.39,-1.03 0,-1.41L5.99,4.58zM18.36,16.95c-0.39,-0.39 -1.03,-0.39 -1.41,0 -0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0 0.39,-0.39 0.39,-1.03 0,-1.41l-1.06,-1.06zM19.42,5.99c0.39,-0.39 0.39,-1.03 0,-1.41 -0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0l1.06,-1.06zM7.05,18.36c0.39,-0.39 0.39,-1.03 0,-1.41 -0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0l1.06,-1.06z")

    val DarkMode = svg("DarkMode", "M9.37,5.51c-0.18,0.64 -0.27,1.31 -0.27,1.99 0,4.08 3.32,7.4 7.4,7.4 0.68,0 1.35,-0.09 1.99,-0.27C17.45,17.19 14.93,19 12,19c-3.86,0 -7,-3.14 -7,-7 0,-2.93 1.81,-5.45 4.37,-6.49zM12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 -0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3z")

    val BrightnessAuto = svg("BrightnessAuto", "M20,8.69V4h-4.69L12,0.69 8.69,4H4v4.69L0.69,12 4,15.31V20h4.69L12,23.31 15.31,20H20v-4.69L23.31,12 20,8.69zM12,18c-3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6 6,2.69 6,6 -2.69,6 -6,6zM12,8v8c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4z")

    val Palette = svg("Palette", "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9c0.83,0 1.5,-0.67 1.5,-1.5 0,-0.39 -0.15,-0.74 -0.39,-1.01 -0.23,-0.26 -0.38,-0.61 -0.38,-0.99 0,-0.83 0.67,-1.5 1.5,-1.5L16,16c2.76,0 5,-2.24 5,-5 0,-4.42 -4.03,-8 -9,-8zM6.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,9 6.5,9 8,9.67 8,10.5 7.33,12 6.5,12zM9.5,8C8.67,8 8,7.33 8,6.5S8.67,5 9.5,5s1.5,0.67 1.5,1.5S10.33,8 9.5,8zM14.5,8c-0.83,0 -1.5,-0.67 -1.5,-1.5S13.67,5 14.5,5s1.5,0.67 1.5,1.5S15.33,8 14.5,8zM17.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S16.67,9 17.5,9s1.5,0.67 1.5,1.5S18.33,12 17.5,12z")

    val AutoAwesome = svg("AutoAwesome", "M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15zM11.5,9.5L9,4L6.5,9.5L1,12l5.5,2.5L9,20l2.5,-5.5L17,12L11.5,9.5zM9.99,12.99L9,15.17l-0.99,-2.18L5.83,12l2.18,-0.99L9,8.83l0.99,2.18L12.17,12L9.99,12.99z")

    val Contrast = svg("Contrast", "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20V4c4.41,0 8,3.59 8,8s-3.59,8 -8,8z")

    val FormatColorText = svg("FormatColorText", "M2,20h20v4H2v-4zM5.49,17h2.42l1.27,-3.58h5.65L16.09,17h2.42L13.25,3h-2.5L5.49,17zM9.91,11.39l2.03,-5.79h0.12l2.03,5.79H9.91z")

    val MenuBook = svg("MenuBook", "M21,5c-1.11,-0.35 -2.33,-0.5 -3.5,-0.5 -1.95,0 -4.05,0.4 -5.5,1.5 -1.45,-1.1 -3.55,-1.5 -5.5,-1.5S2.45,4.9 1,6v14.65c0,0.25 0.25,0.5 0.5,0.5 0.1,0 0.15,-0.05 0.25,-0.05C3.1,20.45 5.05,20 6.5,20c1.95,0 4.05,0.4 5.5,1.5 1.35,-0.85 3.8,-1.5 5.5,-1.5 1.65,0 3.35,0.3 4.75,1.05 0.1,0.05 0.15,0.05 0.25,0.05 0.25,0 0.5,-0.25 0.5,-0.5V6c-0.6,-0.45 -1.25,-0.75 -2,-1zM21,18.5c-1.1,-0.35 -2.3,-0.5 -3.5,-0.5 -1.7,0 -4.15,0.65 -5.5,1.5V8c1.35,-0.85 3.8,-1.5 5.5,-1.5 1.2,0 2.4,0.15 3.5,0.5v11.5z")

    val GridView = svg("GridView", "M3,3v8h8L11,3L3,3zM9,9L5,9L5,5h4v4zM3,13v8h8v-8L3,13zM9,19L5,19v-4h4v4zM13,3v8h8L21,3h-8zM19,9h-4L15,5h4v4zM13,13v8h8v-8h-8zM19,19h-4v-4h4v4z")

    val Translate = svg("Translate", "M12.87,15.07l-2.54,-2.51 0.03,-0.03c1.74,-1.94 2.98,-4.17 3.71,-6.53L17,6L17,4h-7L10,2L8,2v2L1,4v1.99h11.17C11.5,7.92 10.44,9.75 9,11.35 8.07,10.32 7.3,9.19 6.69,8h-2c0.73,1.63 1.73,3.17 2.98,4.56l-5.09,5.02L4,19l5,-5 3.11,3.11 0.76,-2.04zM18.5,10h-2L12,22h2l1.12,-3h4.75L21,22h2l-4.5,-12zM15.88,17l1.62,-4.33L19.12,17h-3.24z")

    val PieChart = svg("PieChart", "M11,2v20c-5.07,-0.5 -9,-4.79 -9,-10s3.93,-9.5 9,-10zM13.03,2v8.99L22,10.99c-0.47,-4.74 -4.24,-8.52 -8.97,-8.99zM13.03,13.01L13.03,22c4.74,-0.47 8.52,-4.25 8.99,-8.99h-8.99z")

    val CalendarViewWeek = svg("CalendarViewWeek", "M6,5H3C2.45,5 2,5.45 2,6V18C2,18.55 2.45,19 3,19H6C6.55,19 7,18.55 7,18V6C7,5.45 6.55,5 6,5zM21,5H18C17.45,5 17,5.45 17,6V18C17,18.55 17.45,19 18,19H21C21.55,19 22,18.55 22,18V6C22,5.45 21.55,5 21,5zM13.5,5H10.5C9.95,5 9.5,5.45 9.5,6V18C9.5,18.55 9.95,19 10.5,19H13.5C14.05,19 14.5,18.55 14.5,18V6C14.5,5.45 14.05,5 13.5,5z")

    val FormatListNumbered = svg("FormatListNumbered", "M2,17h2v0.5L3,18.5v1h1v0.5L2,20v1h3v-4L2,17zM3,8h1L4,4L2,4v1h1L3,8zM2,11h1.8L2,13.1v0.9h3v-1L3.2,13L5,10.9L5,10L2,10v1zM7,5v2h14L21,5L7,5zM7,19h14v-2L7,17v2zM7,13h14v-2L7,11v2z")
}

private fun svg(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()
