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

data class DeepLinkUiState(
    val inputText: String = "",
    val history: ImmutableList<String> = persistentListOf()
)

class DeepLinkPopupViewModel(
    private val adbService: AdbService,
    private val deviceId: String
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(DeepLinkUiState())
    val uiState = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun executeDeepLink() {
        val scheme = _uiState.value.inputText.trim()
        if (scheme.isEmpty()) return

        viewModelScope.launch {
            try {
                adbService.executeDeepLink(deviceId, scheme)
                addToHistory(scheme)
                _uiState.value = _uiState.value.copy(inputText = "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addToHistory(scheme: String) {
        val currentHistory = _uiState.value.history.toMutableList()

        // Remove if already exists
        currentHistory.remove(scheme)

        // Add to the top
        currentHistory.add(0, scheme)

        _uiState.value = _uiState.value.copy(history = currentHistory.toImmutableList())
    }

    fun executeHistoryItem(scheme: String) {
        viewModelScope.launch {
            try {
                adbService.executeDeepLink(deviceId, scheme)
                addToHistory(scheme)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}
