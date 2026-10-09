package vm

import data.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import support.LogSource
import support.generateLogs
import support.installTestMainDispatcher
import support.streamingViewModel
import support.useCompactMode
import support.selectedIndices
import support.uninstallTestMainDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Buffer trimming: the buffer holds exactly maxLogCount logs, dropping the oldest
 * first, and selection/bookmarks on dropped logs go with them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogBufferTrimTest {

    @AfterTest
    fun tearDown() = uninstallTestMainDispatcher()

    @Test
    fun `buffer never exceeds maxLogCount`() = runTest {
        installTestMainDispatcher(this)
        val source = LogSource()
        val viewModel = streamingViewModel(source, AppSettings(maxLogCount = 100))

        source.emit(generateLogs(250))
        advanceUntilIdle()

        assertEquals(100, viewModel.uiState.value.allLogCount)
        assertEquals(100, viewModel.uiState.value.filteredLogs.size)
        viewModel.onCleared()
    }

    @Test
    fun `trimming drops the oldest logs first`() = runTest {
        installTestMainDispatcher(this)
        val source = LogSource()
        val viewModel = streamingViewModel(source, AppSettings(maxLogCount = 100))

        source.emit(generateLogs(250))
        advanceUntilIdle()

        val filtered = viewModel.uiState.value.filteredLogs
        assertEquals(150L, filtered.first().id)
        assertEquals(249L, filtered.last().id)
        viewModel.onCleared()
    }

    @Test
    fun `bookmarks on retained logs survive trimming`() = runTest {
        installTestMainDispatcher(this)
        val source = LogSource()
        val viewModel = streamingViewModel(source, AppSettings(maxLogCount = 100))

        source.emit(generateLogs(100))
        advanceUntilIdle()
        viewModel.toggleBookmarkForLog(id = 95)

        source.emit(generateLogs(50, startId = 100))
        advanceUntilIdle()

        // retained window is ids 50..149, so 95 is still there
        assertEquals(1, viewModel.uiState.value.bookmarkCount)
        assertEquals(listOf(45), viewModel.uiState.value.bookmarkedIndicesInFilteredLogs)
        viewModel.onCleared()
    }

    @Test
    fun `bookmarks on trimmed logs disappear with them`() = runTest {
        installTestMainDispatcher(this)
        val source = LogSource()
        val viewModel = streamingViewModel(source, AppSettings(maxLogCount = 100))

        source.emit(generateLogs(100))
        advanceUntilIdle()
        viewModel.toggleBookmarkForLog(id = 5)

        source.emit(generateLogs(50, startId = 100))
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.bookmarkCount)
        viewModel.onCleared()
    }

    @Test
    fun `logs arriving live are appended in order`() = runTest {
        installTestMainDispatcher(this)
        val source = LogSource()
        val viewModel = streamingViewModel(source)

        source.emit(generateLogs(10))
        advanceUntilIdle()
        assertEquals(10, viewModel.uiState.value.allLogCount)

        source.emit(generateLogs(5, startId = 10))
        advanceUntilIdle()

        assertEquals(15, viewModel.uiState.value.allLogCount)
        assertEquals(
            (0L until 15L).toList(),
            viewModel.uiState.value.filteredLogs.map { it.id }
        )
        viewModel.onCleared()
    }

    @Test
    fun `live logs that fail the filter stay out of the filtered view`() = runTest {
        installTestMainDispatcher(this)
        val source = LogSource()
        val viewModel = streamingViewModel(source)

        viewModel.updateTagFilter("Tag3")
        source.emit(generateLogs(30, tagCount = 10))
        advanceUntilIdle()

        assertEquals(30, viewModel.uiState.value.allLogCount)
        assertEquals(3, viewModel.uiState.value.filteredLogs.size)
        viewModel.onCleared()
    }

    @Test
    fun `selection on trimmed logs disappears with them`() = runTest {
        installTestMainDispatcher(this)
        val source = LogSource()
        val viewModel = streamingViewModel(source, AppSettings(maxLogCount = 100)).useCompactMode()

        source.emit(generateLogs(100))
        advanceUntilIdle()
        viewModel.selectSingleLog(id = 40)
        viewModel.selectRangeLog(id = 60)

        source.emit(generateLogs(50, startId = 100))
        advanceUntilIdle()

        // retained window is ids 50..149
        assertEquals((50..60).toList(), viewModel.selectedIndices())
        assertEquals((50L..60L).toSet(), viewModel.uiState.value.selectedIds)
        viewModel.onCleared()
    }

    @Test
    fun `range select falls back to single select once the anchor is trimmed`() = runTest {
        installTestMainDispatcher(this)
        val source = LogSource()
        val viewModel = streamingViewModel(source, AppSettings(maxLogCount = 100)).useCompactMode()

        source.emit(generateLogs(100))
        advanceUntilIdle()
        viewModel.selectSingleLog(id = 10)

        source.emit(generateLogs(50, startId = 100))
        advanceUntilIdle()
        viewModel.selectRangeLog(id = 70)

        assertEquals(listOf(70), viewModel.selectedIndices())
        viewModel.onCleared()
    }
}
