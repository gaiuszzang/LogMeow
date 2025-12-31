package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PhoneIcon: ImageVector
    get() {
        if (_phoneIcon != null) {
            return _phoneIcon!!
        }
        _phoneIcon = ImageVector.Builder(
            name = "PhoneIcon",
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
                // M15.4,4.9L8.4,4.9a2.1,2.1 0,0 0,-2.1 2.1l0,9.8a2.1,2.1 0,0 0,2.1 2.1l7,0a2.1,2.1 0,0 0,2.1 -2.1L17.5,7a2.1,2.1 0,0 0,-2.1 -2.1zM16.1,16.8a0.7,0.7 0,0 1,-0.7 0.7L8.4,17.5a0.7,0.7 0,0 1,-0.7 -0.7L7.7,7a0.7,0.7 0,0 1,0.7 -0.7l7,0a0.7,0.7 0,0 1,0.7 0.7z
                moveTo(15.4f, 4.9f)
                lineTo(8.4f, 4.9f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, -2.1f, 2.1f)
                lineToRelative(0f, 9.8f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, 2.1f)
                lineToRelative(7f, 0f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, -2.1f)
                lineTo(17.5f, 7f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, -2.1f, -2.1f)
                close()
                moveTo(16.1f, 16.8f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, -0.7f, 0.7f)
                lineTo(8.4f, 17.5f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, -0.7f, -0.7f)
                lineTo(7.7f, 7f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, 0.7f, -0.7f)
                lineToRelative(7f, 0f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, 0.7f, 0.7f)
                close()
            }
            path(
                fill = SolidColor(Color.LightGray),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // M11.9,15.05m-1.05,0a1.05,1.05 0,1 1,2.1 0a1.05,1.05 0,1 1,-2.1 0
                moveTo(11.9f, 15.05f)
                moveToRelative(-1.05f, 0f)
                arcToRelative(1.05f, 1.05f, 0f, true, true, 2.1f, 0f)
                arcToRelative(1.05f, 1.05f, 0f, true, true, -2.1f, 0f)
            }
            path(
                fill = SolidColor(Color.LightGray),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // M13.65,7.7l-3.5,0a0.7,0.7 0,0 0,0 1.4l3.5,0a0.7,0.7 0,0 0,0 -1.4z
                moveTo(13.65f, 7.7f)
                lineToRelative(-3.5f, 0f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, 0f, 1.4f)
                lineToRelative(3.5f, 0f)
                arcToRelative(0.7f, 0.7f, 0f, false, false, 0f, -1.4f)
                close()
            }
        }.build()
        return _phoneIcon!!
    }

private var _phoneIcon: ImageVector? = null
