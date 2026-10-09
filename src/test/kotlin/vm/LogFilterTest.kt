package vm

import adb.data.LogLevel
import kotlinx.coroutines.test.runTest
import support.generateLogs
import support.installTestMainDispatcher
import support.loadedViewModel
import support.uninstallTestMainDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks in the observable filtering behaviour so changes to the log pipeline
 * cannot silently change which logs are shown.
 */
class LogFilterTest {

    @AfterTest
    fun tearDown() = uninstallTestMainDispatcher()

    @Test
    fun `no filter shows every log`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(200))

        assertEquals(200, viewModel.uiState.value.filteredLogs.size)
        assertEquals(200, viewModel.uiState.value.allLogCount)
        viewModel.onCleared()
    }

    @Test
    fun `level filter matches that level exactly`() = runTest {
        installTestMainDispatcher(this)
        val logs = generateLogs(120)
        val viewModel = loadedViewModel(logs)

        viewModel.updateLogLevelFilter(LogLevel.WARN)

        val expected = logs.count { it.level == LogLevel.WARN }
        assertTrue(expected > 0, "fixture should contain WARN logs")
        assertEquals(expected, viewModel.uiState.value.filteredLogs.size)
        assertTrue(viewModel.uiState.value.filteredLogs.all { it.level == LogLevel.WARN })
        // allLogCount keeps counting everything, not just what passes the filter
        assertEquals(120, viewModel.uiState.value.allLogCount)
        viewModel.onCleared()
    }

    @Test
    fun `clearing the level filter restores every log`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(120))

        viewModel.updateLogLevelFilter(LogLevel.ERROR)
        viewModel.updateLogLevelFilter(null)

        assertEquals(120, viewModel.uiState.value.filteredLogs.size)
        viewModel.onCleared()
    }

    @Test
    fun `pid filter matches exactly`() = runTest {
        installTestMainDispatcher(this)
        val logs = generateLogs(100, pidCount = 4)
        val viewModel = loadedViewModel(logs)

        viewModel.updatePidFilter(1001)

        val expected = logs.count { it.pid == 1001 }
        assertEquals(expected, viewModel.uiState.value.filteredLogs.size)
        assertTrue(viewModel.uiState.value.filteredLogs.all { it.pid == 1001 })
        viewModel.onCleared()
    }

    @Test
    fun `tag filter is a case insensitive substring match`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(100, tagCount = 10))

        viewModel.updateTagFilter("tag1")

        // Tag0..Tag9 -> only "Tag1" contains "tag1"
        assertTrue(viewModel.uiState.value.filteredLogs.isNotEmpty())
        assertTrue(viewModel.uiState.value.filteredLogs.all { it.tag == "Tag1" })
        viewModel.onCleared()
    }

    @Test
    fun `message filter is a case insensitive substring match`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(50))

        viewModel.updateMessageFilter("BODY 7")

        assertEquals(1, viewModel.uiState.value.filteredLogs.size)
        assertEquals("message body 7", viewModel.uiState.value.filteredLogs.first().message)
        viewModel.onCleared()
    }

    @Test
    fun `filters combine with AND`() = runTest {
        installTestMainDispatcher(this)
        val logs = generateLogs(400, tagCount = 10, pidCount = 4)
        val viewModel = loadedViewModel(logs)

        viewModel.updateLogLevelFilter(LogLevel.INFO)
        viewModel.updatePidFilter(1002)
        viewModel.updateTagFilter("Tag3")

        val expected = logs.count {
            it.level == LogLevel.INFO && it.pid == 1002 && it.tag.contains("Tag3", ignoreCase = true)
        }
        assertEquals(expected, viewModel.uiState.value.filteredLogs.size)
        viewModel.onCleared()
    }

    @Test
    fun `blank text filters are treated as no filter`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(30))

        viewModel.updateTagFilter("   ")
        viewModel.updateMessageFilter("")

        assertEquals(30, viewModel.uiState.value.filteredLogs.size)
        viewModel.onCleared()
    }

    @Test
    fun `clearLogs empties both the buffer and the filtered view`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(80))

        viewModel.clearLogs()

        assertEquals(0, viewModel.uiState.value.allLogCount)
        assertEquals(0, viewModel.uiState.value.filteredLogs.size)
        viewModel.onCleared()
    }
}
