package ui

import adb.data.LogcatMessage
import adb.data.LogLevel
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ui.common.LazyListScrollBar
import ui.theme.LocalLogMeowTheme
import vm.DisplayMode
import vm.UiState

@Composable
fun LogCatView(
    uiState: UiState,
    onLogClick: (id: Long, isShift: Boolean, isAlt: Boolean) -> Unit,
    onLogDoubleClick: (id: Long) -> Unit = {},
    onDragSelect: (id: Long) -> Unit = {},
    onKeyNavigate: (direction: Int, extendSelection: Boolean) -> Int? = { _, _ -> null },
    scrollToIndexFlow: kotlinx.coroutines.flow.Flow<Int>? = null // TODO m.c.shin 개선 필요
) {
    val listState = rememberLazyListState()
    val filteredLogs = uiState.filteredLogs
    val isCompact = uiState.displayMode == DisplayMode.Compact
    var isDragging by remember { mutableStateOf(false) }
    var dragMouseY by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to the bottom when a new log is added
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.scrollToItem(filteredLogs.size - 1)
        }
    }

    // Scroll to specific index when requested (bookmark navigation)
    LaunchedEffect(scrollToIndexFlow) {
        scrollToIndexFlow?.collect { targetIndex ->
            if (targetIndex in 0 until listState.layoutInfo.totalItemsCount) {
                listState.scrollToItem(targetIndex)
            }
        }
    }

    // Auto-scroll while dragging outside the list bounds
    LaunchedEffect(isDragging, dragMouseY, containerHeight) {
        if (!isDragging) return@LaunchedEffect

        while (isDragging) {
            val scrollAmount = when {
                dragMouseY < 0 -> -1 // Scroll up
                dragMouseY > containerHeight -> 1 // Scroll down
                else -> 0
            }

            if (scrollAmount != 0) {
                val firstVisibleIndex = listState.firstVisibleItemIndex
                val targetIndex = (firstVisibleIndex + scrollAmount).coerceIn(0, filteredLogs.size - 1)
                listState.scrollToItem(targetIndex)

                // Select the item at the edge
                if (filteredLogs.isNotEmpty()) {
                    val selectIndex = if (scrollAmount < 0) {
                        listState.firstVisibleItemIndex
                    } else {
                        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: (filteredLogs.size - 1)
                    }
                    if (selectIndex in filteredLogs.indices) {
                        onDragSelect(filteredLogs[selectIndex].id)
                    }
                }
            }
            delay(50) // Scroll speed
        }
    }

    val theme = LocalLogMeowTheme.current
    if (filteredLogs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.panelBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Logs will appear here...", fontSize = theme.fontSizeBody)
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.panelBackground)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.hasFocus }
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val isShift = event.isShiftPressed
                    when (event.key) {
                        Key.DirectionUp -> {
                            val targetIndex = onKeyNavigate(-1, isShift)
                            if (targetIndex != null) {
                                coroutineScope.launch {
                                    val firstVisible = listState.firstVisibleItemIndex
                                    if (targetIndex < firstVisible) {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            val targetIndex = onKeyNavigate(1, isShift)
                            if (targetIndex != null) {
                                coroutineScope.launch {
                                    val layoutInfo = listState.layoutInfo
                                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                                    val isOutOfView = lastVisible == null || targetIndex > lastVisible.index ||
                                        (targetIndex == lastVisible.index &&
                                            lastVisible.offset + lastVisible.size > layoutInfo.viewportEndOffset)
                                    if (isOutOfView) {
                                        // Scroll so the target item appears at the bottom
                                        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                                        val avgItemSize = layoutInfo.visibleItemsInfo.map { it.size }.average().toInt()
                                        val itemsPerPage = if (avgItemSize > 0) viewportHeight / avgItemSize else 1
                                        val scrollTo = (targetIndex - itemsPerPage + 1).coerceAtLeast(0)
                                        listState.animateScrollToItem(scrollTo)
                                    }
                                }
                            }
                            true
                        }
                        Key.MoveHome -> {
                            val targetIndex = onKeyNavigate(-Int.MAX_VALUE, isShift)
                            if (targetIndex != null) {
                                coroutineScope.launch { listState.animateScrollToItem(0) }
                            }
                            true
                        }
                        Key.MoveEnd -> {
                            val targetIndex = onKeyNavigate(Int.MAX_VALUE, isShift)
                            if (targetIndex != null) {
                                coroutineScope.launch { listState.animateScrollToItem(filteredLogs.size - 1) }
                            }
                            true
                        }
                        Key.PageUp -> {
                            coroutineScope.launch {
                                val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                                listState.animateScrollBy(-viewportHeight.toFloat())
                                val firstVisible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                                if (firstVisible != null) {
                                    val currentIndex = filteredLogs.indexOfFirst { it.isSelected }
                                    val targetIndex = firstVisible.index
                                    if (currentIndex != -1 && targetIndex != currentIndex) {
                                        onKeyNavigate(targetIndex - currentIndex, isShift)
                                    }
                                }
                            }
                            true
                        }
                        Key.PageDown -> {
                            coroutineScope.launch {
                                val viewportHeight = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                                listState.animateScrollBy(viewportHeight.toFloat())
                                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                                if (lastVisible != null) {
                                    val currentIndex = filteredLogs.indexOfLast { it.isSelected }
                                    val targetIndex = lastVisible.index
                                    if (currentIndex != -1 && targetIndex != currentIndex) {
                                        onKeyNavigate(targetIndex - currentIndex, isShift)
                                    }
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
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp)
                    .pointerInput(filteredLogs) {
                        containerHeight = size.height.toFloat()
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        val isAlt = event.keyboardModifiers.isMetaPressed || event.keyboardModifiers.isCtrlPressed
                                        isDragging = !isAlt
                                    }
                                    PointerEventType.Release -> {
                                        isDragging = false
                                    }
                                    PointerEventType.Move -> {
                                        if (isDragging && event.buttons.isPrimaryPressed) {
                                            val y = event.changes.firstOrNull()?.position?.y ?: 0f
                                            dragMouseY = y
                                            // Find the item at the current Y position
                                            val layoutInfo = listState.layoutInfo
                                            for (itemInfo in layoutInfo.visibleItemsInfo) {
                                                if (y >= itemInfo.offset && y < itemInfo.offset + itemInfo.size) {
                                                    val index = itemInfo.index
                                                    if (index in filteredLogs.indices) {
                                                        onDragSelect(filteredLogs[index].id)
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                items(filteredLogs.size) { index ->
                    val log = filteredLogs[index]
                    LogRow(
                        modifier = Modifier.fillMaxWidth(),
                        log = log,
                        filterTag = uiState.filterTag,
                        filterMessage = uiState.filterMessage,
                        isCompact = isCompact,
                        isListFocused = isFocused,
                        onLogClick = { isShift, isAlt ->
                            onLogClick(log.id, isShift, isAlt)
                            focusRequester.requestFocus()
                        },
                        onLogDoubleClick = {
                            onLogDoubleClick(log.id)
                        }
                    )
                }
            }
            LazyListScrollBar(
                state = listState,
                direction = ui.common.Direction.Vertical,
                thickness = 12.dp,
                color = theme.scrollbarThumb,
                backgroundColor = theme.scrollbarTrack,
                isAlwaysDisplay = true,
                bookmarkedIndices = uiState.bookmarkedIndicesInFilteredLogs,
                totalItemCount = filteredLogs.size
            )
        }
    }
}

@Composable
private fun LogRow(
    log: LogcatMessage,
    filterTag: String?,
    filterMessage: String?,
    isCompact: Boolean,
    isListFocused: Boolean = false,
    onLogClick: (isShift: Boolean, isAlt: Boolean) -> Unit,
    onLogDoubleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }

    val theme = LocalLogMeowTheme.current
    val logColor = when (log.level) {
        LogLevel.VERBOSE -> theme.logVerbose
        LogLevel.DEBUG -> theme.logDebug
        LogLevel.INFO -> theme.logInfo
        LogLevel.WARN -> theme.logWarn
        LogLevel.ERROR, LogLevel.FATAL -> theme.logError
    }

    val rowBackgroundColor = when {
        log.isSelected && log.isBookmarked && isListFocused -> theme.selectedBookmarkFocused
        log.isSelected && log.isBookmarked -> theme.selectedBookmarkUnfocused
        log.isSelected && isListFocused -> theme.selectedFocused
        log.isSelected -> theme.selectedUnfocused
        log.isBookmarked && isHovered -> theme.bookmarkHoverBackground
        log.isBookmarked -> theme.bookmarkBackground
        isHovered -> theme.textSelectionHoverBackground
        else -> theme.panelBackground
    }

    Row(
        modifier = modifier
            .background(rowBackgroundColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                val now = System.currentTimeMillis()
                                if (now - lastClickTime < 300) {
                                    onLogDoubleClick()
                                } else {
                                    val isShift = event.keyboardModifiers.isShiftPressed
                                    val isAlt = event.keyboardModifiers.isMetaPressed || event.keyboardModifiers.isCtrlPressed
                                    onLogClick(isShift, isAlt)
                                }
                                lastClickTime = now
                            }
                            PointerEventType.Enter -> {
                                isHovered = true
                            }
                            PointerEventType.Exit -> {
                                isHovered = false
                            }
                        }
                    }
                }
            }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (isCompact) {
            CompactLogRowContent(log, filterMessage, logColor)
        } else {
            FullLogRowContent(log, filterTag, filterMessage, logColor)
        }
    }
}

@Composable
private fun RowScope.CompactLogRowContent(
    log: LogcatMessage,
    filterMessage: String?,
    logColor: Color
) {
    MessageText(log.message, filterMessage, logColor, Modifier.weight(1f))
}

@Composable
private fun RowScope.FullLogRowContent(
    log: LogcatMessage,
    filterTag: String?,
    filterMessage: String?,
    logColor: Color
) {
    val theme = LocalLogMeowTheme.current
    // Timestamp
    Text(
        text = log.timestamp,
        color = theme.textDim,
        fontSize = theme.fontSizeBody,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        minLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(180.dp)
    )

    // Level
    Text(
        text = log.level.name.first().toString(),
        color = logColor,
        fontSize = theme.fontSizeBody,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.width(30.dp)
    )

    // PID
    Text(
        text = log.pid.toString(),
        color = theme.textDim,
        fontSize = theme.fontSizeBody,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        minLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(50.dp)
    )

    // TID
    Text(
        text = log.tid.toString(),
        color = theme.textDim,
        fontSize = theme.fontSizeBody,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        minLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(50.dp)
    )

    // Tag
    Box(modifier = Modifier.width(220.dp)) {
        if (!filterTag.isNullOrBlank()) {
            Text(
                text = buildHighlightedText(log.tag, filterTag, MaterialTheme.colors.onSurface.copy(alpha = 0.8f)),
                fontSize = theme.fontSizeBody,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = log.tag,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                fontSize = theme.fontSizeBody,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    // Message
    MessageText(log.message, filterMessage, logColor, Modifier.weight(1f))
}

@Composable
private fun MessageText(
    message: String,
    filterMessage: String?,
    logColor: Color,
    modifier: Modifier
) {
    val theme = LocalLogMeowTheme.current
    if (!filterMessage.isNullOrBlank()) {
        Text(
            text = buildHighlightedText(message, filterMessage, logColor),
            fontSize = theme.fontSizeBody,
            fontFamily = FontFamily.Monospace,
            modifier = modifier
        )
    } else {
        Text(
            text = message,
            color = logColor,
            fontSize = theme.fontSizeBody,
            fontFamily = FontFamily.Monospace,
            modifier = modifier
        )
    }
}

@Composable
private fun buildHighlightedText(
    text: String,
    filter: String,
    baseColor: Color
) = buildAnnotatedString {
    val theme = LocalLogMeowTheme.current
    val surfaceColor = MaterialTheme.colors.surface
    var currentIndex = 0
    val lowerText = text.lowercase()
    val lowerFilter = filter.lowercase()

    while (currentIndex < text.length) {
        val matchIndex = lowerText.indexOf(lowerFilter, currentIndex)
        if (matchIndex == -1) {
            withStyle(style = SpanStyle(color = baseColor)) {
                append(text.substring(currentIndex))
            }
            break
        } else {
            if (matchIndex > currentIndex) {
                withStyle(style = SpanStyle(color = baseColor)) {
                    append(text.substring(currentIndex, matchIndex))
                }
            }
            withStyle(style = SpanStyle(
                color = surfaceColor,
                background = theme.highlightBackground
            )) {
                append(text.substring(matchIndex, matchIndex + filter.length))
            }
            currentIndex = matchIndex + filter.length
        }
    }
}
