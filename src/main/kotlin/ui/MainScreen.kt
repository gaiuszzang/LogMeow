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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.common.AppTheme
import ui.common.LogMeowColors
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
import ui.icons.VideoIcon
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
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
    val uiState by viewModel.uiState.collectAsState()

    var deviceListExpanded by remember { mutableStateOf(false) }
    var logLevelFilterExpanded by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .focusRequester(focusRequester)
                .focusTarget()
                .onPreviewKeyEvent { keyEvent ->
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
                Text("Device List", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                RowItemSpacer()
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
                                    Text(device.id, fontSize = 12.sp)
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = device.state.name,
                                        color = when (device.state) {
                                            AdbDeviceState.DEVICE -> LogMeowColors.ServerRunning
                                            AdbDeviceState.OFFLINE -> Color.Red
                                            else -> Color.Gray
                                        },
                                        fontSize = 12.sp
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
                    enabled = selectedDevice != null,
                    onClick = { viewModel.toggleLogging() },
                )
                RowItemSpacer()
                // Log Clear Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = DeleteIcon,
                    onClick = { viewModel.clearLogs() }
                )
                RowItemDivider()
                // Screenshot Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = CameraIcon,
                    enabled = selectedDevice != null,
                    onClick = { viewModel.captureScreenshot() }
                )
                RowItemSpacer()
                // ScreenRecording Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = VideoIcon,
                    enabled = selectedDevice != null,
                    backgroundColor = if (isScreenRecording) Color.Red /* TODO */ else Color.Unspecified,
                    onClick = { viewModel.toggleScreenRecording() }
                )
                RowItemDivider()
                // DeepLink Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = NavigationIcon,
                    enabled = selectedDevice != null,
                    onClick = { viewModel.showDeepLinkPopup() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Scrcpy Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = PhoneIcon,
                    enabled = selectedDevice != null,
                    onClick = { viewModel.launchScrcpy() }
                )
                RowItemDivider()
                // Network Inspector Button
                IconButton(
                    modifier = Modifier.size(36.dp),
                    icon = NetworkIcon,
                    enabled = selectedDevice != null,
                    onClick = { viewModel.showNetworkInspector() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Filter Controls: LogLevel Filter + PID Filter + Tag Filter + Message Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LogLevel Filter Dropdown
                Text("LogLevelFilter", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                RowItemSpacer()
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
                            Text("All", fontSize = 12.sp)
                        }
                        LogLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.updateLogLevelFilter(level)
                                    logLevelFilterExpanded = false
                                }
                            ) {
                                Text(level.name, fontSize = 12.sp)
                            }
                        }
                    }
                }
                RowItemSpacer()
                // PID Filter Text Field
                Text("PIDFilter", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                RowItemSpacer()
                SingleLineTextField(
                    value = uiState.filterPid?.toString() ?: "",
                    onValueChange = {
                        val pid = it.toIntOrNull()
                        viewModel.updatePidFilter(pid)
                    },
                    modifier = Modifier.width(70.dp)
                )
                RowItemSpacer()
                // Tag Filter Text Field
                Text("TagFilter", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                RowItemSpacer()
                SingleLineTextField(
                    value = uiState.filterTag ?: "",
                    onValueChange = { viewModel.updateTagFilter(it.ifBlank { null }) },
                    modifier = Modifier.width(180.dp)
                )
                RowItemSpacer()
                // Message Filter Text Field
                Text("MessageFilter", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                RowItemSpacer()
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
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
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
                var isDisplayModeHovered by remember { mutableStateOf(false) }
                Text(
                    modifier = Modifier
                        .background(
                            color = when {
                                isCompactMode -> LogMeowColors.TextSelectionBackground
                                isDisplayModeHovered -> LogMeowColors.TextSelectionHoverBackground
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Enter -> isDisplayModeHovered = true
                                        PointerEventType.Exit -> isDisplayModeHovered = false
                                    }
                                }
                            }
                        }
                        .clickable { viewModel.toggleDisplayMode() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    text = if (isCompactMode) "Show Compact Mode" else "Show All Mode",
                    fontSize = 12.sp,
                    color = if (isCompactMode) Color.White else Color.Gray
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
                            fontSize = 12.sp,
                            color = Color.Gray
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
                        fontSize = 12.sp,
                        color = Color.Gray
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
                    onDismiss = { viewModel.hideNetworkInspector() }
                )
            }
        }
    }
}
