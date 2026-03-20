package ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

import ui.theme.LocalLogMeowTheme

data class DarkMenuItem(
    val label: String,
    val color: Color? = null,
    val onClick: () -> Unit
)

/**
 * Right-click context menu using Compose CursorDropdownMenu (dark themed).
 * Positions at the cursor location.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ContextDropdownMenu(
    items: () -> List<DarkMenuItem>,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press &&
                            event.button == PointerButton.Secondary
                        ) {
                            expanded = true
                        }
                    }
                }
            }
    ) {
        content()
        CursorDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items().forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expanded = false
                            item.onClick()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        fontSize = LocalLogMeowTheme.current.fontSizeBody,
                        color = item.color ?: LocalLogMeowTheme.current.textSecondary
                    )
                }
            }
        }
    }
}
