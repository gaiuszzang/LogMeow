package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.theme.LocalLogMeowTheme

@Composable
fun RowItemDivider() {
    Spacer(modifier = Modifier
        .padding(horizontal = 4.dp)
        .height(24.dp)
        .width(1.dp)
        .background(LocalLogMeowTheme.current.divider)
    )
}
