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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import network.data.NetworkTrafficEntry
import ui.common.Direction
import ui.common.LazyListScrollBar
import io.groovin.logmeow.interceptor.ResponseType
import ui.theme.AppTheme
import ui.common.ContextDropdownMenu
import ui.common.DarkMenuItem
import ui.common.DragHandle
import ui.common.DropDownButton
import ui.theme.LocalLogMeowTheme
import ui.theme.LogMeowTheme
import vm.NetworkInspectorUiState
import vm.NetworkInspectorViewModel

@Composable
fun NetworkInspectorScreen(
    viewModel: NetworkInspectorViewModel,
    theme: LogMeowTheme,
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
        AppTheme(theme = theme) {
            val theme = LocalLogMeowTheme.current
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = theme.darkBackground
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
                                fontSize = theme.fontSizeBody,
                                fontWeight = FontWeight.Medium,
                                color = theme.textPrimary,
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
                                fontSize = theme.fontSizeBody,
                                fontWeight = FontWeight.Medium,
                                color = theme.textPrimary,
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
    val theme = LocalLogMeowTheme.current
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
                backgroundColor = theme.buttonBackground,
                contentColor = theme.textPrimary,
                disabledBackgroundColor = theme.disabledBackground,
                disabledContentColor = theme.textDim
            )
        ) {
            Text("Mock API", fontSize = theme.fontSizeBody)
        }

        Spacer(Modifier.width(8.dp))

        Button(
            onClick = onClear,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = theme.buttonBackground,
                contentColor = theme.textPrimary
            )
        ) {
            Text("Clear", fontSize = theme.fontSizeBody)
        }

        Spacer(Modifier.width(8.dp))

        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = theme.buttonBackground,
                contentColor = theme.textPrimary
            )
        ) {
            Text("Refresh", fontSize = theme.fontSizeBody)
        }
    }
}

@Composable
private fun AppSelector(
    connectedApps: Set<String>,
    selectedAppId: String?,
    onSelectApp: (String) -> Unit
) {
    val theme = LocalLogMeowTheme.current
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
                        fontSize = theme.fontSizeBody,
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
    val theme = LocalLogMeowTheme.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        Text(
            text = "Traffic (${trafficList.size})",
            fontSize = theme.fontSizeBody,
            fontWeight = FontWeight.Medium,
            color = theme.textPrimary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, theme.border, RoundedCornerShape(theme.cornerRadius))
                .background(theme.panelBackground)
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
                color = theme.scrollbarThumb,
                backgroundColor = theme.scrollbarTrack,
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
    val theme = LocalLogMeowTheme.current
    val backgroundColor = when {
        isSelected && isFocused -> theme.selectedFocused
        isSelected -> theme.selectedUnfocused
        else -> Color.Transparent
    }
    val methodColor = theme.methodColor(entry.method)
    val statusColor = theme.statusCodeColor(entry.statusCode)

    ContextDropdownMenu(
        items = {
            buildList {
                if (mockSupportEnabled) {
                    add(DarkMenuItem("Add to Mock API") { onMockApi() })
                }
                add(DarkMenuItem("Delete", color = theme.danger) { onDelete() })
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
                fontSize = theme.fontSizeLabel,
                fontWeight = FontWeight.Bold,
                color = methodColor,
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = entry.statusCode?.toString() ?: "...",
                fontSize = theme.fontSizeLabel,
                color = statusColor,
                modifier = Modifier.width(32.dp)
            )
            // Mock indicator
            if (entry.responseType == ResponseType.MOCK) {
                Text(
                    text = "M",
                    fontSize = theme.fontSizeBadge,
                    fontWeight = FontWeight.Bold,
                    color = theme.warning,
                    modifier = Modifier
                        .background(theme.mockBadgeBackground, RoundedCornerShape(theme.cornerRadiusSmall))
                        .padding(horizontal = 3.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = entry.path,
                fontSize = theme.fontSizeBody,
                color = theme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (entry.durationMs != null) {
                Text(
                    text = "${entry.durationMs}ms",
                    fontSize = theme.fontSizeLabel,
                    color = theme.textDim
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
    val theme = LocalLogMeowTheme.current
    Column(
        modifier = modifier
            .border(1.dp, theme.border, RoundedCornerShape(theme.cornerRadius))
            .background(theme.panelBackground, RoundedCornerShape(theme.cornerRadius))
    ) {
        if (entry == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a request", fontSize = theme.fontSizeBody, color = theme.textDim)
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
    val theme = LocalLogMeowTheme.current
    Column(
        modifier = modifier
            .border(1.dp, theme.border, RoundedCornerShape(theme.cornerRadius))
            .background(theme.panelBackground, RoundedCornerShape(theme.cornerRadius))
    ) {
        if (entry == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a request", fontSize = theme.fontSizeBody, color = theme.textDim)
            }
        } else if (!entry.isCompleted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Pending...", fontSize = theme.fontSizeBody, color = theme.textDim)
            }
        } else if (entry.error != null) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Error: ${entry.error}",
                    fontSize = theme.fontSizeBody,
                    color = theme.danger,
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
                    val statusColor = theme.statusCodeColor(entry.statusCode)
                    InfoRow("Status", "${entry.statusCode}", valueColor = statusColor)
                    if (entry.responseType == ResponseType.MOCK) {
                        InfoRow("Type", "Mock Response", valueColor = theme.warning)
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
    val theme = LocalLogMeowTheme.current
    Column {
        Text(
            text = title,
            fontSize = theme.fontSizeTitle,
            fontWeight = FontWeight.SemiBold,
            color = theme.accent,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Divider(color = theme.divider, thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = LocalLogMeowTheme.current.headerValue) {
    val theme = LocalLogMeowTheme.current
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "$label: ",
            fontSize = theme.fontSizeBody,
            color = theme.textDim,
            fontFamily = FontFamily.Monospace,
            lineHeight = 1.5.em
        )
        Text(
            text = value,
            fontSize = theme.fontSizeBody,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            lineHeight = 1.5.em
        )
    }
}

@Composable
private fun HeadersTable(headers: Map<String, String>) {
    val theme = LocalLogMeowTheme.current
    Column {
        headers.forEach { (key, value) ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(
                    text = key,
                    fontSize = theme.fontSizeBody,
                    fontWeight = FontWeight.Medium,
                    color = theme.headerKey,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 1.5.em
                )
                Text(
                    text = ": ",
                    fontSize = theme.fontSizeBody,
                    color = theme.textDim,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 1.5.em
                )
                Text(
                    text = value,
                    fontSize = theme.fontSizeBody,
                    color = theme.headerValue,
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
    val theme = LocalLogMeowTheme.current
    val isJson = isJsonBody(body)
    var prettyEnabled by remember { mutableStateOf(true) }

    // Header with Pretty Json toggle
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Body",
                fontSize = theme.fontSizeTitle,
                fontWeight = FontWeight.SemiBold,
                color = theme.accent,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            if (isJson) {
                DisableSelection {
                    Spacer(Modifier.width(8.dp))
                    Checkbox(
                        checked = prettyEnabled,
                        onCheckedChange = { prettyEnabled = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = theme.accent,
                            uncheckedColor = theme.textDim,
                            checkmarkColor = theme.textPrimary
                        ),
                        modifier = Modifier.size(14.dp).scale(0.75f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Pretty Json",
                        fontSize = theme.fontSizeLabel,
                        color = theme.textDim,
                        modifier = Modifier.clickable { prettyEnabled = !prettyEnabled }
                    )
                }
            }
        }
        Divider(color = theme.divider, thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
    }

    val displayText = if (isJson && prettyEnabled) tryFormatJson(body) else body
    Text(
        text = displayText,
        fontSize = theme.fontSizeBody,
        color = theme.textSecondary,
        fontFamily = FontFamily.Monospace,
        lineHeight = 1.6.em,
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.bodyBackground, RoundedCornerShape(theme.cornerRadius))
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
    val theme = LocalLogMeowTheme.current
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
                        if (isServerRunning) theme.serverRunning else theme.textDim,
                        RoundedCornerShape(theme.cornerRadius)
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isServerRunning) "Listening on :$serverPort" else "Server stopped",
                fontSize = theme.fontSizeLabel,
                color = theme.textSecondary
            )
        }
        if (connectedAppCount > 0) {
            Text(
                text = "Connected: $connectedAppCount",
                fontSize = theme.fontSizeLabel,
                color = theme.success
            )
        }
    }
}
