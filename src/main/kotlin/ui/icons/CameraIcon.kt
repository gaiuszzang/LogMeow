package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val CameraIcon: ImageVector
    get() {
        if (_cameraIcon != null) {
            return _cameraIcon!!
        }
        _cameraIcon = ImageVector.Builder(
            name = "CameraIcon",
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
                // M16.8,8.4l-2.1,0L14.7,7.35A1.75,1.75 0,0 0,12.95 5.6l-2.1,0A1.75,1.75 0,0 0,9.1 7.35L9.1,8.4L7,8.4a2.1,2.1 0,0 0,-2.1 2.1l0,5.6a2.1,2.1 0,0 0,2.1 2.1l9.8,0a2.1,2.1 0,0 0,2.1 -2.1l0,-5.6a2.1,2.1 0,0 0,-2.1 -2.1zM10.5,7.35a0.35,0.35 0,0 1,0.35 -0.35l2.1,0a0.35,0.35 0,0 1,0.35 0.35L13.3,8.4l-2.8,0zM17.5,16.1a0.7,0.7 0,0 1,-0.7 0.7L7,16.8a0.7,0.7 0,0 1,-0.7 -0.7l0,-5.6a0.7,0.7 0,0 1,0.7 -0.7l9.8,0a0.7,0.7 0,0 1,0.7 0.7z
                moveTo(16.8f, 8.4f)
                lineToRelative(-2.1f, 0f)
                lineTo(14.7f, 7.35f)
                arcTo(1.75f, 1.75f, 0f, false, false, 12.95f, 5.6f)
                lineToRelative(-2.1f, 0f)
                arcTo(1.75f, 1.75f, 0f, false, false, 9.1f, 7.35f)
                lineTo(9.1f, 8.4f)
                lineTo(7f, 8.4f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, -2.1f, 2.1f)
                lineToRelative(0f, 5.6f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, 2.1f)
                lineToRelative(9.8f, 0f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, 2.1f, -2.1f)
                lineToRelative(0f, -5.6f)
                arcToRelative(2.1f, 2.1f, 0f, false, false, -2.1f, -2.1f)
                close()
                moveTo(10.5f, 7.35f)
                arcToRelative(0.35f, 0.35f, 0f, false, true, 0.35f, -0.35f)
                lineToRelative(2.1f, 0f)
                arcToRelative(0.35f, 0.35f, 0f, false, true, 0.35f, 0.35f)
                lineTo(13.3f, 8.4f)
                lineToRelative(-2.8f, 0f)
                close()
                moveTo(17.5f, 16.1f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, -0.7f, 0.7f)
                lineTo(7f, 16.8f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, -0.7f, -0.7f)
                lineToRelative(0f, -5.6f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, 0.7f, -0.7f)
                lineToRelative(9.8f, 0f)
                arcToRelative(0.7f, 0.7f, 0f, false, true, 0.7f, 0.7f)
                close()
            }
            path(
                fill = SolidColor(Color.LightGray),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // M11.9,10.85a2.45,2.45 0,1 0,2.45 2.45,2.45 2.45,0 0,0 -2.45,-2.45zM11.9,14.35a1.05,1.05 0,1 1,1.05 -1.05,1.05 1.05,0 0,1 -1.05,1.05z
                moveTo(11.9f, 10.85f)
                arcToRelative(2.45f, 2.45f, 0f, true, false, 2.45f, 2.45f)
                arcToRelative(2.45f, 2.45f, 0f, false, false, -2.45f, -2.45f)
                close()
                moveTo(11.9f, 14.35f)
                arcToRelative(1.05f, 1.05f, 0f, true, true, 1.05f, -1.05f)
                arcToRelative(1.05f, 1.05f, 0f, false, true, -1.05f, 1.05f)
                close()
            }
        }.build()
        return _cameraIcon!!
    }

private var _cameraIcon: ImageVector? = null
