package adb

import adb.data.LogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogcatParserTest {

    @Test
    fun `parses a threadtime line`() {
        val parsed = LogcatParser.parse(
            "01-02 03:04:05.678  1234  5678 D MyTag: hello world",
            id = 7
        )

        requireNotNull(parsed)
        assertEquals(7, parsed.id)
        assertEquals("01-02 03:04:05.678", parsed.timestamp)
        assertEquals(1234, parsed.pid)
        assertEquals(5678, parsed.tid)
        assertEquals(LogLevel.DEBUG, parsed.level)
        assertEquals("MyTag", parsed.tag)
        assertEquals("hello world", parsed.message)
    }

    @Test
    fun `keeps colons inside the message`() {
        val parsed = LogcatParser.parse(
            "01-02 03:04:05.678  1234  5678 I Net: GET https://example.com: 200",
            id = 0
        )

        requireNotNull(parsed)
        assertEquals("Net", parsed.tag)
        assertEquals("GET https://example.com: 200", parsed.message)
    }

    @Test
    fun `trims whitespace around the tag`() {
        val parsed = LogcatParser.parse(
            "01-02 03:04:05.678  1234  5678 W Spaced   : body",
            id = 0
        )

        requireNotNull(parsed)
        assertEquals("Spaced", parsed.tag)
        assertEquals("body", parsed.message)
    }

    @Test
    fun `maps every level character`() {
        val expected = mapOf(
            'V' to LogLevel.VERBOSE,
            'D' to LogLevel.DEBUG,
            'I' to LogLevel.INFO,
            'W' to LogLevel.WARN,
            'E' to LogLevel.ERROR,
            'F' to LogLevel.FATAL
        )

        expected.forEach { (char, level) ->
            val parsed = LogcatParser.parse("01-02 03:04:05.678 1 2 $char T: m", id = 0)
            requireNotNull(parsed) { "level $char should parse" }
            assertEquals(level, parsed.level, "level $char")
        }
    }

    @Test
    fun `returns null for buffer markers and malformed lines`() {
        assertNull(LogcatParser.parse("--------- beginning of main", id = 0))
        assertNull(LogcatParser.parse("", id = 0))
        assertNull(LogcatParser.parse("not a logcat line at all", id = 0))
    }

    @Test
    fun `returns null for a message with no tag separator`() {
        assertNull(LogcatParser.parse("01-02 03:04:05.678  1234  5678 D no-separator-here", id = 0))
    }
}
