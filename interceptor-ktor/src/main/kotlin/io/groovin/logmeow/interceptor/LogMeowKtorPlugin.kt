package io.groovin.logmeow.interceptor

import android.content.Context
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*

class LogMeowKtorPluginConfig {
    lateinit var context: Context
    var port: Int = LogMeow.DEFAULT_PORT
    var mockSupportType: MockSupportType = MockSupportType.ALWAYS
}

private val StartTimeKey = AttributeKey<Long>("LogMeowStartTime")
private val RequestInfoKey = AttributeKey<RequestCaptureInfo>("LogMeowRequestInfo")

private data class RequestCaptureInfo(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String?
)

private const val MAX_BODY_SIZE = 32_768

val LogMeowPlugin = createClientPlugin("LogMeow", ::LogMeowKtorPluginConfig) {
    LogMeow.init(pluginConfig.context, pluginConfig.port, pluginConfig.mockSupportType)

    onRequest { request, content ->
        request.attributes.put(StartTimeKey, System.currentTimeMillis())

        val headers = request.headers.build().entries()
            .associate { (key, values) -> key to values.joinToString(", ") }
        val body = when (content) {
            is OutgoingContent.ByteArrayContent -> {
                val ct = content.contentType
                if (ct == null || ct.contentType in listOf("text", "application")) {
                    content.bytes().decodeToString().take(MAX_BODY_SIZE)
                } else {
                    "[Binary: $ct, ${content.contentLength} bytes]"
                }
            }
            else -> null
        }
        request.attributes.put(RequestInfoKey, RequestCaptureInfo(
            method = request.method.value,
            url = request.url.buildString(),
            headers = headers,
            body = body
        ))
    }

    onResponse { response ->
        val startTime = response.call.attributes.getOrNull(StartTimeKey) ?: return@onResponse
        val reqInfo = response.call.attributes.getOrNull(RequestInfoKey) ?: return@onResponse
        val durationMs = System.currentTimeMillis() - startTime

        val respHeaders = response.headers.entries()
            .associate { (key, values) -> key to values.joinToString(", ") }

        val respBody = try {
            val ct = response.contentType()
            if (ct == null || ct.contentType in listOf("text", "application")) {
                response.bodyAsText().take(MAX_BODY_SIZE)
            } else {
                val len = response.headers[HttpHeaders.ContentLength]
                "[Binary: $ct, $len bytes]"
            }
        } catch (_: Exception) {
            null
        }

        LogMeowClient.enqueue(TrafficMessage(
            method = reqInfo.method,
            url = reqInfo.url,
            statusCode = response.status.value,
            durationMs = durationMs,
            requestHeaders = reqInfo.headers,
            requestBody = reqInfo.body,
            responseHeaders = respHeaders,
            responseBody = respBody,
            responseType = ResponseType.REAL
        ))
    }
}
