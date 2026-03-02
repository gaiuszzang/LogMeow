package repository

import data.DeepLinkHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class MainRepositoryImpl : MainRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val logMeowDir: File by lazy {
        val homeDir = System.getProperty("user.home")
        File(homeDir, ".logmeow").also { it.mkdirs() }
    }

    private val historyFile: File
        get() = File(logMeowDir, "deeplink_history.json")

    private val _deepLinkHistoryFlow: MutableStateFlow<DeepLinkHistory>

    init {
        _deepLinkHistoryFlow = MutableStateFlow(loadHistory())
    }

    override fun updateDeepLinkHistory(history: DeepLinkHistory) {
        _deepLinkHistoryFlow.value = history
        scope.launch {
            try {
                historyFile.writeText(json.encodeToString(DeepLinkHistory.serializer(), history))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getDeepLinkHistoryFlow(): StateFlow<DeepLinkHistory> {
        return _deepLinkHistoryFlow.asStateFlow()
    }

    private fun loadHistory(): DeepLinkHistory {
        return try {
            if (historyFile.exists()) {
                json.decodeFromString(DeepLinkHistory.serializer(), historyFile.readText())
            } else {
                DeepLinkHistory()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DeepLinkHistory()
        }
    }
}
