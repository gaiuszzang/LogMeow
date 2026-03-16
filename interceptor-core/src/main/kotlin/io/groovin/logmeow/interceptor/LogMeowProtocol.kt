package io.groovin.logmeow.interceptor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class LogMeowMessage

// ── Client → Server ──

@Serializable
@SerialName("handshake")
internal data class HandshakeMessage(
    val appId: String,
    val version: String = "1",
    val mockSupportType: String = "always",
    val mockApiSettings: List<MockApiSettingDto> = emptyList()
) : LogMeowMessage()

@Serializable
enum class ResponseType {
    @SerialName("real") REAL,
    @SerialName("mock") MOCK
}

@Serializable
@SerialName("traffic")
data class TrafficMessage(
    val method: String,
    val url: String,
    val statusCode: Int,
    val durationMs: Long,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val error: String? = null,
    val responseType: ResponseType = ResponseType.REAL
) : LogMeowMessage()

// ── Server → Client ──

@Serializable
@SerialName("mock_api_add")
internal data class MockApiAddMessage(
    val setting: MockApiSettingDto
) : LogMeowMessage()

@Serializable
@SerialName("mock_api_update")
internal data class MockApiUpdateMessage(
    val setting: MockApiSettingDto
) : LogMeowMessage()

@Serializable
@SerialName("mock_api_delete")
internal data class MockApiDeleteMessage(
    val id: String
) : LogMeowMessage()

@Serializable
@SerialName("mock_api_clear")
internal class MockApiClearMessage : LogMeowMessage()

@Serializable
@SerialName("clear_buffer")
internal class ClearBufferMessage : LogMeowMessage()

// ── Shared DTO ──

@Serializable
data class MockApiSettingDto(
    val id: String,
    val method: String,
    val url: String,
    val statusCode: Int = 200,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val delayMs: Long = 0
)
