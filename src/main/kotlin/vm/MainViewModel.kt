package vm

import adb.AdbService
import adb.data.AdbDevice
import adb.data.LogLevel
import adb.data.LogcatMessage
import repository.MainRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

enum class DisplayMode {
    All,
    Compact
}

data class UiState(
    val logLevelFilter: LogLevel? = null,
    val filterPid: Int? = null,
    val filterTag: String? = null,
    val filterMessage: String? = null,
    val filteredLogs: ImmutableList<LogcatMessage> = persistentListOf(),
    val selectedIds: ImmutableSet<Long> = persistentHashSetOf(),
    val bookmarkedIds: ImmutableSet<Long> = persistentHashSetOf(),
    val allLogCount: Int = 0,
    val bookmarkCount: Int = 0,
    val bookmarkedIndicesInFilteredLogs: ImmutableList<Int> = persistentListOf(),
    val displayMode: DisplayMode = DisplayMode.All
)

class MainViewModel(
    private val adbService: AdbService,
    private val repository: MainRepository
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Device list state
    private val _devices = MutableStateFlow<ImmutableList<AdbDevice>>(persistentListOf())
    val devices = _devices.asStateFlow()

    // Selected device state
    private val _selectedDevice = MutableStateFlow<AdbDevice?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    // Logging state
    private val _isLogging = MutableStateFlow(false)
    val isLogging = _isLogging.asStateFlow()

    // Screen recording state
    private val _isScreenRecording = MutableStateFlow(false)
    val isScreenRecording = _isScreenRecording.asStateFlow()

    // DeepLink popup state
    private val _isDeepLinkPopupVisible = MutableStateFlow(false)
    val isDeepLinkPopupVisible = _isDeepLinkPopupVisible.asStateFlow()
    private val _deepLinkFocusRequest = MutableStateFlow(0)
    val deepLinkFocusRequest = _deepLinkFocusRequest.asStateFlow()

    // Network Inspector popup state
    private val _isNetworkInspectorVisible = MutableStateFlow(false)
    val isNetworkInspectorVisible = _isNetworkInspectorVisible.asStateFlow()
    private val _networkInspectorFocusRequest = MutableStateFlow(0)
    val networkInspectorFocusRequest = _networkInspectorFocusRequest.asStateFlow()

    // Settings popup state
    private val _isSettingsVisible = MutableStateFlow(false)
    val isSettingsVisible = _isSettingsVisible.asStateFlow()

    // Settings
    val settingsFlow = repository.getSettingsFlow()

    // Logcat messages state (private - only managed internally)
    private var allLogs = LogBuffer()
    private var filteredLogs = LogBuffer()
    private var logFilter = LogFilter()

    // Selection and bookmarks are tracked by log id, outside of LogcatMessage, so
    // changing them never copies or re-filters the log buffers.
    private var selectedIds: PersistentSet<Long> = persistentHashSetOf()
    private var bookmarkedIds: PersistentSet<Long> = persistentHashSetOf()
    private var bookmarkedIndices: ImmutableList<Int> = persistentListOf()

    // Anchor for range selection and the moving end of a shift+arrow selection.
    private var focusLogId: Long? = null
    private var shiftCursorLogId: Long? = null

    // UI state
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    // Scroll event for bookmark navigation
    private val _scrollToFilteredIndex = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val scrollToFilteredIndex = _scrollToFilteredIndex.asSharedFlow()

    private var logcatJob: Job? = null
    private var screenRecordingOutputPath: String? = null

    init {
        observeDevices()
    }

    fun selectDevice(device: AdbDevice) {
        // If a different device is selected, stop current logging
        if (_selectedDevice.value?.id != device.id) {
            stopLogging()
            _selectedDevice.value = device
        } else {
            // If the same device is clicked, deselect it
            stopLogging()
            _selectedDevice.value = null
        }
    }

    fun toggleLogging() {
        if (_isLogging.value) {
            stopLogging()
        } else {
            startLogging()
        }
    }

    fun updateLogLevelFilter(logLevel: LogLevel?) {
        _uiState.value = _uiState.value.copy(logLevelFilter = logLevel)
        updateFilteredLogs()
    }

    fun updatePidFilter(pid: Int?) {
        _uiState.value = _uiState.value.copy(filterPid = pid)
        updateFilteredLogs()
    }

    fun updateTagFilter(tag: String?) {
        _uiState.value = _uiState.value.copy(filterTag = tag)
        updateFilteredLogs()
    }

    fun updateMessageFilter(message: String?) {
        _uiState.value = _uiState.value.copy(filterMessage = message)
        updateFilteredLogs()
    }

    fun clearLogs() {
        allLogs = LogBuffer()
        selectedIds = persistentHashSetOf()
        bookmarkedIds = persistentHashSetOf()
        focusLogId = null
        shiftCursorLogId = null
        updateFilteredLogs()
    }

    fun captureScreenshot() {
        val deviceId = _selectedDevice.value?.id ?: return

        viewModelScope.launch {
            try {
                // 1. Create LogMeow directory in home directory
                val homeDir = System.getProperty("user.home")
                val mediaDir = File(homeDir, "LogMeow")
                if (!mediaDir.exists()) {
                    mediaDir.mkdirs()
                }

                // 2. Generate filename with current date and time
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
                val currentTime = dateFormat.format(Date())
                val fileName = "ScreenShot_${currentTime}.png"
                val outputPath = File(mediaDir, fileName).absolutePath

                // 3. Capture screenshot
                val success = adbService.captureScreenshot(deviceId, outputPath)

                if (success) {
                    // 4. Open media directory
                    openDirectory(mediaDir)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleScreenRecording() {
        if (_isScreenRecording.value) {
            stopScreenRecording()
        } else {
            startScreenRecording()
        }
    }

    fun launchScrcpy() {
        val deviceId = _selectedDevice.value?.id ?: return

        viewModelScope.launch {
            try {
                adbService.launchScrcpy(deviceId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showDeepLinkPopup() {
        if (_isDeepLinkPopupVisible.value) {
            _deepLinkFocusRequest.value++
        } else {
            _isDeepLinkPopupVisible.value = true
        }
    }

    fun hideDeepLinkPopup() {
        _isDeepLinkPopupVisible.value = false
    }

    fun showNetworkInspector() {
        if (_isNetworkInspectorVisible.value) {
            _networkInspectorFocusRequest.value++
        } else {
            _isNetworkInspectorVisible.value = true
        }
    }

    fun hideNetworkInspector() {
        _isNetworkInspectorVisible.value = false
    }

    fun showSettings() {
        _isSettingsVisible.value = true
    }

    fun hideSettings() {
        _isSettingsVisible.value = false
    }

    fun updateTheme(themeName: String) {
        val current = repository.getSettingsFlow().value
        repository.updateSettings(current.copy(themeName = themeName))
    }

    fun updateMaxLogCount(maxLogCount: Int) {
        val current = repository.getSettingsFlow().value
        repository.updateSettings(current.copy(maxLogCount = maxLogCount))
    }

    fun updateMaxTrafficCount(maxTrafficCount: Int) {
        val current = repository.getSettingsFlow().value
        repository.updateSettings(current.copy(maxTrafficCount = maxTrafficCount))
    }

    private fun startScreenRecording() {
        val deviceId = _selectedDevice.value?.id ?: return

        try {
            // 1. Create LogMeow directory in home directory
            val homeDir = System.getProperty("user.home")
            val mediaDir = File(homeDir, "LogMeow")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }

            // 2. Generate filename with current date and time
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
            val currentTime = dateFormat.format(Date())
            val fileName = "ScreenRecording_${currentTime}.mp4"
            screenRecordingOutputPath = File(mediaDir, fileName).absolutePath

            // 3. Start screen recording
            val success = adbService.startScreenRecording(deviceId)
            if (success) {
                _isScreenRecording.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopScreenRecording() {
        val deviceId = _selectedDevice.value?.id ?: return
        val outputPath = screenRecordingOutputPath ?: return

        viewModelScope.launch {
            try {
                // 1. Stop screen recording and save file
                val success = adbService.stopScreenRecording(deviceId, outputPath)

                if (success) {
                    // 2. Open media directory
                    val mediaDir = File(outputPath).parentFile
                    if (mediaDir != null) {
                        openDirectory(mediaDir)
                    }
                }

                _isScreenRecording.value = false
                screenRecordingOutputPath = null
            } catch (e: Exception) {
                e.printStackTrace()
                _isScreenRecording.value = false
                screenRecordingOutputPath = null
            }
        }
    }

    private fun openDirectory(directory: File) {
        try {
            val osName = System.getProperty("os.name").lowercase()
            when {
                osName.contains("mac") -> {
                    Runtime.getRuntime().exec(arrayOf("open", directory.absolutePath))
                }
                osName.contains("win") -> {
                    Runtime.getRuntime().exec(arrayOf("explorer", directory.absolutePath))
                }
                osName.contains("nix") || osName.contains("nux") -> {
                    Runtime.getRuntime().exec(arrayOf("xdg-open", directory.absolutePath))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun appendNewLogs(batch: List<LogcatMessage>) {
        val filteredSizeBefore = filteredLogs.size
        for (log in batch) {
            allLogs.add(log)
            if (logFilter.matches(log)) filteredLogs.add(log)
        }
        var filteredChanged = filteredLogs.size != filteredSizeBefore

        // trim oldest logs if exceeding maxLogCount
        var selectionChanged = false
        var bookmarksChanged = false
        val maxLogCount = repository.getSettingsFlow().value.maxLogCount
        if (maxLogCount > 0) {
            while (allLogs.size > maxLogCount) {
                val removedId = allLogs.removeFirst().id
                if (!filteredLogs.isEmpty() && filteredLogs.first().id == removedId) {
                    filteredLogs.removeFirst()
                    filteredChanged = true
                    // bookmark indices are positions in the filtered list
                    if (bookmarkedIds.isNotEmpty()) bookmarksChanged = true
                }
                if (removedId in selectedIds) {
                    selectedIds = selectedIds.remove(removedId)
                    selectionChanged = true
                }
                if (removedId in bookmarkedIds) {
                    bookmarkedIds = bookmarkedIds.remove(removedId)
                    bookmarksChanged = true
                }
            }
        }
        if (bookmarksChanged) bookmarkedIndices = computeBookmarkedIndices()

        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            filteredLogs = if (filteredChanged) filteredLogs.snapshot() else currentState.filteredLogs,
            selectedIds = if (selectionChanged) selectedIds else currentState.selectedIds,
            allLogCount = allLogs.size,
            bookmarkCount = bookmarkedIds.size,
            bookmarkedIndicesInFilteredLogs = bookmarkedIndices
        )
    }

    /** Full re-filter. Only needed when the filter itself or the whole buffer changes. */
    private fun updateFilteredLogs() {
        val state = _uiState.value
        logFilter = LogFilter(
            level = state.logLevelFilter,
            pid = state.filterPid,
            tag = state.filterTag?.takeIf { it.isNotBlank() },
            message = state.filterMessage?.takeIf { it.isNotBlank() }
        )
        val filtered = LogBuffer(allLogs.size)
        for (i in 0 until allLogs.size) {
            val log = allLogs[i]
            if (logFilter.matches(log)) filtered.add(log)
        }
        filteredLogs = filtered
        bookmarkedIndices = computeBookmarkedIndices()
        publishLogState()
    }

    private fun publishLogState() {
        _uiState.value = _uiState.value.copy(
            filteredLogs = filteredLogs.snapshot(),
            selectedIds = selectedIds,
            bookmarkedIds = bookmarkedIds,
            allLogCount = allLogs.size,
            bookmarkCount = bookmarkedIds.size,
            bookmarkedIndicesInFilteredLogs = bookmarkedIndices
        )
    }

    private fun publishSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = selectedIds)
    }

    private fun publishBookmarks() {
        bookmarkedIndices = computeBookmarkedIndices()
        _uiState.value = _uiState.value.copy(
            bookmarkedIds = bookmarkedIds,
            bookmarkCount = bookmarkedIds.size,
            bookmarkedIndicesInFilteredLogs = bookmarkedIndices
        )
    }

    private fun computeBookmarkedIndices(): ImmutableList<Int> {
        if (bookmarkedIds.isEmpty()) return persistentListOf()
        return bookmarkedIds
            .mapNotNull { id -> filteredLogs.indexOfId(id).takeIf { it >= 0 } }
            .sorted()
            .toImmutableList()
    }

    private fun containsLog(id: Long): Boolean = allLogs.indexOfId(id) >= 0

    /** Ids of the filtered logs between [fromIndex] and [toIndex], inclusive. */
    private fun filteredIdsBetween(fromIndex: Int, toIndex: Int): PersistentSet<Long> {
        val builder = persistentHashSetOf<Long>().builder()
        for (i in fromIndex..toIndex) builder.add(filteredLogs[i].id)
        return builder.build()
    }

    /** Filtered-list index of the first (or last) selected log, or -1. */
    private fun selectedFilteredIndex(last: Boolean): Int {
        var result = -1
        for (id in selectedIds) {
            val index = filteredLogs.indexOfId(id)
            if (index < 0) continue
            if (result == -1 || (if (last) index > result else index < result)) result = index
        }
        return result
    }

    fun selectSingleLog(id: Long) {
        if (!containsLog(id)) return
        // Deselect all and select only the clicked one
        selectedIds = persistentHashSetOf(id)
        // Update focus and reset shift cursor
        focusLogId = id
        shiftCursorLogId = null
        publishSelection()
    }

    fun selectRangeLog(id: Long) {
        if (!containsLog(id)) return

        val anchorId = focusLogId
        if (anchorId == null || !containsLog(anchorId)) {
            // If the anchor is not valid, just select the clicked item
            selectSingleLog(id)
            return
        }

        // Select the range between the anchor and id, but only logs that match the current filter
        val from = filteredLogs.lowerBound(minOf(anchorId, id))
        val to = filteredLogs.lowerBound(maxOf(anchorId, id) + 1) - 1
        selectedIds = if (from <= to) filteredIdsBetween(from, to) else persistentHashSetOf()
        shiftCursorLogId = null
        publishSelection()
    }

    fun toggleSingleLogSelection(id: Long) {
        if (!containsLog(id)) return
        selectedIds = if (id in selectedIds) selectedIds.remove(id) else selectedIds.add(id)
        // Update focus and reset shift cursor
        focusLogId = id
        shiftCursorLogId = null
        publishSelection()
    }

    /**
     * Move selection to the adjacent log in filtered list.
     * @param direction -1 for up, 1 for down
     * @param extendSelection if true, select range from anchor (focusLogId) to moving cursor
     * @return the target filtered index for scrolling, or null if no move
     */
    fun selectAdjacentLog(direction: Int, extendSelection: Boolean = false): Int? {
        if (filteredLogs.isEmpty()) return null
        val lastIndex = filteredLogs.size - 1

        if (extendSelection) {
            // Range selection from anchor point
            val anchorId = focusLogId ?: return null
            val anchorFilteredIndex = filteredLogs.indexOfId(anchorId)
            if (anchorFilteredIndex == -1) return null

            // Determine current cursor position, or start from anchor
            val currentCursor = shiftCursorLogId
                ?.let { filteredLogs.indexOfId(it) }
                ?.takeIf { it >= 0 }
                ?: anchorFilteredIndex
            val newCursor = (currentCursor.toLong() + direction).coerceIn(0L, lastIndex.toLong()).toInt()
            if (newCursor == currentCursor) return null

            selectedIds = filteredIdsBetween(minOf(anchorFilteredIndex, newCursor), maxOf(anchorFilteredIndex, newCursor))
            shiftCursorLogId = filteredLogs[newCursor].id
            publishSelection()
            return newCursor
        } else {
            // Normal single selection
            val currentFilteredIndex = selectedFilteredIndex(last = direction > 0)
            val targetIndex = if (currentFilteredIndex == -1) {
                0
            } else {
                (currentFilteredIndex.toLong() + direction).coerceIn(0L, lastIndex.toLong()).toInt()
            }
            if (targetIndex == currentFilteredIndex) return null

            selectSingleLog(filteredLogs[targetIndex].id)
            return targetIndex
        }
    }

    fun getSelectedLogsAsText(): String {
        val isCompact = _uiState.value.displayMode == DisplayMode.Compact
        return selectedIds
            .sorted()
            .mapNotNull { id -> allLogs.indexOfId(id).takeIf { it >= 0 }?.let { allLogs[it] } }
            .joinToString("\n") { log ->
                if (isCompact) {
                    log.message
                } else {
                    "${log.timestamp}\t${log.level.name.first()}\t${log.pid}\t${log.tid}\t${log.tag}\t${log.message}"
                }
            }
    }

    fun toggleBookmarkForSelectedLogs() {
        if (selectedIds.isEmpty()) return

        // If any selected log is not bookmarked, bookmark all. Otherwise, unbookmark all.
        val shouldBookmark = selectedIds.any { it !in bookmarkedIds }
        bookmarkedIds = if (shouldBookmark) bookmarkedIds.addAll(selectedIds) else bookmarkedIds.removeAll(selectedIds)
        publishBookmarks()
    }

    fun toggleBookmarkForLog(id: Long) {
        if (!containsLog(id)) return
        bookmarkedIds = if (id in bookmarkedIds) bookmarkedIds.remove(id) else bookmarkedIds.add(id)
        publishBookmarks()
    }

    fun navigateToPreviousBookmark() {
        val bookmarkedIndices = _uiState.value.bookmarkedIndicesInFilteredLogs
        if (bookmarkedIndices.isEmpty()) return

        val currentIndex = selectedFilteredIndex(last = false).takeIf { it >= 0 }
        val target = if (currentIndex != null) {
            bookmarkedIndices.lastOrNull { it < currentIndex } ?: bookmarkedIndices.last()
        } else {
            bookmarkedIndices.last()
        }

        selectSingleLog(filteredLogs[target].id)
        _scrollToFilteredIndex.tryEmit(target)
    }

    fun navigateToNextBookmark() {
        val bookmarkedIndices = _uiState.value.bookmarkedIndicesInFilteredLogs
        if (bookmarkedIndices.isEmpty()) return

        val currentIndex = selectedFilteredIndex(last = false).takeIf { it >= 0 }
        val target = if (currentIndex != null) {
            bookmarkedIndices.firstOrNull { it > currentIndex } ?: bookmarkedIndices.first()
        } else {
            bookmarkedIndices.first()
        }

        selectSingleLog(filteredLogs[target].id)
        _scrollToFilteredIndex.tryEmit(target)
    }

    fun toggleDisplayMode() {
        val current = _uiState.value.displayMode
        val newMode = if (current == DisplayMode.All) DisplayMode.Compact else DisplayMode.All
        _uiState.value = _uiState.value.copy(displayMode = newMode)
    }

    @Suppress("unused")
    fun scrollToFilteredLogIndex(index: Int): Long? {
        val filteredLogs = _uiState.value.filteredLogs
        if (index in filteredLogs.indices) {
            return filteredLogs[index].id
        }
        return null
    }

    @OptIn(FlowPreview::class)
    private fun startLogging() {
        val deviceId = _selectedDevice.value?.id ?: return

        _isLogging.value = true
        logcatJob = viewModelScope.launch {
            // Always clear the device's log buffer before starting a new collection
            adbService.clearLogcat(deviceId)

            // Drain whatever has queued up since the last pass and apply it as one
            // batch, so a burst of logs costs one state update instead of one per line.
            val channel = adbService.getLogcatFlow(deviceId)
                .buffer(Channel.UNLIMITED)
                .produceIn(this)
            val batch = ArrayList<LogcatMessage>(MAX_LOG_BATCH_SIZE)
            for (logMessage in channel) {
                batch.add(logMessage)
                while (batch.size < MAX_LOG_BATCH_SIZE) {
                    batch.add(channel.tryReceive().getOrNull() ?: break)
                }
                appendNewLogs(batch)
                batch.clear()
            }
        }
    }

    private fun stopLogging() {
        logcatJob?.cancel()
        logcatJob = null
        _isLogging.value = false
    }

    private fun observeDevices() {
        adbService.getDevicesFlow()
            .onEach { deviceList ->
                _devices.value = deviceList.toImmutableList()
                // If the selected device is disconnected, deselect it
                if (_selectedDevice.value != null && deviceList.none { it.id == _selectedDevice.value!!.id }) {
                    _isNetworkInspectorVisible.value = false
                    selectDevice(_selectedDevice.value!!) // This will stop logging and deselect
                }
                // Auto-select the first device when no device is currently selected
                if (_selectedDevice.value == null && deviceList.isNotEmpty()) {
                    _selectedDevice.value = deviceList.first()
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCleared() {
        viewModelScope.cancel()
    }

    companion object {
        private const val MAX_LOG_BATCH_SIZE = 5_000
    }
}

private data class LogFilter(
    val level: LogLevel? = null,
    val pid: Int? = null,
    val tag: String? = null,
    val message: String? = null
) {
    fun matches(log: LogcatMessage): Boolean =
        (level == null || log.level == level) &&
            (pid == null || log.pid == pid) &&
            (tag == null || log.tag.contains(tag, ignoreCase = true)) &&
            (message == null || log.message.contains(message, ignoreCase = true))
}
