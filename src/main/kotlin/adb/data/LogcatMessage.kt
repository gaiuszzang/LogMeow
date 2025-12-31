package adb.data

data class LogcatMessage(
    val id: Long,
    val timestamp: String,
    val pid: Int,
    val tid: Int,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val isSelected: Boolean = false,
    val isBookmarked: Boolean = false
)
