package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ChevronRightIcon: ImageVector
    get() {
        if (_chevronRightIcon != null) {
            return _chevronRightIcon!!
        }
        _chevronRightIcon = ImageVector.Builder(
            name = "ChevronRightIcon",
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
                // chevron_right: M10 6 L8.59 7.41 13.17 12 8.59 16.59 10 18 l6-6z
                moveTo(10f, 6f)
                lineTo(8.59f, 7.41f)
                lineTo(13.17f, 12f)
                lineTo(8.59f, 16.59f)
                lineTo(10f, 18f)
                lineToRelative(6f, -6f)
                close()
            }
        }.build()
        return _chevronRightIcon!!
    }

private var _chevronRightIcon: ImageVector? = null
