package ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.awt.Cursor

@Composable
fun DragHandle(
    isVertical: Boolean,
    onDrag: (Float) -> Unit
) {
    val cursor = if (isVertical) Cursor(Cursor.E_RESIZE_CURSOR) else Cursor(Cursor.N_RESIZE_CURSOR)
    Box(
        modifier = Modifier
            .then(
                if (isVertical) Modifier.width(6.dp).fillMaxHeight()
                else Modifier.height(6.dp).fillMaxWidth()
            )
            .pointerHoverIcon(PointerIcon(cursor))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = if (isVertical) dragAmount.x else dragAmount.y
                    onDrag(delta / density)
                }
            },
        contentAlignment = Alignment.Center
    ) {}
}
