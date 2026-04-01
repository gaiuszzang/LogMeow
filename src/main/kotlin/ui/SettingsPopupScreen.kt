package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import ui.common.DropDownButton
import ui.common.SingleLineTextField
import ui.theme.AppTheme
import ui.theme.LocalLogMeowTheme
import ui.theme.LogMeowTheme
import ui.theme.availableThemes
import java.util.Properties

@Composable
fun SettingsPopupScreen(
    theme: LogMeowTheme,
    currentThemeName: String,
    currentMaxLogCount: Int,
    onThemeChange: (String) -> Unit,
    onMaxLogCountChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Window(
        onCloseRequest = onDismiss,
        title = "Settings",
        resizable = false,
        state = rememberWindowState(
            width = 450.dp,
            height = 240.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )
    ) {
        AppTheme(theme = theme) {
            val theme = LocalLogMeowTheme.current
            Surface(
                color = theme.darkBackground
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    // Theme Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Theme",
                            fontSize = theme.fontSizeBody,
                            color = theme.textPrimary
                        )
                        Spacer(Modifier.width(8.dp))

                        var themeDropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            DropDownButton(
                                modifier = Modifier.width(180.dp),
                                text = "[ $currentThemeName ]",
                                onClick = { themeDropdownExpanded = !themeDropdownExpanded }
                            )
                            DropdownMenu(
                                expanded = themeDropdownExpanded,
                                onDismissRequest = { themeDropdownExpanded = false }
                            ) {
                                availableThemes.forEach { (name, _) ->
                                    DropdownMenuItem(
                                        onClick = {
                                            onThemeChange(name)
                                            themeDropdownExpanded = false
                                        }
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = theme.fontSizeBody,
                                            color = theme.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Max Log Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Log Count",
                            fontSize = theme.fontSizeBody,
                            color = theme.textPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        var maxLogCountText by remember { mutableStateOf(currentMaxLogCount.toString()) }
                        SingleLineTextField(
                            value = maxLogCountText,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) {
                                    maxLogCountText = newValue
                                    newValue.toIntOrNull()?.let { onMaxLogCountChange(it) }
                                }
                            },
                            modifier = Modifier.width(180.dp)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // App Version
                    val appVersion = remember {
                        val props = Properties()
                        Thread.currentThread().contextClassLoader?.getResourceAsStream("version.properties")?.use {
                            props.load(it)
                        }
                        props.getProperty("desktop.app.version", "dev")
                    }
                    Text(
                        text = "v$appVersion",
                        fontSize = 10.sp,
                        color = theme.textDim
                    )
                }
            }
        }
    }
}
