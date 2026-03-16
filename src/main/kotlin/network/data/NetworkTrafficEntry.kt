package network.data

data class NetworkTrafficEntry(
    val id: Int,
    val appId: String = "unknown",
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val statusCode: Int? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long? = null,
    val error: String? = null,
    val responseType: ResponseType = ResponseType.REAL
) {
    val path: String
        get() = try {
            val uri = java.net.URI(url)
            val p = uri.path ?: url
            val q = uri.query
            if (q.isNullOrEmpty()) p else "$p?$q"
        } catch (_: Exception) {
            url
        }

    val isCompleted: Boolean
        get() = statusCode != null || error != null
}

enum class ResponseType {
    REAL, MOCK
}

enum class MockSupportType {
    ALWAYS, CONNECTED_ONLY, DISABLED
}

data class MockApiSetting(
    val id: String,
    val method: String,
    val url: String,
    val statusCode: Int = 200,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val delayMs: Long = 0
)
