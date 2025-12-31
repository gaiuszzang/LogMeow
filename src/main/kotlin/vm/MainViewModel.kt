package vm

import adb.AdbService
import adb.data.AdbDevice
import adb.data.LogLevel
import adb.data.LogcatMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

data class UiState(
    val logLevelFilter: LogLevel? = null,
    val filterPid: Int? = null,
    val filterTag: String? = null,
    val filterMessage: String? = null,
    val focusLogIndex: Int? = null,
    val filteredLogs: ImmutableList<LogcatMessage> = persistentListOf(),
    val allLogCount: Int = 0,
    val bookmarkCount: Int = 0,
    val bookmarkedIndicesInFilteredLogs: ImmutableList<Int> = persistentListOf()
)

class MainViewModel(
    private val adbService: AdbService
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

    // Logcat messages state (private - only managed internally)
    private var allLogs = mutableListOf<LogcatMessage>()

    // UI state
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

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
        allLogs.clear()
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
        _isDeepLinkPopupVisible.value = true
    }

    fun hideDeepLinkPopup() {
        _isDeepLinkPopupVisible.value = false
    }

    fun getAdbService(): AdbService = adbService

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

    private fun isLogMatchingFilter(log: LogcatMessage): Boolean {
        val levelMatch = _uiState.value.logLevelFilter == null || log.level == _uiState.value.logLevelFilter
        val pidMatch = _uiState.value.filterPid == null || log.pid == _uiState.value.filterPid
        val tagMatch = _uiState.value.filterTag.isNullOrBlank() ||
            log.tag.contains(_uiState.value.filterTag!!, ignoreCase = true)
        val messageMatch = _uiState.value.filterMessage.isNullOrBlank() ||
            log.message.contains(_uiState.value.filterMessage!!, ignoreCase = true)
        return levelMatch && pidMatch && tagMatch && messageMatch
    }

    private fun updateFilteredLogs() {
        val filtered = allLogs.filter { log ->
            val levelMatch = _uiState.value.logLevelFilter == null || log.level == _uiState.value.logLevelFilter
            val pidMatch = _uiState.value.filterPid == null || log.pid == _uiState.value.filterPid
            val tagMatch = _uiState.value.filterTag.isNullOrBlank() ||
                log.tag.contains(_uiState.value.filterTag!!, ignoreCase = true)
            val messageMatch = _uiState.value.filterMessage.isNullOrBlank() ||
                log.message.contains(_uiState.value.filterMessage!!, ignoreCase = true)
            levelMatch && pidMatch && tagMatch && messageMatch
        }.toImmutableList()

        // Calculate bookmarked indices in filtered logs
        val bookmarkedIndices = filtered.mapIndexedNotNull { index, log ->
            if (log.isBookmarked) index else null
        }.toImmutableList()

        val bookmarkCount = allLogs.count { it.isBookmarked }

        _uiState.value = _uiState.value.copy(
            filteredLogs = filtered,
            allLogCount = allLogs.size,
            bookmarkCount = bookmarkCount,
            bookmarkedIndicesInFilteredLogs = bookmarkedIndices
        )
    }

    fun selectSingleLog(id: Long) {
        val index = allLogs.indexOfFirst { it.id == id }
        if (index != -1) {
            // Deselect all and select only the clicked one
            for (i in allLogs.indices) {
                allLogs[i] = allLogs[i].copy(isSelected = i == index)
            }
            // Update focus index
            _uiState.value = _uiState.value.copy(focusLogIndex = index)
            updateFilteredLogs()
        }
    }

    fun selectRangeLog(id: Long) {
        val index = allLogs.indexOfFirst { it.id == id }
        if (index == -1) return

        val focusIndex = _uiState.value.focusLogIndex

        // Check if focusLogIndex is valid
        if (focusIndex != null && focusIndex in allLogs.indices) {
            // Select range between focusLogIndex and index
            val start = minOf(focusIndex, index)
            val end = maxOf(focusIndex, index)

            // Deselect all first
            for (i in allLogs.indices) {
                allLogs[i] = allLogs[i].copy(isSelected = false)
            }

            // Select range, but only logs that match the current filter
            for (i in start..end) {
                if (isLogMatchingFilter(allLogs[i])) {
                    allLogs[i] = allLogs[i].copy(isSelected = true)
                }
            }

            updateFilteredLogs()
        } else {
            // If focusLogIndex is not valid, just select the clicked item
            selectSingleLog(id)
        }
    }

    fun toggleSingleLogSelection(id: Long) {
        val index = allLogs.indexOfFirst { it.id == id }
        if (index != -1) {
            allLogs[index] = allLogs[index].copy(isSelected = !allLogs[index].isSelected)
            // Update focus index
            _uiState.value = _uiState.value.copy(focusLogIndex = index)
            updateFilteredLogs()
        }
    }

    //TODO Refactoring
    fun getSelectedLogsAsText(): String {
        return allLogs
            .filter { it.isSelected }
            .joinToString("\n") { log ->
                "${log.timestamp} ${log.level.name.first()} ${log.pid} ${log.tid} ${log.tag} ${log.message}"
            }
    }

    fun toggleBookmarkForSelectedLogs() {
        val selectedIndices = allLogs.indices.filter { allLogs[it].isSelected }
        if (selectedIndices.isEmpty()) return

        // If any selected log is not bookmarked, bookmark all. Otherwise, unbookmark all.
        val shouldBookmark = selectedIndices.any { !allLogs[it].isBookmarked }

        for (index in selectedIndices) {
            allLogs[index] = allLogs[index].copy(isBookmarked = shouldBookmark)
        }
        updateFilteredLogs()
    }

    fun toggleBookmarkForLog(id: Long) {
        val index = allLogs.indexOfFirst { it.id == id }
        if (index != -1) {
            allLogs[index] = allLogs[index].copy(isBookmarked = !allLogs[index].isBookmarked)
            updateFilteredLogs()
        }
    }

    fun scrollToFilteredLogIndex(index: Int): Long? {
        val filteredLogs = _uiState.value.filteredLogs
        if (index in filteredLogs.indices) {
            return filteredLogs[index].id
        }
        return null
    }

    private fun startLogging() {
        val deviceId = _selectedDevice.value?.id ?: return

        _isLogging.value = true
        logcatJob = viewModelScope.launch {
            // Always clear the device's log buffer before starting a new collection
            adbService.clearLogcat(deviceId)

            adbService.getLogcatFlow(deviceId)
                .onEach { logMessage ->
                    // Append new log to the existing list
                    allLogs.add(logMessage)
                    updateFilteredLogs()
                }
                .collect() // Use collect to keep it within this job
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
                    selectDevice(_selectedDevice.value!!) // This will stop logging and deselect
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}
