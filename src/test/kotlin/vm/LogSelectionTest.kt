package vm

import adb.data.LogLevel
import kotlinx.coroutines.test.runTest
import support.generateLogs
import support.installTestMainDispatcher
import support.loadedViewModel
import support.selectedIndices
import support.uninstallTestMainDispatcher
import support.useCompactMode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Selection semantics. Selection is tracked as an id set in [MainViewModel], outside
 * of [adb.data.LogcatMessage].
 */
class LogSelectionTest {

    @AfterTest
    fun tearDown() = uninstallTestMainDispatcher()

    @Test
    fun `single click selects exactly one log`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectSingleLog(id = 10)

        assertEquals(listOf(10), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `a second single click replaces the previous selection`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectSingleLog(id = 10)
        viewModel.selectSingleLog(id = 20)

        assertEquals(listOf(20), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `range select covers the span between anchor and target`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectSingleLog(id = 10)
        viewModel.selectRangeLog(id = 14)

        assertEquals(listOf(10, 11, 12, 13, 14), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `range select works backwards`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectSingleLog(id = 20)
        viewModel.selectRangeLog(id = 17)

        assertEquals(listOf(17, 18, 19, 20), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `range select without an anchor falls back to single select`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectRangeLog(id = 12)

        assertEquals(listOf(12), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `range select skips logs excluded by the active filter`() = runTest {
        installTestMainDispatcher(this)
        // levels cycle V,D,I,W,E,F -> INFO lands on indices 2, 8, 14, 20...
        val viewModel = loadedViewModel(generateLogs(60)).useCompactMode()
        viewModel.updateLogLevelFilter(LogLevel.INFO)

        viewModel.selectSingleLog(id = 2)
        viewModel.selectRangeLog(id = 20)

        assertEquals(listOf(2, 8, 14, 20), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `toggle adds and removes a single log from the selection`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectSingleLog(id = 5)
        viewModel.toggleSingleLogSelection(id = 9)
        assertEquals(listOf(5, 9), viewModel.selectedIndices())

        viewModel.toggleSingleLogSelection(id = 5)
        assertEquals(listOf(9), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `arrow navigation moves the selection by one`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectSingleLog(id = 10)

        assertEquals(11, viewModel.selectAdjacentLog(direction = 1))
        assertEquals(listOf(11), viewModel.selectedIndices())

        assertEquals(10, viewModel.selectAdjacentLog(direction = -1))
        assertEquals(listOf(10), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `arrow navigation stops at both ends`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(10)).useCompactMode()

        viewModel.selectSingleLog(id = 0)
        assertNull(viewModel.selectAdjacentLog(direction = -1))

        viewModel.selectSingleLog(id = 9)
        assertNull(viewModel.selectAdjacentLog(direction = 1))
        viewModel.onCleared()
    }

    @Test
    fun `shift arrow extends the selection from the anchor`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectSingleLog(id = 10)
        viewModel.selectAdjacentLog(direction = 1, extendSelection = true)
        viewModel.selectAdjacentLog(direction = 1, extendSelection = true)

        assertEquals(listOf(10, 11, 12), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `shift arrow shrinks back towards the anchor`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50)).useCompactMode()

        viewModel.selectSingleLog(id = 10)
        viewModel.selectAdjacentLog(direction = 1, extendSelection = true)
        viewModel.selectAdjacentLog(direction = 1, extendSelection = true)
        viewModel.selectAdjacentLog(direction = -1, extendSelection = true)

        assertEquals(listOf(10, 11), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `copy text uses the full format outside compact mode`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(20))

        viewModel.selectSingleLog(id = 3)
        val line = viewModel.getSelectedLogsAsText()

        val columns = line.split("\t")
        assertEquals(6, columns.size, "timestamp/level/pid/tid/tag/message")
        assertEquals("1003", columns[2])
        assertEquals("message body 3", columns[5])
        viewModel.onCleared()
    }

    @Test
    fun `copy text is empty when nothing is selected`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(20))

        assertTrue(viewModel.getSelectedLogsAsText().isEmpty())
        viewModel.onCleared()
    }
}
