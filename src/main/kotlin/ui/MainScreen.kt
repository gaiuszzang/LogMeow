package ui

import adb.data.AdbDeviceState
import adb.data.LogLevel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.theme.AppTheme
import ui.theme.LocalLogMeowTheme
import ui.common.DropDownButton
import ui.common.SingleLineTextField
import ui.common.IconButton
import ui.common.RowItemDivider
import ui.common.RowItemSpacer
import ui.icons.CameraIcon
import ui.icons.ChevronLeftIcon
import ui.icons.ChevronRightIcon
import ui.icons.DeleteIcon
import ui.icons.NavigationIcon
import ui.icons.PhoneIcon
import ui.icons.PlayIcon
import ui.icons.StopIcon
import ui.icons.NetworkIcon
import ui.icons.SettingsIcon
import ui.icons.VideoIcon
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import ui.theme.themeByName
import vm.DeepLinkPopupViewModel
import vm.DisplayMode
import vm.MainViewModel
import vm.NetworkInspectorViewModel
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val isLogging by viewModel.isLogging.collectAsState()
    val isScreenRecording by viewModel.isScreenRecording.collectAsState()
    val isDeepLinkPopupVisible by viewModel.isDeepLinkPopupVisible.collectAsState()
    val isNetworkInspectorVisible by viewModel.isNetworkInspectorVisible.collectAsState()
    val isSettingsVisible by viewModel.isSettingsVisible.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var deviceListExpanded by remember { mutableStateOf(false) }
    var logLevelFilterExpanded by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AppTheme(theme = themeByName(settings.themeName)) {
        val theme = LocalLogMeowTheme.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .focusRequester(focusRequester)
                .focusTarget()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when {
                            // Handle Cmd+C (macOS) or Ctrl+C (Windows/Linux)
                            (keyEvent.isMetaPressed || keyEvent.isCtrlPressed) && keyEvent.key == Key.C -> {
                                val selectedLogsText = viewModel.getSelectedLogsAsText()
                                if (selectedLogsText.isNotEmpty()) {
                                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                    clipboard.setContents(StringSelection(selectedLogsText), null)
                                    true
                                } else {
                                    false
                                }
                            }
                            // Handle Space key for bookmark (same as Cmd+B)
                            !keyEvent.isMetaPressed && !keyEvent.isCtrlPressed && !keyEvent.isAltPressed && keyEvent.key == Key.Spacebar -> {
                                viewModel.toggleBookmarkForSelectedLogs()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
        ) {
            // Top Controls: Device List Dropdown + Log On/Off Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device List Dropdown
                Text("Device List", fontWeight = FontWeight.Medium, fontSize = theme.fontSizeBody)
                Spacer(Modifier.width(8.dp))
                Box {
                    DropDownButton(
                        modifier = Modifier
                            .width(300.dp),
                        text = if (devices.isEmpty()) {
                            "[ No Devices ]"
                        } else if (selectedDevice != null) {
                            "[ ${selectedDevice?.id} - ${selectedDevice?.state?.name} ]"
                        } else {
                            "[ Select Device ]"
                        },
                        enabled = devices.isNotEmpty() && !isDeepLinkPopupVisible,
                        onClick = { deviceListExpanded = !deviceListExpanded }
                    )
                    DropdownMenu(
                        expanded = deviceListExpanded,
                        onDismissRequest = { deviceListExpanded = false },
                        modifier = Modifier.width(300.dp)
                    ) {
                        devices.forEach { device ->
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.selectDevice(device)
                                    deviceListExpanded = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(device.id, fontSize = theme.fontSizeBody)
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = device.state.name,
                                        color = when (device.state) {
                                            AdbDeviceState.DEVICE -> theme.serverRunning
                                            AdbDeviceState.OFFLINE -> theme.danger
                                            else -> theme.textDim
                                        },
                                        fontSize = theme.fontSizeBody
                                    )
                                }
                            }
                        }
                    }
                }
                RowItemSpacer()
                // Log On/Off Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = if (isLogging) StopIcon else PlayIcon,
                    tintColor = if (isLogging) theme.stopIcon else theme.playIcon,
                    enabled = selectedDevice != null,
                    tooltip = if (isLogging) "Stop Logging" else "Start Logging",
                    onClick = { viewModel.toggleLogging() },
                )
                RowItemSpacer()
                // Log Clear Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = DeleteIcon,
                    tooltip = "Clear Logs",
                    onClick = { viewModel.clearLogs() }
                )
                RowItemDivider()
                // Screenshot Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = CameraIcon,
                    enabled = selectedDevice != null,
                    tooltip = "Screenshot",
                    onClick = { viewModel.captureScreenshot() }
                )
                RowItemSpacer()
                // ScreenRecording Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = VideoIcon,
                    enabled = selectedDevice != null,
                    backgroundColor = if (isScreenRecording) theme.danger else Color.Unspecified,
                    tooltip = if (isScreenRecording) "Stop Recording" else "Screen Recording",
                    onClick = { viewModel.toggleScreenRecording() }
                )
                RowItemDivider()
                // DeepLink Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = NavigationIcon,
                    enabled = selectedDevice != null,
                    tooltip = "DeepLink",
                    onClick = { viewModel.showDeepLinkPopup() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Scrcpy Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = PhoneIcon,
                    enabled = selectedDevice != null,
                    tooltip = "Scrcpy",
                    onClick = { viewModel.launchScrcpy() }
                )
                RowItemDivider()
                // Network Inspector Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = NetworkIcon,
                    enabled = selectedDevice != null,
                    tooltip = "Network Inspector",
                    onClick = { viewModel.showNetworkInspector() }
                )
                Spacer(Modifier.weight(1f))
                // Settings Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = SettingsIcon,
                    tooltip = "Settings",
                    onClick = { viewModel.showSettings() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Filter Controls: LogLevel Filter + PID Filter + Tag Filter + Message Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LogLevel Filter Dropdown
                Text("LogLevelFilter", fontWeight = FontWeight.Medium, fontSize = theme.fontSizeBody)
                RowItemSpacer(8.dp)
                Box {
                    DropDownButton(
                        modifier = Modifier.width(100.dp),
                        text = "[ ${uiState.logLevelFilter?.name ?: "All"} ]",
                        onClick = { logLevelFilterExpanded = !logLevelFilterExpanded }

                    )
                    DropdownMenu(
                        expanded = logLevelFilterExpanded,
                        onDismissRequest = { logLevelFilterExpanded = false }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                viewModel.updateLogLevelFilter(null)
                                logLevelFilterExpanded = false
                            }
                        ) {
                            Text("All", fontSize = theme.fontSizeBody)
                        }
                        LogLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.updateLogLevelFilter(level)
                                    logLevelFilterExpanded = false
                                }
                            ) {
                                Text(level.name, fontSize = theme.fontSizeBody)
                            }
                        }
                    }
                }
                RowItemSpacer(8.dp)
                // PID Filter Text Field
                Text("PIDFilter", fontWeight = FontWeight.Medium, fontSize = theme.fontSizeBody)
                RowItemSpacer(8.dp)
                SingleLineTextField(
                    value = uiState.filterPid?.toString() ?: "",
                    onValueChange = {
                        val pid = it.toIntOrNull()
                        viewModel.updatePidFilter(pid)
                    },
                    modifier = Modifier.width(70.dp)
                )
                RowItemSpacer(8.dp)
                // Tag Filter Text Field
                Text("TagFilter", fontWeight = FontWeight.Medium, fontSize = theme.fontSizeBody)
                RowItemSpacer(8.dp)
                SingleLineTextField(
                    value = uiState.filterTag ?: "",
                    onValueChange = { viewModel.updateTagFilter(it.ifBlank { null }) },
                    modifier = Modifier.width(180.dp)
                )
                RowItemSpacer(8.dp)
                // Message Filter Text Field
                Text("MessageFilter", fontWeight = FontWeight.Medium, fontSize = theme.fontSizeBody)
                RowItemSpacer(8.dp)
                SingleLineTextField(
                    value = uiState.filterMessage ?: "",
                    onValueChange = { viewModel.updateMessageFilter(it.ifBlank { null }) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Log View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, theme.border, RoundedCornerShape(theme.cornerRadius))
            ) {
                LogCatView(
                    uiState = uiState,
                    onLogClick = { id, isShift, isAlt ->
                        when {
                            isShift -> viewModel.selectRangeLog(id)
                            isAlt -> viewModel.toggleSingleLogSelection(id)
                            else -> viewModel.selectSingleLog(id)
                        }
                        focusRequester.requestFocus()
                    },
                    onLogDoubleClick = { id ->
                        viewModel.toggleBookmarkForLog(id)
                        focusRequester.requestFocus()
                    },
                    onDragSelect = { id ->
                        viewModel.selectRangeLog(id)
                    },
                    onKeyNavigate = { direction, extendSelection ->
                        viewModel.selectAdjacentLog(direction, extendSelection)
                    },
                    scrollToIndexFlow = viewModel.scrollToFilteredIndex
                )
            }

            // Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Display Mode toggle button
                val isCompactMode = uiState.displayMode == DisplayMode.Compact
                Text(
                    modifier = Modifier
                        .clickable { viewModel.toggleDisplayMode() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    text = if (isCompactMode) "Show Compact Mode" else "Show All Mode",
                    fontSize = theme.fontSizeBody,
                    color = if (isCompactMode) theme.textPrimary else theme.textDim
                )

                // Right side: Bookmarks and LogSize
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.bookmarkCount > 0) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            text = "Bookmarks : ${uiState.bookmarkCount}",
                            fontSize = theme.fontSizeBody,
                            color = theme.textDim
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            modifier = Modifier.size(18.dp),
                            icon = ChevronLeftIcon,
                            onClick = { viewModel.navigateToPreviousBookmark() }
                        )
                        Spacer(Modifier.width(2.dp))
                        IconButton(
                            modifier = Modifier.size(18.dp),
                            icon = ChevronRightIcon,
                            onClick = { viewModel.navigateToNextBookmark() }
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        text = "LogSize : ${uiState.allLogCount}",
                        fontSize = theme.fontSizeBody,
                        color = theme.textDim
                    )
                }
            }
        }

        // DeepLink Popup
        if (isDeepLinkPopupVisible) {
            val currentDevice = selectedDevice
            if (currentDevice != null) {
                val deepLinkViewModel: DeepLinkPopupViewModel = koinInject(
                    parameters = { parametersOf(currentDevice.id) }
                )
                DeepLinkPopupScreen(
                    viewModel = deepLinkViewModel,
                    theme = themeByName(settings.themeName),
                    onDismiss = { viewModel.hideDeepLinkPopup() }
                )
            }
        }

        // Network Inspector Popup
        if (isNetworkInspectorVisible) {
            val currentDevice = selectedDevice
            if (currentDevice != null) {
                val networkViewModel: NetworkInspectorViewModel = koinInject(
                    parameters = { parametersOf(currentDevice.id) }
                )
                NetworkInspectorScreen(
                    viewModel = networkViewModel,
                    theme = themeByName(settings.themeName),
                    onDismiss = { viewModel.hideNetworkInspector() }
                )
            }
        }

        // Settings Popup
        if (isSettingsVisible) {
            SettingsPopupScreen(
                theme = themeByName(settings.themeName),
                currentThemeName = settings.themeName,
                currentMaxLogCount = settings.maxLogCount,
                onThemeChange = { viewModel.updateTheme(it) },
                onMaxLogCountChange = { viewModel.updateMaxLogCount(it) },
                onDismiss = { viewModel.hideSettings() }
            )
        }
    }
}
