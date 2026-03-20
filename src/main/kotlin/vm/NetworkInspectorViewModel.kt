package vm

import adb.AdbService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.LibraryConnectionService
import io.groovin.logmeow.interceptor.MockApiSettingDto
import io.groovin.logmeow.interceptor.MockSupportType
import network.data.NetworkTrafficEntry

data class MockApiDialogRequest(
    val prefillMethod: String? = null,
    val prefillUrl: String? = null,
    val prefillStatusCode: Int? = null,
    val prefillResponseHeaders: Map<String, String> = emptyMap(),
    val prefillResponseBody: String? = null,
)

data class NetworkInspectorUiState(
    val isServerRunning: Boolean = false,
    val serverPort: Int = LibraryConnectionService.DEFAULT_PORT,
    val connectedApps: Set<String> = emptySet(),
    val selectedAppId: String? = null,
    val trafficList: ImmutableList<NetworkTrafficEntry> = persistentListOf(),
    val selectedTraffic: NetworkTrafficEntry? = null,
    val mockApiSettings: ImmutableList<MockApiSettingDto> = persistentListOf(),
    val showMockApiDialog: Boolean = false,
    val mockApiDialogRequest: MockApiDialogRequest? = null,
    val mockSupportEnabled: Boolean = true,
)

class NetworkInspectorViewModel(
    adbService: AdbService,
    private val deviceId: String
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val service = LibraryConnectionService(adbService)

    private val _uiState = MutableStateFlow(NetworkInspectorUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { service.startServer(deviceId) }
        observeService()
    }

    private fun observeService() {
        viewModelScope.launch {
            service.isServerRunning.collect { running ->
                _uiState.value = _uiState.value.copy(isServerRunning = running)
            }
        }
        viewModelScope.launch {
            service.connectedApps.collect { apps ->
                val current = _uiState.value
                var newSelectedAppId = current.selectedAppId

                // Auto-select: if new app connected and nothing selected, or new app appeared
                if (newSelectedAppId == null || newSelectedAppId !in apps) {
                    // Find newly added app
                    val newApps = apps - current.connectedApps
                    newSelectedAppId = newApps.firstOrNull() ?: apps.firstOrNull()
                }

                _uiState.value = current.copy(
                    connectedApps = apps,
                    selectedAppId = newSelectedAppId
                )
                // Refresh traffic/mock for new selection
                if (newSelectedAppId != current.selectedAppId) {
                    refreshForSelectedApp(newSelectedAppId)
                }
            }
        }
        viewModelScope.launch {
            service.clientTraffic.collect { trafficMap ->
                val appId = _uiState.value.selectedAppId ?: return@collect
                val entries = trafficMap[appId].orEmpty()
                _uiState.value = _uiState.value.copy(trafficList = entries.toImmutableList())
            }
        }
        viewModelScope.launch {
            service.clientMockSettings.collect { mockMap ->
                val appId = _uiState.value.selectedAppId ?: return@collect
                val settings = mockMap[appId].orEmpty()
                _uiState.value = _uiState.value.copy(mockApiSettings = settings.toImmutableList())
            }
        }
        viewModelScope.launch {
            service.clientMockSupportType.collect { typeMap ->
                val appId = _uiState.value.selectedAppId ?: return@collect
                val type = typeMap[appId] ?: MockSupportType.ALWAYS
                _uiState.value = _uiState.value.copy(mockSupportEnabled = type != MockSupportType.DISABLED)
            }
        }
    }

    fun selectApp(appId: String) {
        _uiState.value = _uiState.value.copy(selectedAppId = appId, selectedTraffic = null)
        refreshForSelectedApp(appId)
    }

    private fun refreshForSelectedApp(appId: String?) {
        if (appId == null) {
            _uiState.value = _uiState.value.copy(
                trafficList = persistentListOf(),
                selectedTraffic = null,
                mockApiSettings = persistentListOf(),
                mockSupportEnabled = true
            )
            return
        }
        viewModelScope.launch {
            val traffic = service.clientTraffic.value[appId].orEmpty()
            val mock = service.clientMockSettings.value[appId].orEmpty()
            val type = service.clientMockSupportType.value[appId] ?: MockSupportType.ALWAYS
            _uiState.value = _uiState.value.copy(
                trafficList = traffic.toImmutableList(),
                selectedTraffic = null,
                mockApiSettings = mock.toImmutableList(),
                mockSupportEnabled = type != MockSupportType.DISABLED
            )
        }
    }

    fun selectTraffic(entry: NetworkTrafficEntry?) {
        _uiState.value = _uiState.value.copy(selectedTraffic = entry)
    }

    fun deleteTraffic(entry: NetworkTrafficEntry) {
        val current = _uiState.value
        val newList = current.trafficList.filter { it.id != entry.id }.toImmutableList()
        val newSelected = if (current.selectedTraffic?.id == entry.id) null else current.selectedTraffic
        _uiState.value = current.copy(trafficList = newList, selectedTraffic = newSelected)
    }

    fun clearTraffic() {
        val appId = _uiState.value.selectedAppId ?: return
        service.clearTraffic(appId)
        _uiState.value = _uiState.value.copy(
            trafficList = persistentListOf(),
            selectedTraffic = null
        )
    }

    fun showMockApiDialog() {
        _uiState.value = _uiState.value.copy(showMockApiDialog = true, mockApiDialogRequest = null)
    }

    fun showMockApiDialogForTraffic(entry: NetworkTrafficEntry) {
        _uiState.value = _uiState.value.copy(
            showMockApiDialog = true,
            mockApiDialogRequest = MockApiDialogRequest(
                prefillMethod = entry.method,
                prefillUrl = entry.url,
                prefillStatusCode = entry.statusCode,
                prefillResponseHeaders = entry.responseHeaders,
                prefillResponseBody = entry.responseBody,
            )
        )
    }

    fun hideMockApiDialog() {
        _uiState.value = _uiState.value.copy(showMockApiDialog = false, mockApiDialogRequest = null)
    }

    fun addMockApiSetting(setting: MockApiSettingDto) {
        val appId = _uiState.value.selectedAppId ?: return
        service.sendMockApiAdd(appId, setting)
    }

    fun updateMockApiSetting(setting: MockApiSettingDto) {
        val appId = _uiState.value.selectedAppId ?: return
        service.sendMockApiUpdate(appId, setting)
    }

    fun deleteMockApiSetting(settingId: String) {
        val appId = _uiState.value.selectedAppId ?: return
        service.sendMockApiDelete(appId, settingId)
    }

    fun clearMockApiSettings() {
        val appId = _uiState.value.selectedAppId ?: return
        service.sendMockApiClear(appId)
    }

    fun restartServer() {
        viewModelScope.launch { service.startServer(deviceId) }
    }

    fun onCleared() {
        service.onCleared()
        viewModelScope.cancel()
    }
}
