package support

import adb.AdbService
import adb.data.AdbDevice
import adb.data.AdbDeviceState
import adb.data.LogLevel
import adb.data.LogcatMessage
import data.AppSettings
import data.DeepLinkHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import repository.MainRepository

const val FAKE_DEVICE_ID = "fake-device"

/**
 * [AdbService] stand-in that emits a caller-supplied logcat stream instead of
 * spawning a real adb process.
 */
class FakeAdbService(
    private val logcat: Flow<LogcatMessage> = emptyFlow(),
    private val devices: List<AdbDevice> = listOf(AdbDevice(FAKE_DEVICE_ID, AdbDeviceState.DEVICE))
) : AdbService() {

    var clearLogcatCount = 0
        private set

    override fun getDevicesFlow(): Flow<List<AdbDevice>> = flowOf(devices)

    override fun getLogcatFlow(deviceId: String): Flow<LogcatMessage> = logcat

    override suspend fun clearLogcat(deviceId: String) {
        clearLogcatCount++
    }
}

/** In-memory [MainRepository]; nothing touches the user's real ~/.logmeow. */
class FakeRepository(
    settings: AppSettings = AppSettings()
) : MainRepository {

    private val settingsFlow = MutableStateFlow(settings)
    private val historyFlow = MutableStateFlow(DeepLinkHistory())

    override fun updateDeepLinkHistory(history: DeepLinkHistory) {
        historyFlow.value = history
    }

    override fun getDeepLinkHistoryFlow(): StateFlow<DeepLinkHistory> = historyFlow.asStateFlow()

    override fun updateSettings(settings: AppSettings) {
        settingsFlow.value = settings
    }

    override fun getSettingsFlow(): StateFlow<AppSettings> = settingsFlow.asStateFlow()
}

/**
 * Deterministic log generator. [tagCount] and [levels] control how selective the
 * filters under test end up being.
 */
fun generateLogs(
    count: Int,
    startId: Long = 0,
    tagCount: Int = 10,
    pidCount: Int = 4,
    levels: List<LogLevel> = LogLevel.entries
): List<LogcatMessage> = List(count) { index ->
    LogcatMessage(
        id = startId + index,
        timestamp = "01-01 00:00:%02d.%03d".format((index / 1000) % 60, index % 1000),
        pid = 1000 + (index % pidCount),
        tid = 2000 + (index % pidCount),
        level = levels[index % levels.size],
        tag = "Tag${index % tagCount}",
        message = "message body $index"
    )
}
