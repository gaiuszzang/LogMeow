package io.groovin.logmeow.interceptor

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.coroutines.CoroutineContext

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
    LogMeow.registerInitializer(CoreLogMeowInitializer())
    LogMeow.init(pluginConfig.context, pluginConfig.port, pluginConfig.mockSupportType)
    val ktorClient = client

    on(Send) { request ->
        val startTime = System.currentTimeMillis()
        val method = request.method.value
        val url = request.url.buildString()
        val reqHeaders = request.headers.build().entries()
            .associate { (key, values) -> key to values.joinToString(", ") }
        val body = when (val content = request.body) {
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

        // Check for mock match
        val mockSetting = MockApiSettingsManager.findMatch(method, url)
        if (mockSetting != null) {
            if (mockSetting.delayMs > 0) {
                delay(mockSetting.delayMs)
            }
            val durationMs = System.currentTimeMillis() - startTime
            LogMeowClient.enqueue(TrafficMessage(
                method = method, url = url,
                requestHeaders = reqHeaders, requestBody = body,
                statusCode = mockSetting.statusCode,
                durationMs = durationMs,
                responseHeaders = mockSetting.responseHeaders,
                responseBody = mockSetting.responseBody,
                responseType = ResponseType.MOCK
            ))
            return@on createMockCall(ktorClient, request, mockSetting, startTime)
        }

        // Real request
        request.attributes.put(StartTimeKey, startTime)
        request.attributes.put(RequestInfoKey, RequestCaptureInfo(method, url, reqHeaders, body))
        proceed(request)
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
            method = reqInfo.method, url = reqInfo.url,
            statusCode = response.status.value,
            durationMs = durationMs,
            requestHeaders = reqInfo.headers, requestBody = reqInfo.body,
            responseHeaders = respHeaders, responseBody = respBody,
            responseType = ResponseType.REAL
        ))
    }
}

@OptIn(InternalAPI::class)
private fun createMockCall(
    client: HttpClient,
    request: HttpRequestBuilder,
    mockSetting: MockApiSettingDto,
    startTime: Long
): HttpClientCall {
    val callJob = Job(client.coroutineContext[Job])
    val callContext: CoroutineContext = client.coroutineContext + callJob

    val responseHeaders = Headers.build {
        mockSetting.responseHeaders.forEach { (k, v) -> append(k, v) }
    }

    val requestData = HttpRequestData(
        url = Url(request.url.buildString()),
        method = request.method,
        headers = request.headers.build(),
        body = request.body as? OutgoingContent ?: TextContent("", ContentType.Application.Json),
        executionContext = callJob,
        attributes = Attributes()
    )

    val responseData = HttpResponseData(
        statusCode = HttpStatusCode.fromValue(mockSetting.statusCode),
        requestTime = GMTDate(startTime),
        headers = responseHeaders,
        version = HttpProtocolVersion.HTTP_1_1,
        body = ByteReadChannel((mockSetting.responseBody ?: "").toByteArray()),
        callContext = callContext
    )

    return HttpClientCall(client, requestData, responseData)
}
