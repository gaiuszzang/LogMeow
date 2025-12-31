package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val StopIcon: ImageVector
    get() {
        if (_stopIcon != null) {
            return _stopIcon!!
        }
        _stopIcon = ImageVector.Builder(
            name = "StopIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFE04040)),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // M13.3,5.6L7.7,5.6a2.1,2.1 0,0 0,-2.1 2.1l0,8.4a2.1,2.1 0,0 0,2.1 2.1l8.4,0a2.1,2.1 0,0 0,2.1 -2.1L18.2,7.7a2.1,2.1 0,0 0,-2.1 -2.1z
                moveTo(13.3f, 5.6f)
                lineTo(7.7f, 5.6f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, -2.1f, 2.1f)
                lineToRelative(0f, 8.4f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, 2.1f)
                lineToRelative(8.4f, 0f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, -2.1f)
                lineTo(18.2f, 7.7f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, -2.1f, -2.1f)
                close()
            }
        }.build()
        return _stopIcon!!
    }

private var _stopIcon: ImageVector? = null
