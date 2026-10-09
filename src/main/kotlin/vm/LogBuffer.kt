package vm

import adb.data.LogcatMessage
import kotlinx.collections.immutable.ImmutableList

/**
 * Append-only log buffer that can drop entries from the head and hands out O(1)
 * immutable snapshots.
 *
 * Every slot of the backing array is written exactly once and never modified
 * afterwards; appends only touch slots past the current end and trimming only
 * moves the start offset. A snapshot covering `[start, end)` therefore stays valid
 * no matter what happens to the buffer later. Growing or compacting always
 * allocates a fresh array instead of shifting in place.
 *
 * Logs must be appended in ascending [LogcatMessage.id] order, which lets lookups
 * by id use binary search.
 */
internal class LogBuffer(initialCapacity: Int = 1024) {

    private var array = arrayOfNulls<LogcatMessage>(initialCapacity.coerceAtLeast(16))
    private var start = 0
    private var end = 0

    val size: Int get() = end - start

    operator fun get(index: Int): LogcatMessage = array[start + index]!!

    fun isEmpty(): Boolean = size == 0

    fun first(): LogcatMessage = get(0)

    fun add(log: LogcatMessage) {
        if (end == array.size) reallocate()
        array[end++] = log
    }

    /**
     * Drops the oldest entry. The slot is not cleared because older snapshots may
     * still cover it; it is released on the next reallocation.
     */
    fun removeFirst(): LogcatMessage {
        val log = get(0)
        start++
        return log
    }

    /** Index of the log with [id], or -1. */
    fun indexOfId(id: Long): Int {
        val index = lowerBound(id)
        return if (index < size && get(index).id == id) index else -1
    }

    /** Index of the first log whose id is >= [id] (== [size] if there is none). */
    fun lowerBound(id: Long): Int {
        var low = 0
        var high = size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (get(mid).id < id) low = mid + 1 else high = mid
        }
        return low
    }

    fun snapshot(): ImmutableList<LogcatMessage> = Snapshot(array, start, end)

    /**
     * Copies the live range into a new array. Doubles the capacity only when the
     * buffer is more than half full, so a buffer that is trimmed at the head as fast
     * as it grows keeps a stable footprint and the copy is amortised over at least
     * `size` appends.
     */
    private fun reallocate() {
        val live = size
        val capacity = if (live * 2 > array.size) array.size * 2 else array.size
        val newArray = arrayOfNulls<LogcatMessage>(capacity)
        System.arraycopy(array, start, newArray, 0, live)
        array = newArray
        start = 0
        end = live
    }

    private class Snapshot(
        private val array: Array<LogcatMessage?>,
        private val from: Int,
        private val to: Int
    ) : AbstractList<LogcatMessage>(), ImmutableList<LogcatMessage> {

        override val size: Int get() = to - from

        override fun get(index: Int): LogcatMessage {
            if (index < 0 || index >= size) throw IndexOutOfBoundsException("index: $index, size: $size")
            return array[from + index]!!
        }

        override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<LogcatMessage> {
            if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
                throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
            }
            return Snapshot(array, from + fromIndex, from + toIndex)
        }

        // UiState equality runs on every StateFlow update; avoid an element-wise
        // comparison when both sides are views of the same range.
        override fun equals(other: Any?): Boolean {
            if (other is Snapshot && other.array === array && other.from == from && other.to == to) return true
            return super.equals(other)
        }

        override fun hashCode(): Int = super.hashCode()
    }
}
