package ui

import adb.data.LogcatMessage
import adb.data.LogLevel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.sp
import ui.common.LazyListScrollBar
import vm.UiState

@Composable
fun LogCatView(
    uiState: UiState,
    onLogClick: (id: Long, isShift: Boolean, isAlt: Boolean) -> Unit,
    onDragSelect: (id: Long) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val filteredLogs = uiState.filteredLogs
    var isDragging by remember { mutableStateOf(false) }
    var dragMouseY by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }

    // Auto-scroll to the bottom when a new log is added
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.scrollToItem(filteredLogs.size - 1)
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

    if (filteredLogs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Logs will appear here...", fontSize = 12.sp)
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp)
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                //contentPadding = PaddingValues(6.dp)
            ) {
                items(filteredLogs.size) { index ->
                    val log = filteredLogs[index]
                    LogRow(
                        modifier = Modifier.fillMaxWidth(),
                        log = log,
                        filterTag = uiState.filterTag,
                        filterMessage = uiState.filterMessage,
                        onLogClick = { isShift, isAlt ->
                            onLogClick(log.id, isShift, isAlt)
                        }
                    )
                }
            }
            LazyListScrollBar(
                state = listState,
                direction = ui.common.Direction.Vertical,
                thickness = 12.dp,
                color = Color.Gray,
                backgroundColor = Color.DarkGray,
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
    onLogClick: (isShift: Boolean, isAlt: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }

    val logColor = when (log.level) {
        LogLevel.VERBOSE -> Color.Gray
        LogLevel.DEBUG -> Color(0xFF3D95C3) // Blue
        LogLevel.INFO -> Color(0xFF4A9B50) // Green
        LogLevel.WARN -> Color(0xFFC88235) // Orange
        LogLevel.ERROR, LogLevel.FATAL -> Color.Red
    }

    val rowBackgroundColor = when {
        log.isSelected && log.isBookmarked -> Color(0xFF5C4A1A) // Selected + bookmarked: darker gold tint
        log.isSelected -> Color(0xFF4A4A4A)
        log.isBookmarked && isHovered -> Color(0xFF4A3D15) // Bookmarked + hovered
        log.isBookmarked -> Color(0xFF3D3312) // Bookmarked: visible gold/brown tint
        isHovered -> Color(0xFF3A3A3A)
        else -> MaterialTheme.colors.surface
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
                                val isShift = event.keyboardModifiers.isShiftPressed
                                val isAlt = event.keyboardModifiers.isMetaPressed || event.keyboardModifiers.isCtrlPressed
                                onLogClick(isShift, isAlt)
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
        // Timestamp - fixed width 180dp
        Text(
            text = log.timestamp,
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(180.dp)
        )

        // Level - fixed width 30dp
        Text(
            text = log.level.name.first().toString(),
            color = logColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(30.dp)
        )

        // PID - fixed width 60dp
        Text(
            text = log.pid.toString(),
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(50.dp)
        )

        // TID - fixed width 60dp
        Text(
            text = log.tid.toString(),
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(50.dp)
        )

        // Tag - fixed width 220dp with highlighting
        Box(modifier = Modifier.width(220.dp)) {
            if (!filterTag.isNullOrBlank()) {
                Text(
                    text = buildAnnotatedString {
                        var currentIndex = 0
                        val tag = log.tag
                        val lowerTag = tag.lowercase()
                        val lowerFilter = filterTag.lowercase()

                        while (currentIndex < tag.length) {
                            val matchIndex = lowerTag.indexOf(lowerFilter, currentIndex)
                            if (matchIndex == -1) {
                                withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))) {
                                    append(tag.substring(currentIndex))
                                }
                                break
                            } else {
                                if (matchIndex > currentIndex) {
                                    withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))) {
                                        append(tag.substring(currentIndex, matchIndex))
                                    }
                                }
                                withStyle(style = SpanStyle(
                                    color = MaterialTheme.colors.surface,
                                    background = Color.Yellow
                                )) {
                                    append(tag.substring(matchIndex, matchIndex + filterTag.length))
                                }
                                currentIndex = matchIndex + filterTag.length
                            }
                        }
                    },
                    fontSize = 12.sp,
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    minLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Message - fills remaining space with highlighting
        if (!filterMessage.isNullOrBlank()) {
            Text(
                text = buildAnnotatedString {
                    var currentIndex = 0
                    val message = log.message
                    val lowerMessage = message.lowercase()
                    val lowerFilter = filterMessage.lowercase()

                    while (currentIndex < message.length) {
                        val matchIndex = lowerMessage.indexOf(lowerFilter, currentIndex)
                        if (matchIndex == -1) {
                            withStyle(style = SpanStyle(color = logColor)) {
                                append(message.substring(currentIndex))
                            }
                            break
                        } else {
                            if (matchIndex > currentIndex) {
                                withStyle(style = SpanStyle(color = logColor)) {
                                    append(message.substring(currentIndex, matchIndex))
                                }
                            }
                            withStyle(style = SpanStyle(
                                color = MaterialTheme.colors.surface,
                                background = Color.Yellow
                            )) {
                                append(message.substring(matchIndex, matchIndex + filterMessage.length))
                            }
                            currentIndex = matchIndex + filterMessage.length
                        }
                    }
                },
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = log.message,
                color = logColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
