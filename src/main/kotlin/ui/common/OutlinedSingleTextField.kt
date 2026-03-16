package ui.common

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (label.isNotEmpty()) {
            { Text(label, fontSize = 11.sp) }
        } else null,
        modifier = modifier.defaultMinSize(minHeight = 40.dp),
        singleLine = singleLine,
        isError = isError,
        readOnly = readOnly,
        textStyle = TextStyle(
            fontSize = 13.sp,
            lineHeight = 1.5.em,
            color = if (readOnly) Color.LightGray else Color.White,
            fontFamily = fontFamily
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = if (readOnly) Color.LightGray else Color.White,
            cursorColor = LogMeowColors.Accent,
            focusedBorderColor = if (readOnly) Color.DarkGray else LogMeowColors.Accent,
            unfocusedBorderColor = Color.DarkGray,
            errorBorderColor = LogMeowColors.Danger,
            focusedLabelColor = if (readOnly) Color.Gray else LogMeowColors.Accent,
            unfocusedLabelColor = Color.Gray,
            backgroundColor = if (readOnly) LogMeowColors.PanelBackground else LogMeowColors.EditFieldBackground
        )
    )
}
