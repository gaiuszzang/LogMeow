package repository

import data.AppSettings
import data.DeepLinkHistory
import kotlinx.coroutines.flow.StateFlow

interface MainRepository {
    fun updateDeepLinkHistory(history: DeepLinkHistory)
    fun getDeepLinkHistoryFlow(): StateFlow<DeepLinkHistory>

    fun updateSettings(settings: AppSettings)
    fun getSettingsFlow(): StateFlow<AppSettings>
}
