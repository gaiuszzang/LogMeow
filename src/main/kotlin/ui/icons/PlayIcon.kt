package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PlayIcon: ImageVector
    get() {
        if (_playIcon != null) {
            return _playIcon!!
        }
        _playIcon = ImageVector.Builder(
            name = "PlayIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF50E050)),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // M10.46,18a2.23,2.23 0,0 1,-0.91 -0.2,1.76 1.76,0 0,1 -1.05,-1.59V7.79A1.76,1.76 0,0 1,9.55 6.2a2.1,2.1 0,0 1,2.21 0.26l5.1,4.21a1.7,1.7 0,0 1,0 2.66l-5.1,4.21a2.06,2.06 0,0 1,-1.3 0.46z
                moveTo(10.46f, 18f)
                arcToRelative(2.23f, 2.23f, 0f, false, true, -0.91f, -0.2f)
                arcToRelative(1.76f, 1.76f, 0f, false, true, -1.05f, -1.59f)
                verticalLineTo(7.79f)
                arcTo(1.76f, 1.76f, 0f, false, true, 9.55f, 6.2f)
                arcToRelative(2.1f, 2.1f, 0f, false, true, 2.21f, 0.26f)
                lineToRelative(5.1f, 4.21f)
                arcToRelative(1.7f, 1.7f, 0f, false, true, 0f, 2.66f)
                lineToRelative(-5.1f, 4.21f)
                arcToRelative(2.06f, 2.06f, 0f, false, true, -1.3f, 0.46f)
                close()
            }
        }.build()
        return _playIcon!!
    }

private var _playIcon: ImageVector? = null
