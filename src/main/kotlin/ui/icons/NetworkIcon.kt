package ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Globe icon with latitude/longitude grid lines
val NetworkIcon: ImageVector
    get() {
        if (_networkIcon != null) {
            return _networkIcon!!
        }
        _networkIcon = ImageVector.Builder(
            name = "NetworkIcon",
            defaultWidth = 22.dp,
            defaultHeight = 22.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val stroke = SolidColor(Color.LightGray)
            val strokeWidth = 1.6f
            // Outer circle
            path(
                stroke = stroke,
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Circle center (12,12) radius 8
                moveTo(20f, 12f)
                arcTo(8f, 8f, 0f, false, true, 12f, 20f)
                arcTo(8f, 8f, 0f, false, true, 4f, 12f)
                arcTo(8f, 8f, 0f, false, true, 20f, 12f)
                close()
            }
            // Vertical ellipse (longitude line)
            path(
                stroke = stroke,
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 12f)
                arcTo(4f, 8f, 0f, false, true, 12f, 20f)
                arcTo(4f, 8f, 0f, false, true, 8f, 12f)
                arcTo(4f, 8f, 0f, false, true, 16f, 12f)
                close()
            }
            // Horizontal line (equator)
            path(
                stroke = stroke,
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 12f)
                lineTo(20f, 12f)
            }
        }.build()
        return _networkIcon!!
    }

private var _networkIcon: ImageVector? = null
