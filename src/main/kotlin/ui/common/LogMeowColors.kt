package ui.common

import androidx.compose.ui.graphics.Color

object LogMeowColors {
    // Backgrounds
    val DarkBackground = Color(0xFF2B2B2B)
    val PanelBackground = Color(0xFF1E1E1E)
    val BodyBackground = Color(0xFF181818)
    val EditFieldBackground = Color(0xFF252525)
    val TextSelectionBackground = Color(0xFF505050)
    val TextSelectionHoverBackground = Color(0xFF3A3A3A)

    // Selection
    val SelectedFocused = Color(0xFF264F78)
    val SelectedUnfocused = Color(0xFF4A4A4A)

    // Buttons
    val ButtonBackground = Color(0xFF4A4A4A)
    val DisabledBackground = Color(0xFF3A3A3A)
    val DeleteButtonBackground = Color(0xFF4A2020)

    // Accent
    val Accent = Color(0xFF9090E0)
    val AccentBackground = Color(0xFF3A3A5C)

    // Borders / Dividers
    val Divider = Color(0xFF3A3A3A)

    // Text
    val HeaderKey = Color(0xFF61AFEF)
    val HeaderValue = Color(0xFFD4D4D4)
    val Dim = Color(0xFF808080)

    // Status
    val ServerRunning = Color(0xFF37943D)
    val Success = Color(0xFF98C379)
    val Danger = Color(0xFFE06C75)
    val Warning = Color(0xFFE5C07B)

    // Mock badge
    val MockBadgeBackground = Color(0xFF3D3D20)

    // HTTP Method colors
    val MethodGet = Color(0xFF61AFEF)
    val MethodPost = Color(0xFF98C379)
    val MethodPut = Color(0xFFE5C07B)
    val MethodDelete = Color(0xFFE06C75)
    val MethodPatch = Color(0xFFC678DD)
    val MethodHead = Color(0xFF56B6C2)
    val MethodOptions = Color(0xFFD19A66)

    fun methodColor(method: String): Color = when (method.uppercase()) {
        "GET" -> MethodGet
        "POST" -> MethodPost
        "PUT" -> MethodPut
        "DELETE" -> MethodDelete
        "PATCH" -> MethodPatch
        "HEAD" -> MethodHead
        "OPTIONS" -> MethodOptions
        else -> Color.LightGray
    }

    fun statusCodeColor(statusCode: Int?): Color = when {
        statusCode == null -> Color.Gray
        statusCode in 200..299 -> Success
        statusCode in 300..399 -> Warning
        statusCode in 400..499 -> Danger
        statusCode >= 500 -> Color.Red
        else -> Color.Gray
    }
}
