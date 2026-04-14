package data

import kotlinx.serialization.Serializable

@Serializable
data class DeepLinkHistoryItem(
    val scheme: String,
    val extraArgs: String = ""
)

@Serializable
data class DeepLinkHistory(
    val list: List<DeepLinkHistoryItem> = emptyList()
)
