package data

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val themeName: String = "IslandsDark"
)
