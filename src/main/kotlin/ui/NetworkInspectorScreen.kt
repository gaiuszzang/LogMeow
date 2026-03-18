package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import network.data.NetworkTrafficEntry
import ui.common.Direction
import ui.common.LazyListScrollBar
import network.data.ResponseType
import ui.common.AppTheme
import ui.common.ContextDropdownMenu
import ui.common.DarkMenuItem
import ui.common.DragHandle
import ui.common.DropDownButton
import ui.common.LogMeowColors
import vm.NetworkInspectorUiState
import vm.NetworkInspectorViewModel

@Composable
fun NetworkInspectorScreen(
    viewModel: NetworkInspectorViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(viewModel) {
        onDispose { viewModel.onCleared() }
    }

    Window(
        onCloseRequest = { onDismiss() },
        title = "Network Inspector",
        state = rememberWindowState(
            width = 1400.dp,
            height = 900.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )
    ) {
        AppTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = LogMeowColors.DarkBackground
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(
                        connectedApps = uiState.connectedApps,
                        selectedAppId = uiState.selectedAppId,
                        onSelectApp = { viewModel.selectApp(it) },
                        onMockApi = { viewModel.showMockApiDialog() },
                        onClear = { viewModel.clearTraffic() },
                        onRefresh = { viewModel.restartServer() },
                        mockSupportEnabled = uiState.mockSupportEnabled
                    )

                    // Horizontal split: TrafficList | Detail
                    var horizontalSplitFraction by remember { mutableStateOf(0.3f) }
                    var totalWidth by remember { mutableStateOf(0f) }
                    val density = LocalDensity.current

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .onSizeChanged { totalWidth = it.width.toFloat() }
                    ) {
                        TrafficList(
                            trafficList = uiState.trafficList,
                            selectedTraffic = uiState.selectedTraffic,
                            onSelect = { viewModel.selectTraffic(it) },
                            onDeleteTraffic = { viewModel.deleteTraffic(it) },
                            onMockApi = { viewModel.showMockApiDialogForTraffic(it) },
                            mockSupportEnabled = uiState.mockSupportEnabled,
                            modifier = Modifier
                                .weight(horizontalSplitFraction)
                                .fillMaxHeight()
                        )

                        // Vertical drag handle
                        DragHandle(
                            isVertical = true,
                            onDrag = { delta ->
                                if (totalWidth > 0) {
                                    val px = with(density) { delta.dp.toPx() }
                                    horizontalSplitFraction = (horizontalSplitFraction + px / totalWidth)
                                        .coerceIn(0.15f, 0.6f)
                                }
                            }
                        )

                        // Vertical split: Request / Response
                        var verticalSplitFraction by remember { mutableStateOf(0.5f) }
                        var totalHeight by remember { mutableStateOf(0f) }

                        Column(
                            modifier = Modifier
                                .weight(1f - horizontalSplitFraction)
                                .fillMaxHeight()
                                .onSizeChanged { totalHeight = it.height.toFloat() }
                        ) {
                            // Request title
                            Text(
                                text = "Request",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            RequestDetailPanel(
                                entry = uiState.selectedTraffic,
                                modifier = Modifier
                                    .weight(verticalSplitFraction)
                                    .fillMaxWidth()
                            )

                            // Horizontal drag handle
                            DragHandle(
                                isVertical = false,
                                onDrag = { delta ->
                                    if (totalHeight > 0) {
                                        val px = with(density) { delta.dp.toPx() }
                                        verticalSplitFraction = (verticalSplitFraction + px / totalHeight)
                                            .coerceIn(0.15f, 0.85f)
                                    }
                                }
                            )

                            // Response title
                            Text(
                                text = "Response",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            ResponseDetailPanel(
                                entry = uiState.selectedTraffic,
                                modifier = Modifier
                                    .weight(1f - verticalSplitFraction)
                                    .fillMaxWidth()
                            )
                        }
                    }

                    StatusBar(
                        isServerRunning = uiState.isServerRunning,
                        serverPort = uiState.serverPort,
                        connectedAppCount = uiState.connectedApps.size
                    )
                }
            }

            if (uiState.showMockApiDialog && uiState.selectedAppId != null) {
                MockApiSettingScreen(
                    mockApiSettings = uiState.mockApiSettings,
                    onAdd = { viewModel.addMockApiSetting(it) },
                    onUpdate = { viewModel.updateMockApiSetting(it) },
                    onDelete = { viewModel.deleteMockApiSetting(it) },
                    onDismiss = { viewModel.hideMockApiDialog() },
                    dialogRequest = uiState.mockApiDialogRequest
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    connectedApps: Set<String>,
    selectedAppId: String?,
    onSelectApp: (String) -> Unit,
    onMockApi: () -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    mockSupportEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App selector dropdown (same style as device combobox)
        AppSelector(
            connectedApps = connectedApps,
            selectedAppId = selectedAppId,
            onSelectApp = onSelectApp
        )

        Spacer(modifier = Modifier.weight(1f))

        // Mock API button
        Button(
            onClick = onMockApi,
            enabled = selectedAppId != null && mockSupportEnabled,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = LogMeowColors.ButtonBackground,
                contentColor = Color.White,
                disabledBackgroundColor = LogMeowColors.DisabledBackground,
                disabledContentColor = Color.Gray
            )
        ) {
            Text("Mock API", fontSize = 12.sp)
        }

        Spacer(Modifier.width(8.dp))

        Button(
            onClick = onClear,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = LogMeowColors.ButtonBackground,
                contentColor = Color.White
            )
        ) {
            Text("Clear", fontSize = 12.sp)
        }

        Spacer(Modifier.width(8.dp))

        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = LogMeowColors.ButtonBackground,
                contentColor = Color.White
            )
        ) {
            Text("Refresh", fontSize = 12.sp)
        }
    }
}

@Composable
private fun AppSelector(
    connectedApps: Set<String>,
    selectedAppId: String?,
    onSelectApp: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (connectedApps.isEmpty()) {
        "[ No Apps ]"
    } else {
        selectedAppId ?: "[ Select App ]"
    }

    Box {
        DropDownButton(
            modifier = Modifier.width(300.dp),
            text = displayText,
            enabled = connectedApps.isNotEmpty(),
            onClick = { expanded = !expanded }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            connectedApps.forEach { appId ->
                DropdownMenuItem(onClick = {
                    onSelectApp(appId)
                    expanded = false
                }) {
                    Text(
                        text = appId,
                        fontSize = 12.sp,
                        fontWeight = if (appId == selectedAppId) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun TrafficList(
    trafficList: List<NetworkTrafficEntry>,
    selectedTraffic: NetworkTrafficEntry?,
    onSelect: (NetworkTrafficEntry) -> Unit,
    onDeleteTraffic: (NetworkTrafficEntry) -> Unit,
    onMockApi: (NetworkTrafficEntry) -> Unit,
    mockSupportEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Text(
            text = "Traffic (${trafficList.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                .background(LogMeowColors.PanelBackground)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 8.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.hasFocus }
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        val currentIndex = trafficList.indexOfFirst { it.id == selectedTraffic?.id }
                        when (event.key) {
                            Key.DirectionUp -> {
                                if (currentIndex > 0) {
                                    val newIndex = currentIndex - 1
                                    onSelect(trafficList[newIndex])
                                    coroutineScope.launch { listState.animateScrollToItem(newIndex) }
                                }
                                true
                            }
                            Key.DirectionDown -> {
                                if (currentIndex < trafficList.size - 1) {
                                    val newIndex = currentIndex + 1
                                    onSelect(trafficList[newIndex])
                                    coroutineScope.launch { listState.animateScrollToItem(newIndex) }
                                }
                                true
                            }
                            Key.PageUp -> {
                                coroutineScope.launch {
                                    val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                                    listState.animateScrollBy(-viewportHeight.toFloat())
                                    val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                                    if (firstVisible != null && firstVisible.index in trafficList.indices) {
                                        onSelect(trafficList[firstVisible.index])
                                    }
                                }
                                true
                            }
                            Key.PageDown -> {
                                coroutineScope.launch {
                                    val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                                    listState.animateScrollBy(viewportHeight.toFloat())
                                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                                    if (lastVisible != null && lastVisible.index in trafficList.indices) {
                                        onSelect(trafficList[lastVisible.index])
                                    }
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { focusRequester.requestFocus() }
            ) {
                itemsIndexed(trafficList) { _, entry ->
                    TrafficItem(
                        entry = entry,
                        isSelected = selectedTraffic?.id == entry.id,
                        isFocused = isFocused,
                        onClick = {
                            onSelect(entry)
                            focusRequester.requestFocus()
                        },
                        onDelete = { onDeleteTraffic(entry) },
                        onMockApi = { onMockApi(entry) },
                        mockSupportEnabled = mockSupportEnabled
                    )
                }
            }
            LazyListScrollBar(
                state = listState,
                direction = Direction.Vertical,
                thickness = 8.dp,
                color = Color.Gray,
                backgroundColor = Color.DarkGray,
            )
        }
    }
}

@Composable
private fun TrafficItem(
    entry: NetworkTrafficEntry,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMockApi: () -> Unit,
    mockSupportEnabled: Boolean
) {
    val backgroundColor = when {
        isSelected && isFocused -> LogMeowColors.SelectedFocused
        isSelected -> LogMeowColors.SelectedUnfocused
        else -> Color.Transparent
    }
    val methodColor = LogMeowColors.methodColor(entry.method)
    val statusColor = LogMeowColors.statusCodeColor(entry.statusCode)

    ContextDropdownMenu(
        items = {
            buildList {
                if (mockSupportEnabled) {
                    add(DarkMenuItem("Add to Mock API") { onMockApi() })
                }
                add(DarkMenuItem("Delete", color = LogMeowColors.Danger) { onDelete() })
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(backgroundColor)
                .clickable { onClick() }
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.method,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = methodColor,
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = entry.statusCode?.toString() ?: "...",
                fontSize = 11.sp,
                color = statusColor,
                modifier = Modifier.width(32.dp)
            )
            // Mock indicator
            if (entry.responseType == ResponseType.MOCK) {
                Text(
                    text = "M",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = LogMeowColors.Warning,
                    modifier = Modifier
                        .background(LogMeowColors.MockBadgeBackground, RoundedCornerShape(2.dp))
                        .padding(horizontal = 3.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = entry.path,
                fontSize = 12.sp,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (entry.durationMs != null) {
                Text(
                    text = "${entry.durationMs}ms",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


@Composable
private fun RequestDetailPanel(
    entry: NetworkTrafficEntry?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
            .background(LogMeowColors.PanelBackground, RoundedCornerShape(4.dp))
    ) {
        if (entry == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a request", fontSize = 12.sp, color = LogMeowColors.Dim)
            }
        } else {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column {
                    // General
                    SectionHeader("General")
                    InfoRow("Method", entry.method)
                    InfoRow("URL", entry.url)
                    val contentType = entry.requestHeaders["Content-Type"]
                        ?: entry.requestHeaders["content-type"]
                    if (contentType != null) {
                        InfoRow("Content-Type", contentType)
                    }

                    // Headers
                    if (entry.requestHeaders.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Headers (${entry.requestHeaders.size})")
                        HeadersTable(entry.requestHeaders)
                    }

                    // Body
                    if (!entry.requestBody.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        BodySection(entry.requestBody)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponseDetailPanel(
    entry: NetworkTrafficEntry?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
            .background(LogMeowColors.PanelBackground, RoundedCornerShape(4.dp))
    ) {
        if (entry == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a request", fontSize = 12.sp, color = LogMeowColors.Dim)
            }
        } else if (!entry.isCompleted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pending...", fontSize = 12.sp, color = LogMeowColors.Dim)
            }
        } else if (entry.error != null) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Error: ${entry.error}",
                    fontSize = 12.sp,
                    color = LogMeowColors.Danger,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Column {
                    // General
                    SectionHeader("General")
                    val statusColor = LogMeowColors.statusCodeColor(entry.statusCode)
                    InfoRow("Status", "${entry.statusCode}", valueColor = statusColor)
                    if (entry.responseType == ResponseType.MOCK) {
                        InfoRow("Type", "Mock Response", valueColor = LogMeowColors.Warning)
                    }
                    if (entry.durationMs != null) {
                        InfoRow("Duration", "${entry.durationMs}ms")
                    }
                    val contentType = entry.responseHeaders["Content-Type"]
                        ?: entry.responseHeaders["content-type"]
                    if (contentType != null) {
                        InfoRow("Content-Type", contentType)
                    }

                    // Headers
                    if (entry.responseHeaders.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Headers (${entry.responseHeaders.size})")
                        HeadersTable(entry.responseHeaders)
                    }

                    // Body
                    if (!entry.responseBody.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        BodySection(entry.responseBody)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LogMeowColors.Accent,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Divider(color = LogMeowColors.Divider, thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = LogMeowColors.HeaderValue) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            color = LogMeowColors.Dim,
            fontFamily = FontFamily.Monospace,
            lineHeight = 1.5.em
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            lineHeight = 1.5.em
        )
    }
}

@Composable
private fun HeadersTable(headers: Map<String, String>) {
    Column {
        headers.forEach { (key, value) ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(
                    text = key,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LogMeowColors.HeaderKey,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 1.5.em
                )
                Text(
                    text = ": ",
                    fontSize = 12.sp,
                    color = LogMeowColors.Dim,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 1.5.em
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    color = LogMeowColors.HeaderValue,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 1.5.em
                )
            }
        }
    }
}

private fun isJsonBody(body: String): Boolean {
    val trimmed = body.trim()
    return trimmed.startsWith("{") || trimmed.startsWith("[")
}

@Composable
private fun BodySection(body: String) {
    val isJson = isJsonBody(body)
    var prettyEnabled by remember { mutableStateOf(true) }

    // Header with Pretty Json toggle
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Body",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LogMeowColors.Accent,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            if (isJson) {
                DisableSelection {
                    Spacer(Modifier.width(8.dp))
                    Checkbox(
                        checked = prettyEnabled,
                        onCheckedChange = { prettyEnabled = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = LogMeowColors.Accent,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White
                        ),
                        modifier = Modifier.size(14.dp).scale(0.75f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Pretty Json",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.clickable { prettyEnabled = !prettyEnabled }
                    )
                }
            }
        }
        Divider(color = LogMeowColors.Divider, thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
    }

    val displayText = if (isJson && prettyEnabled) tryFormatJson(body) else body
    Text(
        text = displayText,
        fontSize = 12.sp,
        color = Color.LightGray,
        fontFamily = FontFamily.Monospace,
        lineHeight = 1.6.em,
        modifier = Modifier
            .fillMaxWidth()
            .background(LogMeowColors.BodyBackground, RoundedCornerShape(4.dp))
            .padding(8.dp)
    )
}

private val prettyJson = kotlinx.serialization.json.Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

private fun tryFormatJson(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return raw
    return try {
        val element = prettyJson.parseToJsonElement(trimmed)
        prettyJson.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), element)
    } catch (_: Exception) {
        raw
    }
}

@Composable
private fun StatusBar(
    isServerRunning: Boolean,
    serverPort: Int,
    connectedAppCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(8.dp)
                    .background(
                        if (isServerRunning) LogMeowColors.ServerRunning else Color.Gray,
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isServerRunning) "Listening on :$serverPort" else "Server stopped",
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }
        if (connectedAppCount > 0) {
            Text(
                text = "Connected: $connectedAppCount",
                fontSize = 11.sp,
                color = LogMeowColors.Success
            )
        }
    }
}
