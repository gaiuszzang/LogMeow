package vm

import support.generateLogs
import kotlin.test.Test
import kotlin.test.assertEquals

class LogBufferTest {

    @Test
    fun `snapshots are unaffected by later appends and trims`() {
        val buffer = LogBuffer(initialCapacity = 16)
        generateLogs(10).forEach(buffer::add)
        val snapshot = buffer.snapshot()

        repeat(8) { buffer.removeFirst() }
        generateLogs(100, startId = 10).forEach(buffer::add) // forces reallocation

        assertEquals((0L until 10L).toList(), snapshot.map { it.id })
        assertEquals((8L until 110L).toList(), buffer.snapshot().map { it.id })
    }

    @Test
    fun `id lookups use the retained window`() {
        val buffer = LogBuffer()
        generateLogs(20).forEach(buffer::add)
        repeat(5) { buffer.removeFirst() }

        assertEquals(-1, buffer.indexOfId(3))
        assertEquals(0, buffer.indexOfId(5))
        assertEquals(14, buffer.indexOfId(19))
        assertEquals(0, buffer.lowerBound(0))
        assertEquals(15, buffer.lowerBound(100))
    }

    @Test
    fun `snapshot sublists stay within their range`() {
        val buffer = LogBuffer()
        generateLogs(10).forEach(buffer::add)

        val sub = buffer.snapshot().subList(2, 5)

        assertEquals(listOf(2L, 3L, 4L), sub.map { it.id })
    }
}
