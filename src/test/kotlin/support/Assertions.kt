package support

import vm.MainViewModel

/**
 * Selected logs expressed as [generateLogs] indices, read through the public
 * copy-to-clipboard API so the assertion does not depend on how the view model
 * stores selection internally.
 *
 * Requires the view model to be in Compact display mode and the logs to come
 * from [generateLogs].
 */
fun MainViewModel.selectedIndices(): List<Int> {
    val text = getSelectedLogsAsText()
    if (text.isEmpty()) return emptyList()
    return text.lines().map { line ->
        line.removePrefix("message body ").toIntOrNull()
            ?: error("unexpected selected line: '$line' (is the view model in Compact mode?)")
    }
}

/** Switches to Compact mode so [selectedIndices] can read plain message bodies. */
fun MainViewModel.useCompactMode(): MainViewModel {
    if (uiState.value.displayMode != vm.DisplayMode.Compact) toggleDisplayMode()
    return this
}
