package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ArrowDownIcon: ImageVector
    get() {
        if (_arrowDownIcon != null) {
            return _arrowDownIcon!!
        }
        _arrowDownIcon = ImageVector.Builder(
            name = "ArrowDownIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.LightGray),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // M12 17a1.72 1.72 0 0 1-1.33-.64l-4.21-5.1a2.1 2.1 0 0 1-.26-2.21A1.76 1.76 0 0 1 7.79 8h8.42a1.76 1.76 0 0 1 1.59 1.05 2.1 2.1 0 0 1-.26 2.21l-4.21 5.1A1.72 1.72 0 0 1 12 17z
                moveTo(12f, 17f)
                arcToRelative(1.72f, 1.72f, 0f, false, true, -1.33f, -0.64f)
                lineToRelative(-4.21f, -5.1f)
                arcToRelative(2.1f, 2.1f, 0f, false, true, -0.26f, -2.21f)
                arcTo(1.76f, 1.76f, 0f, false, true, 7.79f, 8f)
                horizontalLineToRelative(8.42f)
                arcToRelative(1.76f, 1.76f, 0f, false, true, 1.59f, 1.05f)
                arcToRelative(2.1f, 2.1f, 0f, false, true, -0.26f, 2.21f)
                lineToRelative(-4.21f, 5.1f)
                arcTo(1.72f, 1.72f, 0f, false, true, 12f, 17f)
                close()
            }
        }.build()
        return _arrowDownIcon!!
    }

private var _arrowDownIcon: ImageVector? = null
