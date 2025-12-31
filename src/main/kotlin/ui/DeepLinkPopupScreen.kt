package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import org.koin.compose.koinInject
import ui.common.AppTheme
import ui.common.SingleLineTextField
import vm.DeepLinkPopupViewModel

@Composable
fun DeepLinkPopupScreen(
    viewModel: DeepLinkPopupViewModel = koinInject(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Window(
        onCloseRequest = onDismiss,
        title = "DeepLink",
        state = rememberWindowState(
            width = 600.dp,
            height = 500.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )
    ) {
        AppTheme {
            Surface(
                modifier = Modifier.padding(16.dp),
                color = Color(0xFF2B2B2B)
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
                            fontSize = 14.sp,
                            color = Color.White
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
                            enabled = uiState.inputText.isNotBlank()
                        ) {
                            Text("Execute")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // History Title
                    Text(
                        text = "DeepLink History",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // History List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E1E1E))
                    ) {
                        items(uiState.history) { scheme ->
                            HistoryItem(
                                scheme = scheme,
                                onClick = { viewModel.executeHistoryItem(scheme) }
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
    scheme: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = scheme,
            fontSize = 12.sp,
            color = Color.LightGray
        )
    }
}
