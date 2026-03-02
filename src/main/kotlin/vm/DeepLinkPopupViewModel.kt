package vm

import adb.AdbService
import data.DeepLinkHistory
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
import repository.MainRepository

data class DeepLinkUiState(
    val inputText: String = "",
    val history: ImmutableList<String> = persistentListOf(),
    val selectedIndex: Int? = null
)

class DeepLinkPopupViewModel(
    private val adbService: AdbService,
    private val deviceId: String,
    private val mainRepository: MainRepository
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(DeepLinkUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mainRepository.getDeepLinkHistoryFlow().collect { history ->
                _uiState.value = _uiState.value.copy(
                    history = history.list.toImmutableList()
                )
            }
        }
    }

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
        currentHistory.remove(scheme)
        currentHistory.add(0, scheme)
        val newHistory = currentHistory.toImmutableList()
        _uiState.value = _uiState.value.copy(history = newHistory, selectedIndex = null) //TODO m.c.shin 필요할까?
        mainRepository.updateDeepLinkHistory(DeepLinkHistory(list = currentHistory))
    }

    fun selectHistoryItem(index: Int) {
        _uiState.value = _uiState.value.copy(selectedIndex = index)
    }

    fun executeHistoryItem(scheme: String) {
        _uiState.value = _uiState.value.copy(inputText = scheme)
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
