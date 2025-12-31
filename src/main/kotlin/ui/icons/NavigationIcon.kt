package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val NavigationIcon: ImageVector
    get() {
        if (_navigationIcon != null) {
            return _navigationIcon!!
        }
        _navigationIcon = ImageVector.Builder(
            name = "NavigationIcon",
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
                // M11.9,4.9a7,7 0,1 0,7 7A7,7 0,0 0,11.9 4.9zM11.9,17.5a5.6,5.6 0,1 1,5.6 -5.6,5.6 5.6,0 0,1 -5.6,5.6z
                moveTo(11.9f, 4.9f)
                arcToRelative(7f, 7f, 0f, true, false, 7f, 7f)
                arcTo(7f, 7f, 0f, false, false, 11.9f, 4.9f)
                close()
                moveTo(11.9f, 17.5f)
                arcToRelative(5.6f, 5.6f, 0f, true, true, 5.6f, -5.6f)
                arcToRelative(5.6f, 5.6f, 0f, false, true, -5.6f, 5.6f)
                close()
            }
            path(
                fill = SolidColor(Color.LightGray),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // M14.476,9.324a0.7,0.7 0,0 0,-0.77 -0.175l-2.947,1.19a0.7,0.7 0,0 0,-0.385 0.385l-1.225,2.982a0.7,0.7 0,0 0,0.126 0.7l0.035,0A0.7,0.7 0,0 0,9.8 14.7a0.7,0.7 67.241,0 0,0.266 -0.049l2.947,-1.19a0.7,0.7 0,0 0,0.385 -0.385l1.225,-2.982a0.7,0.7 0,0 0,-0.147 -0.77zM11.06,12.747l0.497,-1.218 1.183,-0.476 -0.497,1.218z
                moveTo(14.476f, 9.324f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, -0.77f, -0.175f)
                lineToRelative(-2.947f, 1.19f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, -0.385f, 0.385f)
                lineToRelative(-1.225f, 2.982f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, 0.126f, 0.7f)
                lineToRelative(0.035f, 0f)
                arcTo(0.7f, 0.7f, 0f, false, false, 9.8f, 14.7f)
                arcToRelative(0.7f, 0.7f, 67.241f, false, false, 0.266f, -0.049f)
                lineToRelative(2.947f, -1.19f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, 0.385f, -0.385f)
                lineToRelative(1.225f, -2.982f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, -0.147f, -0.77f)
                close()
                moveTo(11.06f, 12.747f)
                lineToRelative(0.497f, -1.218f)
                lineToRelative(1.183f, -0.476f)
                lineToRelative(-0.497f, 1.218f)
                close()
            }
        }.build()
        return _navigationIcon!!
    }

private var _navigationIcon: ImageVector? = null
