package repository

import data.AppSettings
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

    private val settingsFile: File
        get() = File(logMeowDir, "settings.json")

    private val _deepLinkHistoryFlow: MutableStateFlow<DeepLinkHistory>
    private val _settingsFlow: MutableStateFlow<AppSettings>

    init {
        _deepLinkHistoryFlow = MutableStateFlow(loadHistory())
        _settingsFlow = MutableStateFlow(loadSettings())
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

    override fun updateSettings(settings: AppSettings) {
        _settingsFlow.value = settings
        scope.launch {
            try {
                settingsFile.writeText(json.encodeToString(AppSettings.serializer(), settings))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getSettingsFlow(): StateFlow<AppSettings> {
        return _settingsFlow.asStateFlow()
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

    private fun loadSettings(): AppSettings {
        return try {
            if (settingsFile.exists()) {
                json.decodeFromString(AppSettings.serializer(), settingsFile.readText())
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppSettings()
        }
    }
}
