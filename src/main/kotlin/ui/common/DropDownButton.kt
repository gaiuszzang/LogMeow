package ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import ui.icons.ArrowDownIcon
import ui.theme.LocalLogMeowTheme

@Composable
fun DropDownButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val theme = LocalLogMeowTheme.current
    Row(
        modifier = Modifier
            .height(36.dp)
            .border(1.dp, theme.border, RoundedCornerShape(theme.cornerRadius))
            .clip(RoundedCornerShape(theme.cornerRadius))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = if (enabled) theme.textSecondary else theme.border,
            fontSize = theme.fontSizeBody,
            minLines = 1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        RowItemSpacer()
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = ArrowDownIcon,
            contentDescription = null,
            tint = if (enabled) theme.textDim else theme.border
        )
    }
}
