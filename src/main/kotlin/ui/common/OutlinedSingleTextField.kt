package ui.common

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

import ui.theme.LocalLogMeowTheme

@Composable
fun OutlinedSingleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    fontFamily: FontFamily? = null,
    readOnly: Boolean = false
) {
    val theme = LocalLogMeowTheme.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (label.isNotEmpty()) {
            { Text(label, fontSize = theme.fontSizeLabel) }
        } else null,
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
        singleLine = singleLine,
        isError = isError,
        readOnly = readOnly,
        textStyle = TextStyle(
            fontSize = theme.fontSizeTitle,
            lineHeight = 1.5.em,
            color = if (readOnly) theme.textSecondary else theme.textPrimary,
            fontFamily = fontFamily
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = if (readOnly) theme.textSecondary else theme.textPrimary,
            cursorColor = theme.accent,
            focusedBorderColor = if (readOnly) theme.border else theme.accent,
            unfocusedBorderColor = theme.border,
            errorBorderColor = theme.danger,
            focusedLabelColor = if (readOnly) theme.textDim else theme.accent,
            unfocusedLabelColor = theme.textDim,
            backgroundColor = if (readOnly) theme.panelBackground else theme.editFieldBackground
        )
    )
}
