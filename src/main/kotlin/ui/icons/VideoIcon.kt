package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val VideoIcon: ImageVector
    get() {
        if (_videoIcon != null) {
            return _videoIcon!!
        }
        _videoIcon = ImageVector.Builder(
            name = "VideoIcon",
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
                // M18.2,8.505a1.19,1.19 0,0 0,-1.295 0.21l-1.505,1.4L15.4,9.1a2.1,2.1 0,0 0,-2.1 -2.1L7,7a2.1,2.1 0,0 0,-2.1 2.1l0,5.6a2.1,2.1 0,0 0,2.1 2.1l6.3,0a2.1,2.1 0,0 0,2.1 -2.1l0,-1.015l1.512,1.4a1.218,1.218 0,0 0,0.812 0.315,1.176 1.176,45 0,0 0.483,-0.105 1.12,1.12 0,0 0,0.7 -1.036L18.907,9.541A1.12,1.12 0,0 0,18.2 8.505zM14,14.7a0.7,0.7 0,0 1,-0.7 0.7L7,15.4a0.7,0.7 0,0 1,-0.7 -0.7L6.3,9.1a0.7,0.7 0,0 1,0.7 -0.7l6.3,0a0.7,0.7 0,0 1,0.7 0.7zM17.5,13.72L15.533,11.9 17.5,10.08z
                moveTo(18.2f, 8.505f)
                arcToRelative(1.19f, 1.19f, 0f, false, false, -1.295f, 0.21f)
                lineToRelative(-1.505f, 1.4f)
                lineTo(15.4f, 9.1f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, -2.1f, -2.1f)
                lineTo(7f, 7f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, -2.1f, 2.1f)
                lineToRelative(0f, 5.6f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, 2.1f)
                lineToRelative(6.3f, 0f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, -2.1f)
                lineToRelative(0f, -1.015f)
                lineToRelative(1.512f, 1.4f)
                arcToRelative(1.218f, 1.218f, 0f, false, false, 0.812f, 0.315f)
                arcToRelative(1.176f, 1.176f, 45f, false, false, 0.483f, -0.105f)
                arcToRelative(1.12f, 1.12f, 0f, false, false, 0.7f, -1.036f)
                lineTo(18.907f, 9.541f)
                arcTo(1.12f, 1.12f, 0f, false, false, 18.2f, 8.505f)
                close()
                moveTo(14f, 14.7f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, -0.7f, 0.7f)
                lineTo(7f, 15.4f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, -0.7f, -0.7f)
                lineTo(6.3f, 9.1f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, 0.7f, -0.7f)
                lineToRelative(6.3f, 0f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, 0.7f, 0.7f)
                close()
                moveTo(17.5f, 13.72f)
                lineTo(15.533f, 11.9f)
                lineTo(17.5f, 10.08f)
                close()
            }
        }.build()
        return _videoIcon!!
    }

private var _videoIcon: ImageVector? = null
