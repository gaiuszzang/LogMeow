package ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Stable
sealed interface LogMeowTheme {
    // Backgrounds
    val darkBackground: Color
    val panelBackground: Color
    val bodyBackground: Color
    val editFieldBackground: Color
    val textSelectionHoverBackground: Color
    val materialBackground: Color

    // Selection
    val selectedFocused: Color
    val selectedUnfocused: Color

    // Buttons
    val buttonBackground: Color
    val disabledBackground: Color
    val disabledContentColor: Color
    val deleteButtonBackground: Color

    // Accent
    val accent: Color
    val accentBackground: Color

    // Borders / Dividers
    val divider: Color
    val border: Color

    // Text
    val textPrimary: Color
    val textSecondary: Color
    val textDim: Color
    val headerKey: Color
    val headerValue: Color

    // Status
    val serverRunning: Color
    val success: Color
    val danger: Color
    val warning: Color

    // Mock badge
    val mockBadgeBackground: Color

    // HTTP Method
    val methodGet: Color
    val methodPost: Color
    val methodPut: Color
    val methodDelete: Color
    val methodPatch: Color
    val methodHead: Color
    val methodOptions: Color

    // Log Level
    val logVerbose: Color
    val logDebug: Color
    val logInfo: Color
    val logWarn: Color
    val logError: Color

    // Bookmark
    val bookmarkMarker: Color
    val bookmarkBackground: Color
    val bookmarkHoverBackground: Color
    val selectedBookmarkFocused: Color
    val selectedBookmarkUnfocused: Color

    // Icons
    val iconDefault: Color
    val iconChevron: Color
    val playIcon: Color
    val stopIcon: Color

    // Highlight
    val highlightBackground: Color

    // Scrollbar
    val scrollbarThumb: Color
    val scrollbarTrack: Color

    // Font Sizes
    val fontSizeBadge: TextUnit      // 뱃지, 아주 작은 인디케이터
    val fontSizeLabel: TextUnit      // 라벨, 상태바, 메서드 태그
    val fontSizeBody: TextUnit       // 본문 텍스트, 리스트 아이템
    val fontSizeTitle: TextUnit      // 섹션 헤더, 버튼, 입력 필드
    val fontSizeHeader: TextUnit     // 다이얼로그 타이틀

    // Corner Radius
    val cornerRadiusSmall: Dp        // 뱃지, 태그
    val cornerRadius: Dp             // 버튼, 입력 필드, 로그창

    fun methodColor(method: String): Color = when (method.uppercase()) {
        "GET" -> methodGet
        "POST" -> methodPost
        "PUT" -> methodPut
        "DELETE" -> methodDelete
        "PATCH" -> methodPatch
        "HEAD" -> methodHead
        "OPTIONS" -> methodOptions
        else -> textSecondary
    }

    fun statusCodeColor(statusCode: Int?): Color = when {
        statusCode == null -> textDim
        statusCode in 200..299 -> success
        statusCode in 300..399 -> warning
        statusCode in 400..499 -> danger
        statusCode >= 500 -> danger
        else -> textDim
    }
}

data object IslandsDarkTheme : LogMeowTheme {
    // Backgrounds
    override val darkBackground = Color(0xFF191A1C)
    override val panelBackground = Color(0xFF1E1F22)
    override val bodyBackground = Color(0xFF1E1F22)
    override val editFieldBackground = Color(0xFF1E1F22)
    override val textSelectionHoverBackground = Color(0xFF2B2D30)
    override val materialBackground = Color(0xFF2B2D30)

    // Selection
    override val selectedFocused = Color(0xFF2E4362)
    override val selectedUnfocused = Color(0xFF2B2D30)

    // Buttons
    override val buttonBackground = Color(0xFF3C3F41)
    override val disabledBackground = Color(0xFF2B2D30)
    override val disabledContentColor = Color(0xFF6F737A)
    override val deleteButtonBackground = Color(0xFF5C3030)

    // Accent
    override val accent = Color(0xFF548AF7)
    override val accentBackground = Color(0xFF2E436E)

    // Borders / Dividers
    override val divider = Color(0xFF2B2D30)
    override val border = Color(0xFF393B40)

    // Text
    override val textPrimary = Color(0xFFDFE1E5)
    override val textSecondary = Color(0xFFAEB1B8)
    override val textDim = Color(0xFF6F737A)
    override val headerKey = Color(0xFF5394EC)
    override val headerValue = Color(0xFFBCBEC4)

    // Status
    override val serverRunning = Color(0xFF59A869)
    override val success = Color(0xFF59A869)
    override val danger = Color(0xFFFF6B68)
    override val warning = Color(0xFFE8BF6A)

    // Mock badge
    override val mockBadgeBackground = Color(0xFF3D3D20)

    // HTTP Method
    override val methodGet = Color(0xFF5394EC)
    override val methodPost = Color(0xFF59A869)
    override val methodPut = Color(0xFFE8BF6A)
    override val methodDelete = Color(0xFFFF6B68)
    override val methodPatch = Color(0xFF9876AA)
    override val methodHead = Color(0xFF299999)
    override val methodOptions = Color(0xFFCC7832)

    // Log Level
    override val logVerbose = Color(0xFF8C8C8C)
    override val logDebug = Color(0xFF5394EC)
    override val logInfo = Color(0xFF59A869)
    override val logWarn = Color(0xFFBE9117)
    override val logError = Color(0xFFFF6B68)

    // Bookmark
    override val bookmarkMarker = Color(0xFFE8BF6A)
    override val bookmarkBackground = Color(0xFF3D3312)
    override val bookmarkHoverBackground = Color(0xFF4A3D15)
    override val selectedBookmarkFocused = Color(0xFF4A3D2A)
    override val selectedBookmarkUnfocused = Color(0xFF5C4A1A)

    // Icons
    override val iconDefault = Color(0xFFAFB1B3)
    override val iconChevron = Color(0xFFAFB1B3)
    override val playIcon = Color(0xFF59A869)
    override val stopIcon = Color(0xFFFF6B68)

    // Highlight
    override val highlightBackground = Color(0xFF32593D)

    // Scrollbar
    override val scrollbarThumb = Color(0xFF3C3F41)
    override val scrollbarTrack = Color(0xFF1E1F22)

    // Font Sizes
    override val fontSizeBadge = 10.sp
    override val fontSizeLabel = 11.sp
    override val fontSizeBody = 12.sp
    override val fontSizeTitle = 13.sp
    override val fontSizeHeader = 14.sp

    // Corner Radius
    override val cornerRadiusSmall = 2.dp
    override val cornerRadius = 4.dp
}

data object DarkModernTheme : LogMeowTheme {
    // Backgrounds
    override val darkBackground = Color(0xFF181818)
    override val panelBackground = Color(0xFF1F1F1F)
    override val bodyBackground = Color(0xFF181818)
    override val editFieldBackground = Color(0xFF2D2D2D)
    override val textSelectionHoverBackground = Color(0xFF232323)
    override val materialBackground = Color(0xFF2D2D2D)

    // Selection
    override val selectedFocused = Color(0xFF264F78)
    override val selectedUnfocused = Color(0xFF2D2D2D)

    // Buttons
    override val buttonBackground = Color(0xFF313131)
    override val disabledBackground = Color(0xFF2D2D2D)
    override val disabledContentColor = Color(0xFF6B6B6B)
    override val deleteButtonBackground = Color(0xFF5C2020)

    // Accent
    override val accent = Color(0xFF007ACC)
    override val accentBackground = Color(0xFF094771)

    // Borders / Dividers
    override val divider = Color(0xFF2D2D2D)
    override val border = Color(0xFF3C3C3C)

    // Text
    override val textPrimary = Color(0xFFD4D4D4)
    override val textSecondary = Color(0xFFCCCCCC)
    override val textDim = Color(0xFF858585)
    override val headerKey = Color(0xFF9CDCFE)
    override val headerValue = Color(0xFFCE9178)

    // Status
    override val serverRunning = Color(0xFF6A9955)
    override val success = Color(0xFF6A9955)
    override val danger = Color(0xFFF44747)
    override val warning = Color(0xFFCCA700)

    // Mock badge
    override val mockBadgeBackground = Color(0xFF3D3D20)

    // HTTP Method
    override val methodGet = Color(0xFF4FC1FF)
    override val methodPost = Color(0xFF6A9955)
    override val methodPut = Color(0xFFDCDCAA)
    override val methodDelete = Color(0xFFF44747)
    override val methodPatch = Color(0xFFC586C0)
    override val methodHead = Color(0xFF4EC9B0)
    override val methodOptions = Color(0xFFCE9178)

    // Log Level
    override val logVerbose = Color(0xFF858585)
    override val logDebug = Color(0xFF569CD6)
    override val logInfo = Color(0xFF6A9955)
    override val logWarn = Color(0xFFCCA700)
    override val logError = Color(0xFFF44747)

    // Bookmark
    override val bookmarkMarker = Color(0xFFDCDCAA)
    override val bookmarkBackground = Color(0xFF3D3D20)
    override val bookmarkHoverBackground = Color(0xFF4A4A25)
    override val selectedBookmarkFocused = Color(0xFF4A3D2A)
    override val selectedBookmarkUnfocused = Color(0xFF5C4A1A)

    // Icons
    override val iconDefault = Color(0xFFC5C5C5)
    override val iconChevron = Color(0xFFC5C5C5)
    override val playIcon = Color(0xFF89D185)
    override val stopIcon = Color(0xFFF44747)

    // Highlight
    override val highlightBackground = Color(0xFF623916)

    // Scrollbar
    override val scrollbarThumb = Color(0xFF3A3A3A)
    override val scrollbarTrack = Color(0xFF181818)

    // Font Sizes
    override val fontSizeBadge = 10.sp
    override val fontSizeLabel = 11.sp
    override val fontSizeBody = 12.sp
    override val fontSizeTitle = 13.sp
    override val fontSizeHeader = 14.sp

    // Corner Radius
    override val cornerRadiusSmall = 0.dp
    override val cornerRadius = 2.dp
}

val LocalLogMeowTheme = staticCompositionLocalOf<LogMeowTheme> { IslandsDarkTheme }

val availableThemes: List<Pair<String, LogMeowTheme>> = listOf(
    "IslandsDark" to IslandsDarkTheme,
    "DarkModern" to DarkModernTheme
)

fun themeByName(name: String): LogMeowTheme =
    availableThemes.firstOrNull { it.first == name }?.second ?: IslandsDarkTheme
