package ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class Direction { Vertical, Horizontal }

@Composable
fun BoxScope.LazyListScrollBar(
    state: LazyListState,
    direction: Direction,
    thickness: Dp = 4.dp,
    minLength: Dp = 16.dp,
    color: Color = Color.Gray,
    backgroundColor: Color = Color.Transparent,
    isAlwaysDisplay: Boolean = false,
    bookmarkedIndices: List<Int> = emptyList(),
    totalItemCount: Int = 0,
    onBookmarkClick: ((Int) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var isDraggingScrollbar by remember { mutableStateOf(false) }
    var dragStartScrollPosition by remember { mutableStateOf(0f) }
    var dragStartAvgItemSize by remember { mutableStateOf(0f) }
    var dragStartTotalScrollableRange by remember { mutableStateOf(0f) }
    var dragStartVariableZone by remember { mutableStateOf(0f) }
    var dragStartTotalItems by remember { mutableStateOf(0) }
    var accumulatedDrag by remember { mutableStateOf(0f) }

    val alpha by animateFloatAsState(
        targetValue = if (state.isScrollInProgress || isDraggingScrollbar || isAlwaysDisplay) 1f else 0f,
        animationSpec = tween(400, delayMillis = if (state.isScrollInProgress || isDraggingScrollbar || isAlwaysDisplay) 0 else 700),
        label = "ScrollBarAlpha"
    )

    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    if (layoutInfo.totalItemsCount == 0 || visibleItems.isEmpty()) return

    with(LocalDensity.current) {
        val visibleHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
        val minLengthPx = minLength.toPx()
        val thicknessPx = thickness.toPx()

        // Better average: sum of visible sizes / visible count
        val averageItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
        val totalItemsCount = layoutInfo.totalItemsCount
        val totalContentHeightPx = averageItemSize * totalItemsCount

        // Thumb height proportional to visible fraction
        val scrollbarHeightPx = (visibleHeightPx * (visibleHeightPx / totalContentHeightPx))
            .coerceIn(minLengthPx..visibleHeightPx)
        val variableZone = (visibleHeightPx - scrollbarHeightPx).coerceAtLeast(1f) // avoid /0

        // Estimate how many pixels we've scrolled from top
        val scrolledPx = state.firstVisibleItemIndex * averageItemSize + state.firstVisibleItemScrollOffset
        val totalScrollableRange = (totalContentHeightPx - visibleHeightPx).coerceAtLeast(1f)

        // normalized progress and thumb offset
        val scrollProgress = (scrolledPx / totalScrollableRange).coerceIn(0f, 1f)
        val scrollOffsetPx = scrollProgress * variableZone

        val isVertical = direction == Direction.Vertical
        val modifier = if (isVertical) {
            Modifier
                .fillMaxHeight()
                .width(thickness)
                .align(Alignment.CenterEnd)
        } else {
            Modifier
                .fillMaxWidth()
                .height(thickness)
                .align(Alignment.BottomCenter)
        }

        Box(
            modifier = modifier
                .pointerInput(state) {
                    detectTapGestures { offset ->
                        coroutineScope.launch {
                            // Get current layout info
                            val currentLayoutInfo = state.layoutInfo
                            val currentVisibleItems = currentLayoutInfo.visibleItemsInfo
                            if (currentVisibleItems.isEmpty()) return@launch

                            val currentVisibleHeightPx = (currentLayoutInfo.viewportEndOffset - currentLayoutInfo.viewportStartOffset).toFloat()
                            val currentAvgItemSize = currentVisibleItems.sumOf { it.size }.toFloat() / currentVisibleItems.size
                            val currentTotalItems = currentLayoutInfo.totalItemsCount
                            val currentTotalContentHeight = currentAvgItemSize * currentTotalItems
                            val currentTotalScrollableRange = (currentTotalContentHeight - currentVisibleHeightPx).coerceAtLeast(1f)

                            // Calculate clicked position ratio
                            val clickedPosition = if (isVertical) offset.y else offset.x
                            val totalTrackLength = if (isVertical) size.height else size.width
                            val scrollRatio = (clickedPosition / totalTrackLength).coerceIn(0f, 1f)

                            // Calculate target scroll position
                            val targetScrollPx = scrollRatio * currentTotalScrollableRange

                            // Calculate target item index and offset
                            val targetItemIndex = (targetScrollPx / currentAvgItemSize).toInt()
                                .coerceIn(0, currentTotalItems - 1)
                            val targetItemOffset = (targetScrollPx % currentAvgItemSize).toInt()
                                .coerceAtLeast(0)

                            state.scrollToItem(targetItemIndex, targetItemOffset)
                        }
                    }
                }
                .pointerInput(state) {
                    val onStart: (Offset) -> Unit = {
                        isDraggingScrollbar = true
                        accumulatedDrag = 0f

                        // Capture all values at drag start and keep them fixed
                        val currentLayoutInfo = state.layoutInfo
                        val currentVisibleItems = currentLayoutInfo.visibleItemsInfo

                        if (currentVisibleItems.isNotEmpty()) {
                            dragStartAvgItemSize = currentVisibleItems.sumOf { it.size }.toFloat() / currentVisibleItems.size
                            dragStartScrollPosition = state.firstVisibleItemIndex * dragStartAvgItemSize + state.firstVisibleItemScrollOffset
                            dragStartTotalItems = currentLayoutInfo.totalItemsCount

                            val currentVisibleHeightPx = (currentLayoutInfo.viewportEndOffset - currentLayoutInfo.viewportStartOffset).toFloat()
                            val currentTotalContentHeight = dragStartAvgItemSize * dragStartTotalItems
                            val currentScrollbarHeight = (currentVisibleHeightPx * (currentVisibleHeightPx / currentTotalContentHeight))
                                .coerceIn(minLengthPx..currentVisibleHeightPx)

                            dragStartVariableZone = (currentVisibleHeightPx - currentScrollbarHeight).coerceAtLeast(1f)
                            dragStartTotalScrollableRange = (currentTotalContentHeight - currentVisibleHeightPx).coerceAtLeast(1f)
                        }
                    }

                    val onDrag: (PointerInputChange, Float) -> Unit = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                        coroutineScope.launch {
                            if (dragStartAvgItemSize == 0f || dragStartVariableZone == 0f) return@launch

                            // Use fixed values from drag start
                            val dragScale = dragStartTotalScrollableRange / dragStartVariableZone
                            val targetScrollPx = (dragStartScrollPosition + accumulatedDrag * dragScale)
                                .coerceIn(0f, dragStartTotalScrollableRange)

                            // Calculate target item index and offset
                            val targetItemIndex = (targetScrollPx / dragStartAvgItemSize).toInt()
                                .coerceIn(0, dragStartTotalItems - 1)
                            val targetItemOffset = (targetScrollPx % dragStartAvgItemSize).toInt()
                                .coerceAtLeast(0)

                            state.scrollToItem(targetItemIndex, targetItemOffset)
                        }
                    }

                    val onEnd = {
                        isDraggingScrollbar = false
                        accumulatedDrag = 0f
                    }

                    if (isVertical) {
                        detectVerticalDragGestures(
                            onDragStart = onStart,
                            onVerticalDrag = onDrag,
                            onDragEnd = onEnd,
                            onDragCancel = onEnd
                        )
                    } else {
                        detectHorizontalDragGestures(
                            onDragStart = onStart,
                            onHorizontalDrag = onDrag,
                            onDragEnd = onEnd,
                            onDragCancel = onEnd
                        )
                    }
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val bookmarkMarkerColor = Color(0xFFE6A817) // Golden/amber color for bookmarks
                val markerThickness = 2.dp.toPx()
                val effectiveTotalItems = if (totalItemCount > 0) totalItemCount else layoutInfo.totalItemsCount

                if (isVertical) {
                    // Draw background track
                    drawRoundRect(
                        topLeft = Offset(0f, 0f),
                        size = Size(thicknessPx, size.height),
                        cornerRadius = CornerRadius(thicknessPx / 2f),
                        color = backgroundColor,
                        alpha = alpha
                    )

                    // Draw scrollbar thumb
                    drawRoundRect(
                        topLeft = Offset(0f, scrollOffsetPx),
                        size = Size(thicknessPx, scrollbarHeightPx),
                        cornerRadius = CornerRadius(thicknessPx / 2f),
                        color = color,
                        alpha = alpha
                    )

                    // Draw bookmark markers (on top of thumb, semi-transparent)
                    if (effectiveTotalItems > 0) {
                        bookmarkedIndices.forEach { index ->
                            val markerY = (index.toFloat() / effectiveTotalItems) * size.height
                            drawRect(
                                color = bookmarkMarkerColor,
                                topLeft = Offset(0f, markerY),
                                size = Size(thicknessPx, markerThickness),
                                alpha = alpha * 0.7f
                            )
                        }
                    }
                } else {
                    // Draw background track
                    drawRoundRect(
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, thicknessPx),
                        cornerRadius = CornerRadius(thicknessPx / 2f),
                        color = backgroundColor,
                        alpha = alpha
                    )

                    // Draw scrollbar thumb
                    drawRoundRect(
                        topLeft = Offset(scrollOffsetPx, 0f),
                        size = Size(scrollbarHeightPx, thicknessPx),
                        cornerRadius = CornerRadius(thicknessPx / 2f),
                        color = color,
                        alpha = alpha
                    )

                    // Draw bookmark markers (on top of thumb, semi-transparent)
                    if (effectiveTotalItems > 0) {
                        bookmarkedIndices.forEach { index ->
                            val markerX = (index.toFloat() / effectiveTotalItems) * size.width
                            drawRect(
                                color = bookmarkMarkerColor,
                                topLeft = Offset(markerX, 0f),
                                size = Size(markerThickness, thicknessPx),
                                alpha = alpha * 0.7f
                            )
                        }
                    }
                }
            }
        }
    }
}
