package vm

import kotlinx.coroutines.test.runTest
import support.generateLogs
import support.installTestMainDispatcher
import support.loadedViewModel
import support.uninstallTestMainDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainViewModelSmokeTest {

    @AfterTest
    fun tearDown() = uninstallTestMainDispatcher()

    @Test
    fun `harness streams logs into the view model`() = runTest {
        installTestMainDispatcher(this)

        val viewModel = loadedViewModel(generateLogs(100))

        assertEquals(100, viewModel.uiState.value.allLogCount)
        assertEquals(100, viewModel.uiState.value.filteredLogs.size)
        assertTrue(viewModel.isLogging.value)
        viewModel.onCleared()
    }
}
