package ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ui.theme.LocalLogMeowTheme


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tintColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Unspecified,
    iconPadding: Modifier = Modifier,
    tooltip: String? = null,
    onClick: () -> Unit = {}
) {
    val theme = LocalLogMeowTheme.current
    val resolvedTint = if (tintColor == Color.Unspecified) theme.iconDefault else tintColor

    val button: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(theme.cornerRadius))
                .then(if (backgroundColor != Color.Unspecified) Modifier.background(backgroundColor) else Modifier)
                .clickable(enabled = enabled) { onClick() }
                .then(modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = iconPadding,
                imageVector = icon,
                contentDescription = tooltip,
                tint = if (enabled) resolvedTint else theme.border
            )
        }
    }

    if (tooltip != null) {
        TooltipArea(
            tooltip = {
                Box(
                    modifier = Modifier
                        .background(theme.materialBackground, RoundedCornerShape(theme.cornerRadiusSmall))
                        .border(1.dp, theme.border, RoundedCornerShape(theme.cornerRadiusSmall))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tooltip,
                        fontSize = theme.fontSizeLabel,
                        color = theme.textPrimary
                    )
                }
            },
            tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
            content = { button() }
        )
    } else {
        button()
    }
}
