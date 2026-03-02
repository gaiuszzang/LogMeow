package repository

import data.DeepLinkHistory
import kotlinx.coroutines.flow.StateFlow

interface MainRepository {
    fun updateDeepLinkHistory(history: DeepLinkHistory)
    fun getDeepLinkHistoryFlow(): StateFlow<DeepLinkHistory>
}
