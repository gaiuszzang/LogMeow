package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import data.DeepLinkHistoryItem
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import ui.theme.AppTheme
import ui.common.IconButton
import ui.theme.LocalLogMeowTheme
import ui.theme.LogMeowTheme
import ui.common.SingleLineTextField
import ui.icons.DeleteIcon
import ui.icons.QuestionIcon
import vm.DeepLinkPopupViewModel

@Composable
fun DeepLinkPopupScreen(
    viewModel: DeepLinkPopupViewModel,
    theme: LogMeowTheme,
    focusRequest: StateFlow<Int>,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequestValue by focusRequest.collectAsState()

    Window(
        onCloseRequest = onDismiss,
        title = "DeepLink",
        state = rememberWindowState(
            width = 600.dp,
            height = 500.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )
    ) {
        LaunchedEffect(focusRequestValue) {
            window.toFront()
        }
        AppTheme(theme = theme) {
            val theme = LocalLogMeowTheme.current
            Surface(
                modifier = Modifier.padding(16.dp),
                color = theme.darkBackground
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DeepLink:",
                            fontSize = theme.fontSizeHeader,
                            color = theme.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SingleLineTextField(
                            value = uiState.inputText,
                            onValueChange = { viewModel.updateInputText(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.executeDeepLink() },
                            enabled = uiState.inputText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = theme.buttonBackground,
                                contentColor = theme.textPrimary,
                                disabledBackgroundColor = theme.disabledBackground,
                                disabledContentColor = theme.disabledContentColor
                            )
                        ) {
                            Text("Execute")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Additional Arguments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Arguments:",
                            fontSize = theme.fontSizeHeader,
                            color = theme.textPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SingleLineTextField(
                            value = uiState.extraArgs,
                            onValueChange = { viewModel.updateExtraArgs(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            icon = QuestionIcon,
                            modifier = Modifier.size(16.dp),
                            tintColor = theme.textSecondary,
                            tooltip = "Append extra adb arguments to the deep link command.\nExample: --es key \"value\" --ez flag true --ei count 5"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // History Title
                    Text(
                        text = "DeepLink History",
                        fontSize = theme.fontSizeHeader,
                        fontWeight = FontWeight.Medium,
                        color = theme.textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // History List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, theme.border, RoundedCornerShape(theme.cornerRadius))
                            .background(theme.panelBackground)
                    ) {
                        itemsIndexed(uiState.history) { index, item ->
                            HistoryItem(
                                item = item,
                                isSelected = uiState.selectedIndex == index,
                                onSelect = { viewModel.selectHistoryItem(index) },
                                onExecute = { viewModel.loadHistoryItem(item) },
                                onDelete = { viewModel.deleteHistoryItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    item: DeepLinkHistoryItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onExecute: () -> Unit,
    onDelete: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    val theme = LocalLogMeowTheme.current
    val backgroundColor = when {
        isSelected -> theme.selectedUnfocused
        isHovered -> theme.textSelectionHoverBackground
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(backgroundColor)
            .pointerInput(item) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                val now = System.currentTimeMillis()
                                if (now - lastClickTime < 300) {
                                    onExecute()
                                } else {
                                    onSelect()
                                }
                                lastClickTime = now
                            }
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Text(
                text = item.scheme,
                fontSize = theme.fontSizeBody,
                color = theme.textSecondary,
                maxLines = 1
            )
            if (item.extraArgs.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.extraArgs,
                    fontSize = theme.fontSizeBody,
                    color = theme.disabledContentColor,
                    maxLines = 1
                )
            }
        }
        if (isSelected) {
            IconButton(
                modifier = Modifier.size(20.dp),
                icon = DeleteIcon,
                onClick = onDelete
            )
        }
    }
}
