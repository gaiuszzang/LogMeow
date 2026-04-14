package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val QuestionIcon: ImageVector
    get() {
        if (_questionIcon != null) {
            return _questionIcon!!
        }
        _questionIcon = ImageVector.Builder(
            name = "QuestionIcon",
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
                // Circle
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, true, false, 12f, 22f)
                arcTo(10f, 10f, 0f, true, false, 12f, 2f)
                close()
                moveTo(12f, 20f)
                arcTo(8f, 8f, 0f, true, true, 12f, 4f)
                arcTo(8f, 8f, 0f, true, true, 12f, 20f)
                close()
            }
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                // Question mark
                moveTo(12f, 17f)
                arcToRelative(1f, 1f, 0f, true, true, 0f, -2f)
                arcToRelative(1f, 1f, 0f, true, true, 0f, 2f)
                close()
                moveTo(12.5f, 13f)
                horizontalLineToRelative(-1.5f)
                verticalLineToRelative(-0.5f)
                arcToRelative(3f, 3f, 0f, false, true, 1.5f, -2.6f)
                arcToRelative(1.5f, 1.5f, 0f, true, false, -3f, -0.4f)
                horizontalLineTo(8f)
                arcToRelative(3f, 3f, 0f, true, true, 4.5f, 3.5f)
                close()
            }
        }.build()
        return _questionIcon!!
    }

private var _questionIcon: ImageVector? = null
