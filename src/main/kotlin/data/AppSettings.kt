package data

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val themeName: String = "IslandsDark",
    val maxLogCount: Int = 200_000,
    // Network Inspector traffic kept per connected app (desktop side only)
    val maxTrafficCount: Int = 10_000
)
