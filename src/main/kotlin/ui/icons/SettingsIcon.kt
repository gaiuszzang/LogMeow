package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SettingsIcon: ImageVector
    get() {
        if (_settingsIcon != null) {
            return _settingsIcon!!
        }
        _settingsIcon = ImageVector.Builder(
            name = "SettingsIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 30f,
            viewportHeight = 30f
        ).apply {
            group(translationX = 3f, translationY = 3f) {
                // Gear outer shape
                path(fill = SolidColor(Color.Black)) {
                    moveTo(8.61f, 22f)
                    arcToRelative(2.25f, 2.25f, 0f, false, true, -1.35f, -0.46f)
                    lineTo(5.19f, 20f)
                    arcToRelative(2.37f, 2.37f, 0f, false, true, -0.49f, -3.22f)
                    arcToRelative(2.06f, 2.06f, 0f, false, false, 0.23f, -1.86f)
                    lineToRelative(-0.06f, -0.16f)
                    arcToRelative(1.83f, 1.83f, 0f, false, false, -1.12f, -1.22f)
                    horizontalLineToRelative(-0.16f)
                    arcToRelative(2.34f, 2.34f, 0f, false, true, -1.48f, -2.94f)
                    lineTo(2.93f, 8f)
                    arcToRelative(2.18f, 2.18f, 0f, false, true, 1.12f, -1.41f)
                    arcToRelative(2.14f, 2.14f, 0f, false, true, 1.68f, -0.12f)
                    arcToRelative(1.93f, 1.93f, 0f, false, false, 1.78f, -0.29f)
                    lineToRelative(0.13f, -0.1f)
                    arcToRelative(1.94f, 1.94f, 0f, false, false, 0.73f, -1.51f)
                    verticalLineToRelative(-0.24f)
                    arcTo(2.32f, 2.32f, 0f, false, true, 10.66f, 2f)
                    horizontalLineToRelative(2.55f)
                    arcToRelative(2.26f, 2.26f, 0f, false, true, 1.6f, 0.67f)
                    arcToRelative(2.37f, 2.37f, 0f, false, true, 0.68f, 1.68f)
                    verticalLineToRelative(0.28f)
                    arcToRelative(1.76f, 1.76f, 0f, false, false, 0.69f, 1.43f)
                    lineToRelative(0.11f, 0.08f)
                    arcToRelative(1.74f, 1.74f, 0f, false, false, 1.59f, 0.26f)
                    lineToRelative(0.34f, -0.11f)
                    arcTo(2.26f, 2.26f, 0f, false, true, 21.1f, 7.8f)
                    lineToRelative(0.79f, 2.52f)
                    arcToRelative(2.36f, 2.36f, 0f, false, true, -1.46f, 2.93f)
                    lineToRelative(-0.2f, 0.07f)
                    arcTo(1.89f, 1.89f, 0f, false, false, 19f, 14.6f)
                    arcToRelative(2f, 2f, 0f, false, false, 0.25f, 1.65f)
                    lineToRelative(0.26f, 0.38f)
                    arcToRelative(2.38f, 2.38f, 0f, false, true, -0.5f, 3.23f)
                    lineTo(17f, 21.41f)
                    arcToRelative(2.24f, 2.24f, 0f, false, true, -3.22f, -0.53f)
                    lineToRelative(-0.12f, -0.17f)
                    arcToRelative(1.75f, 1.75f, 0f, false, false, -1.5f, -0.78f)
                    arcToRelative(1.8f, 1.8f, 0f, false, false, -1.43f, 0.77f)
                    lineToRelative(-0.23f, 0.33f)
                    arcTo(2.25f, 2.25f, 0f, false, true, 9f, 22f)
                    arcToRelative(2f, 2f, 0f, false, true, -0.39f, 0f)
                    close()
                    moveTo(4.4f, 11.62f)
                    arcToRelative(3.83f, 3.83f, 0f, false, true, 2.38f, 2.5f)
                    verticalLineToRelative(0.12f)
                    arcToRelative(4f, 4f, 0f, false, true, -0.46f, 3.62f)
                    arcToRelative(0.38f, 0.38f, 0f, false, false, 0f, 0.51f)
                    lineTo(8.47f, 20f)
                    arcToRelative(0.25f, 0.25f, 0f, false, false, 0.37f, -0.07f)
                    lineToRelative(0.23f, -0.33f)
                    arcToRelative(3.77f, 3.77f, 0f, false, true, 6.2f, 0f)
                    lineToRelative(0.12f, 0.18f)
                    arcToRelative(0.3f, 0.3f, 0f, false, false, 0.18f, 0.12f)
                    arcToRelative(0.25f, 0.25f, 0f, false, false, 0.19f, -0.05f)
                    lineToRelative(2.06f, -1.56f)
                    arcToRelative(0.36f, 0.36f, 0f, false, false, 0.07f, -0.49f)
                    lineToRelative(-0.26f, -0.38f)
                    arcTo(4f, 4f, 0f, false, true, 17.1f, 14f)
                    arcToRelative(3.92f, 3.92f, 0f, false, true, 2.49f, -2.61f)
                    lineToRelative(0.2f, -0.07f)
                    arcToRelative(0.34f, 0.34f, 0f, false, false, 0.19f, -0.44f)
                    lineToRelative(-0.78f, -2.49f)
                    arcToRelative(0.35f, 0.35f, 0f, false, false, -0.2f, -0.19f)
                    arcToRelative(0.21f, 0.21f, 0f, false, false, -0.19f, 0f)
                    lineToRelative(-0.34f, 0.11f)
                    arcToRelative(3.74f, 3.74f, 0f, false, true, -3.43f, -0.57f)
                    lineTo(15f, 7.65f)
                    arcToRelative(3.76f, 3.76f, 0f, false, true, -1.49f, -3f)
                    verticalLineToRelative(-0.31f)
                    arcToRelative(0.37f, 0.37f, 0f, false, false, -0.1f, -0.26f)
                    arcToRelative(0.31f, 0.31f, 0f, false, false, -0.21f, -0.08f)
                    horizontalLineToRelative(-2.54f)
                    arcToRelative(0.31f, 0.31f, 0f, false, false, -0.29f, 0.33f)
                    verticalLineToRelative(0.25f)
                    arcToRelative(3.9f, 3.9f, 0f, false, true, -1.52f, 3.09f)
                    lineToRelative(-0.13f, 0.1f)
                    arcToRelative(3.91f, 3.91f, 0f, false, true, -3.63f, 0.59f)
                    arcToRelative(0.22f, 0.22f, 0f, false, false, -0.14f, 0f)
                    arcToRelative(0.28f, 0.28f, 0f, false, false, -0.12f, 0.15f)
                    lineTo(4f, 11.12f)
                    arcToRelative(0.36f, 0.36f, 0f, false, false, 0.22f, 0.45f)
                    close()
                }
                // Center circle
                path(fill = SolidColor(Color.Black)) {
                    moveTo(12f, 15.5f)
                    arcToRelative(3.5f, 3.5f, 0f, true, true, 3.5f, -3.5f)
                    arcToRelative(3.5f, 3.5f, 0f, false, true, -3.5f, 3.5f)
                    close()
                    moveTo(12f, 10.5f)
                    arcToRelative(1.5f, 1.5f, 0f, true, false, 1.5f, 1.5f)
                    arcToRelative(1.5f, 1.5f, 0f, false, false, -1.5f, -1.5f)
                    close()
                }
            }
        }.build()
        return _settingsIcon!!
    }

private var _settingsIcon: ImageVector? = null
