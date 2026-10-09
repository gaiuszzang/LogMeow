package adb

import adb.data.LogLevel
import adb.data.LogcatMessage

/**
 * Parses `adb logcat -v threadtime` output lines.
 *
 * Extracted from [AdbService] so it can be exercised directly by tests without
 * spawning an adb process.
 */
object LogcatParser {

    private val LOGCAT_REGEX = Regex(
        "(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\d+)\\s+(\\d+)\\s+([VDIWEFS])\\s+(.*?):\\s+(.*)"
    )

    /**
     * Returns the parsed message, or null when [line] is not in threadtime format
     * (e.g. buffer markers such as "--------- beginning of main").
     */
    fun parse(line: String, id: Long): LogcatMessage? {
        val match = LOGCAT_REGEX.matchEntire(line) ?: return null
        val (timestamp, pid, tid, levelChar, tag, message) = match.destructured
        return LogcatMessage(
            id = id,
            timestamp = timestamp,
            pid = pid.toIntOrNull() ?: 0,
            tid = tid.toIntOrNull() ?: 0,
            level = LogLevel.fromChar(levelChar.first()),
            tag = tag.trim(),
            message = message
        )
    }
}
