package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ChevronLeftIcon: ImageVector
    get() {
        if (_chevronLeftIcon != null) {
            return _chevronLeftIcon!!
        }
        _chevronLeftIcon = ImageVector.Builder(
            name = "ChevronLeftIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // chevron_left: M15.41 7.41 L14 6 l-6 6 6 6 1.41-1.41L10.83 12z
                moveTo(15.41f, 7.41f)
                lineTo(14f, 6f)
                lineToRelative(-6f, 6f)
                lineToRelative(6f, 6f)
                lineToRelative(1.41f, -1.41f)
                lineTo(10.83f, 12f)
                close()
            }
        }.build()
        return _chevronLeftIcon!!
    }

private var _chevronLeftIcon: ImageVector? = null
