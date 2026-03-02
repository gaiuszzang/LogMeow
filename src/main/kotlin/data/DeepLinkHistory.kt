package data

import kotlinx.serialization.Serializable

@Serializable
data class DeepLinkHistory(
    val list: List<String> = emptyList()
)
