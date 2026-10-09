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

class LogBookmarkTest {

    @AfterTest
    fun tearDown() = uninstallTestMainDispatcher()

    @Test
    fun `toggling a bookmark flips it on and off`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(40))

        viewModel.toggleBookmarkForLog(id = 12)
        assertEquals(1, viewModel.uiState.value.bookmarkCount)
        assertEquals(listOf(12), viewModel.uiState.value.bookmarkedIndicesInFilteredLogs)

        viewModel.toggleBookmarkForLog(id = 12)
        assertEquals(0, viewModel.uiState.value.bookmarkCount)
        assertEquals(emptyList(), viewModel.uiState.value.bookmarkedIndicesInFilteredLogs)
        viewModel.onCleared()
    }

    @Test
    fun `bookmarking a selection bookmarks all of it`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(40)).useCompactMode()

        viewModel.selectSingleLog(id = 5)
        viewModel.selectRangeLog(id = 8)
        viewModel.toggleBookmarkForSelectedLogs()

        assertEquals(4, viewModel.uiState.value.bookmarkCount)
        assertEquals(listOf(5, 6, 7, 8), viewModel.uiState.value.bookmarkedIndicesInFilteredLogs)
        viewModel.onCleared()
    }

    @Test
    fun `a partially bookmarked selection becomes fully bookmarked`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(40)).useCompactMode()

        viewModel.toggleBookmarkForLog(id = 6)
        viewModel.selectSingleLog(id = 5)
        viewModel.selectRangeLog(id = 8)
        viewModel.toggleBookmarkForSelectedLogs()

        assertEquals(4, viewModel.uiState.value.bookmarkCount)
        viewModel.onCleared()
    }

    @Test
    fun `a fully bookmarked selection is cleared`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(40)).useCompactMode()

        viewModel.selectSingleLog(id = 5)
        viewModel.selectRangeLog(id = 8)
        viewModel.toggleBookmarkForSelectedLogs()
        viewModel.toggleBookmarkForSelectedLogs()

        assertEquals(0, viewModel.uiState.value.bookmarkCount)
        viewModel.onCleared()
    }

    @Test
    fun `bookmark indices are relative to the filtered list`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(60))

        viewModel.toggleBookmarkForLog(id = 8)  // INFO
        viewModel.toggleBookmarkForLog(id = 20) // INFO
        viewModel.updateLogLevelFilter(LogLevel.INFO)

        // INFO logs are ids 2, 8, 14, 20, ... -> filtered positions 1 and 3
        assertEquals(listOf(1, 3), viewModel.uiState.value.bookmarkedIndicesInFilteredLogs)
        assertEquals(2, viewModel.uiState.value.bookmarkCount)
        viewModel.onCleared()
    }

    @Test
    fun `next bookmark advances and wraps around`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(40)).useCompactMode()

        viewModel.toggleBookmarkForLog(id = 10)
        viewModel.toggleBookmarkForLog(id = 20)

        viewModel.navigateToNextBookmark()
        assertEquals(listOf(10), viewModel.selectedIndices())

        viewModel.navigateToNextBookmark()
        assertEquals(listOf(20), viewModel.selectedIndices())

        viewModel.navigateToNextBookmark()
        assertEquals(listOf(10), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `previous bookmark steps backwards and wraps around`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(40)).useCompactMode()

        viewModel.toggleBookmarkForLog(id = 10)
        viewModel.toggleBookmarkForLog(id = 20)

        viewModel.selectSingleLog(id = 25)
        viewModel.navigateToPreviousBookmark()
        assertEquals(listOf(20), viewModel.selectedIndices())

        viewModel.navigateToPreviousBookmark()
        assertEquals(listOf(10), viewModel.selectedIndices())

        viewModel.navigateToPreviousBookmark()
        assertEquals(listOf(20), viewModel.selectedIndices())
        viewModel.onCleared()
    }

    @Test
    fun `bookmark navigation is a no-op with no bookmarks`() = runTest {
        installTestMainDispatcher(this)
        val viewModel = loadedViewModel(generateLogs(40)).useCompactMode()

        viewModel.selectSingleLog(id = 3)
        viewModel.navigateToNextBookmark()
        viewModel.navigateToPreviousBookmark()

        assertEquals(listOf(3), viewModel.selectedIndices())
        viewModel.onCleared()
    }
}
