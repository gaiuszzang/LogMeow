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
import androidx.compose.runtime.derivedStateOf
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
import ui.theme.LocalLogMeowTheme

enum class Direction { Vertical, Horizontal }

@Composable
fun BoxScope.LazyListScrollBar(
    state: LazyListState,
    direction: Direction,
    thickness: Dp = 4.dp,
    minLength: Dp = 16.dp,
    color: Color = LocalLogMeowTheme.current.scrollbarThumb,
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

    // Only this flag is read during composition. Thumb geometry is read in the draw
    // phase below, so scrolling just redraws the bar instead of recomposing it.
    val isEmpty by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            layoutInfo.totalItemsCount == 0 || layoutInfo.visibleItemsInfo.isEmpty()
        }
    }
    if (isEmpty) return

    with(LocalDensity.current) {
        val minLengthPx = minLength.toPx()
        val thicknessPx = thickness.toPx()

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
            val bookmarkMarkerColor = LocalLogMeowTheme.current.bookmarkMarker
            val scrollbarCornerRadius = with(this@with) {
                LocalLogMeowTheme.current.cornerRadiusSmall.toPx()
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                val thumb = state.thumbGeometry(minLengthPx) ?: return@Canvas
                val markerThickness = 2.dp.toPx()
                val effectiveTotalItems = if (totalItemCount > 0) totalItemCount else state.layoutInfo.totalItemsCount

                if (isVertical) {
                    // Draw background track
                    drawRoundRect(
                        topLeft = Offset(0f, 0f),
                        size = Size(thicknessPx, size.height),
                        cornerRadius = CornerRadius(scrollbarCornerRadius),
                        color = backgroundColor,
                        alpha = alpha
                    )

                    // Draw scrollbar thumb
                    drawRoundRect(
                        topLeft = Offset(0f, thumb.offsetPx),
                        size = Size(thicknessPx, thumb.lengthPx),
                        cornerRadius = CornerRadius(scrollbarCornerRadius),
                        color = color,
                        alpha = alpha
                    )

                    // Draw bookmark markers (on top of thumb, semi-transparent)
                    if (effectiveTotalItems > 0) {
                        forEachMarkerPosition(bookmarkedIndices, effectiveTotalItems, size.height, markerThickness) { markerY ->
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
                        cornerRadius = CornerRadius(scrollbarCornerRadius),
                        color = backgroundColor,
                        alpha = alpha
                    )

                    // Draw scrollbar thumb
                    drawRoundRect(
                        topLeft = Offset(thumb.offsetPx, 0f),
                        size = Size(thumb.lengthPx, thicknessPx),
                        cornerRadius = CornerRadius(scrollbarCornerRadius),
                        color = color,
                        alpha = alpha
                    )

                    // Draw bookmark markers (on top of thumb, semi-transparent)
                    if (effectiveTotalItems > 0) {
                        forEachMarkerPosition(bookmarkedIndices, effectiveTotalItems, size.width, markerThickness) { markerX ->
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

private class ThumbGeometry(val offsetPx: Float, val lengthPx: Float)

/** Thumb position and length along the scroll axis, or null when there is nothing to show. */
private fun LazyListState.thumbGeometry(minLengthPx: Float): ThumbGeometry? {
    val info = layoutInfo
    val visibleItems = info.visibleItemsInfo
    if (info.totalItemsCount == 0 || visibleItems.isEmpty()) return null

    val visibleHeightPx = (info.viewportEndOffset - info.viewportStartOffset).toFloat()

    // Better average: sum of visible sizes / visible count
    val averageItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
    val totalContentHeightPx = averageItemSize * info.totalItemsCount

    // Thumb height proportional to visible fraction
    val scrollbarHeightPx = (visibleHeightPx * (visibleHeightPx / totalContentHeightPx))
        .coerceIn(minLengthPx..visibleHeightPx)
    val variableZone = (visibleHeightPx - scrollbarHeightPx).coerceAtLeast(1f) // avoid /0

    // Estimate how many pixels we've scrolled from top
    val scrolledPx = firstVisibleItemIndex * averageItemSize + firstVisibleItemScrollOffset
    val totalScrollableRange = (totalContentHeightPx - visibleHeightPx).coerceAtLeast(1f)

    // normalized progress and thumb offset
    val scrollProgress = (scrolledPx / totalScrollableRange).coerceIn(0f, 1f)
    return ThumbGeometry(offsetPx = scrollProgress * variableZone, lengthPx = scrollbarHeightPx)
}

/**
 * Calls [draw] with the track position of each marker, skipping markers that would
 * overlap the previous one. Thousands of bookmarks collapse to at most one marker
 * per [markerThickness] of track.
 */
private inline fun forEachMarkerPosition(
    indices: List<Int>,
    totalItems: Int,
    trackLength: Float,
    markerThickness: Float,
    draw: (Float) -> Unit
) {
    var lastPosition = Float.NEGATIVE_INFINITY
    for (index in indices) {
        val position = (index.toFloat() / totalItems) * trackLength
        if (position - lastPosition < markerThickness) continue
        draw(position)
        lastPosition = position
    }
}
