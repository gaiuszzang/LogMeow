package ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em

// 공통 TextStyle - 텍스트 수직 정렬 개선 + 적절한 줄간격
private val centeredTextStyle = TextStyle(
    lineHeight = 1.4.em, // 적절한 줄 간격 (폰트 크기의 140%)
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

@Composable
fun AppTheme(
    theme: LogMeowTheme = IslandsDarkTheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = Colors(
            primary = theme.textDim,
            onPrimary = theme.textSecondary,
            primaryVariant = theme.border,
            secondary = theme.accent,
            onSecondary = theme.textSecondary,
            secondaryVariant = theme.success,
            background = theme.materialBackground,
            onBackground = theme.textSecondary,
            surface = theme.darkBackground,
            onSurface = theme.textSecondary,
            error = theme.danger,
            onError = theme.textSecondary,
            isLight = false
        ),
        shapes = Shapes(
            small = RoundedCornerShape(theme.cornerRadiusSmall),
            medium = RoundedCornerShape(theme.cornerRadius),
            large = RoundedCornerShape(theme.cornerRadius)
        ),
        typography = Typography().let { defaultTypography ->
            Typography(
                h1 = defaultTypography.h1.merge(centeredTextStyle),
                h2 = defaultTypography.h2.merge(centeredTextStyle),
                h3 = defaultTypography.h3.merge(centeredTextStyle),
                h4 = defaultTypography.h4.merge(centeredTextStyle),
                h5 = defaultTypography.h5.merge(centeredTextStyle),
                h6 = defaultTypography.h6.merge(centeredTextStyle),
                subtitle1 = defaultTypography.subtitle1.merge(centeredTextStyle),
                subtitle2 = defaultTypography.subtitle2.merge(centeredTextStyle),
                body1 = defaultTypography.body1.merge(centeredTextStyle),
                body2 = defaultTypography.body2.merge(centeredTextStyle),
                button = defaultTypography.button.merge(centeredTextStyle),
                caption = defaultTypography.caption.merge(centeredTextStyle),
                overline = defaultTypography.overline.merge(centeredTextStyle)
            )
        }
    ) {
        CompositionLocalProvider(LocalLogMeowTheme provides theme) {
            AppLocalCommonProvider {
                Box(
                    modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colors.surface)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AppLocalCommonProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colors.onSurface
    ) {
        content()
    }
}
