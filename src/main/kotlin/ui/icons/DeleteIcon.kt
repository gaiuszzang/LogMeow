package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val DeleteIcon: ImageVector
    get() {
        if (_deleteIcon != null) {
            return _deleteIcon!!
        }
        _deleteIcon = ImageVector.Builder(
            name = "DeleteIcon",
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
                // M18.2,7.7l-3.5,0L14.7,6.531A1.694,1.694 0,0 0,12.95 4.9l-2.1,0A1.694,1.694 0,0 0,9.1 6.531L9.1,7.7L5.6,7.7a0.7,0.7 0,0 0,0 1.4l0.7,0l0,7.7a2.1,2.1 0,0 0,2.1 2.1l7,0a2.1,2.1 0,0 0,2.1 -2.1L17.5,9.1l0.7,0a0.7,0.7 0,0 0,0 -1.4zM10.5,6.531c0,-0.112 0.147,-0.231 0.35,-0.231l2.1,0c0.203,0 0.35,0.119 0.35,0.231L13.3,7.7l-2.8,0zM16.1,16.8a0.7,0.7 0,0 1,-0.7 0.7L8.4,17.5a0.7,0.7 0,0 1,-0.7 -0.7L7.7,9.1l8.4,0z
                moveTo(18.2f, 7.7f)
                lineToRelative(-3.5f, 0f)
                lineTo(14.7f, 6.531f)
                arcTo(1.694f, 1.694f, 0f, false, false, 12.95f, 4.9f)
                lineToRelative(-2.1f, 0f)
                arcTo(1.694f, 1.694f, 0f, false, false, 9.1f, 6.531f)
                lineTo(9.1f, 7.7f)
                lineTo(5.6f, 7.7f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, 0f, 1.4f)
                lineToRelative(0.7f, 0f)
                lineToRelative(0f, 7.7f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, 2.1f)
                lineToRelative(7f, 0f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, -2.1f)
                lineTo(17.5f, 9.1f)
                lineToRelative(0.7f, 0f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, 0f, -1.4f)
                close()
                moveTo(10.5f, 6.531f)
                curveToRelative(0f, -0.112f, 0.147f, -0.231f, 0.35f, -0.231f)
                lineToRelative(2.1f, 0f)
                curveToRelative(0.203f, 0f, 0.35f, 0.119f, 0.35f, 0.231f)
                lineTo(13.3f, 7.7f)
                lineToRelative(-2.8f, 0f)
                close()
                moveTo(16.1f, 16.8f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, -0.7f, 0.7f)
                lineTo(8.4f, 17.5f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, -0.7f, -0.7f)
                lineTo(7.7f, 9.1f)
                lineToRelative(8.4f, 0f)
                close()
            }
        }.build()
        return _deleteIcon!!
    }

private var _deleteIcon: ImageVector? = null
