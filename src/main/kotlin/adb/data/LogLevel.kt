package adb.data

enum class LogLevel(val char: Char) {
    VERBOSE('V'),
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E'),
    FATAL('F'),
    ;

    companion object {
        fun fromChar(char: Char): LogLevel {
            return entries.find { it.char == char } ?: VERBOSE
        }
    }
}
