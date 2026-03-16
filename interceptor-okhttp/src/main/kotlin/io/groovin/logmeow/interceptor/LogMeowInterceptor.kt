package io.groovin.logmeow.interceptor

import android.content.Context
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

class LogMeowInterceptor @JvmOverloads constructor(
    context: Context,
    mockSupportType: MockSupportType = MockSupportType.ALWAYS,
    port: Int = LogMeow.DEFAULT_PORT
) : Interceptor {

    init {
        LogMeow.init(context, port, mockSupportType)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.method
        val url = request.url.toString()
        val requestHeaders = headersToMap(request.headers)
        val requestBody = readRequestBody(request)

        val startTime = System.currentTimeMillis()

        // Check for mock match
        val mockSetting = MockApiSettingsManager.findMatch(method, url)
        if (mockSetting != null) {
            if (mockSetting.delayMs > 0) {
                Thread.sleep(mockSetting.delayMs)
            }
            val mockResponse = buildMockResponse(chain, mockSetting)
            val durationMs = System.currentTimeMillis() - startTime
            LogMeowClient.enqueue(TrafficMessage(
                method = method, url = url,
                requestHeaders = requestHeaders, requestBody = requestBody,
                statusCode = mockSetting.statusCode,
                durationMs = durationMs,
                responseHeaders = mockSetting.responseHeaders,
                responseBody = mockSetting.responseBody,
                responseType = ResponseType.MOCK
            ))
            return mockResponse
        }

        // Real request
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            LogMeowClient.enqueue(TrafficMessage(
                method = method, url = url,
                requestHeaders = requestHeaders, requestBody = requestBody,
                statusCode = -1, durationMs = System.currentTimeMillis() - startTime,
                error = e.message,
                responseType = ResponseType.REAL
            ))
            throw e
        }

        LogMeowClient.enqueue(TrafficMessage(
            method = method, url = url,
            requestHeaders = requestHeaders, requestBody = requestBody,
            statusCode = response.code, durationMs = System.currentTimeMillis() - startTime,
            responseHeaders = headersToMap(response.headers),
            responseBody = readResponseBody(response),
            responseType = ResponseType.REAL
        ))
        return response
    }

    private fun buildMockResponse(chain: Interceptor.Chain, setting: MockApiSettingDto): Response {
        val headersBuilder = Headers.Builder()
        setting.responseHeaders.forEach { (k, v) -> headersBuilder.add(k, v) }

        val contentType = setting.responseHeaders["Content-Type"] ?: "application/json"
        val body = (setting.responseBody ?: "").toResponseBody(contentType.toMediaTypeOrNull())

        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(setting.statusCode)
            .message("Mock")
            .headers(headersBuilder.build())
            .body(body)
            .build()
    }

    private fun readRequestBody(request: okhttp3.Request): String? {
        return try {
            val body = request.body ?: return null
            if (body.isOneShot()) return "[one-shot body]"
            val contentType = body.contentType()
            if (contentType != null && contentType.type != "text" && contentType.type != "application") {
                return "[Binary: $contentType, ${body.contentLength()} bytes]"
            }
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = contentType?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            buffer.readString(charset).truncate()
        } catch (_: Exception) {
            null
        }
    }

    private fun readResponseBody(response: Response): String? {
        return try {
            val contentType = response.body?.contentType()
            if (contentType != null && contentType.type != "text" && contentType.type != "application") {
                val len = response.body?.contentLength() ?: -1
                return "[Binary: $contentType, $len bytes]"
            }
            response.peekBody(MAX_BODY_BYTES)?.string()?.truncate()
        } catch (_: Exception) {
            null
        }
    }

    private fun String.truncate(): String =
        if (length > MAX_BODY_SIZE) substring(0, MAX_BODY_SIZE) + "...(truncated)" else this

    private fun headersToMap(headers: Headers): Map<String, String> =
        (0 until headers.size).associate { headers.name(it) to headers.value(it) }

    companion object {
        private const val MAX_BODY_SIZE = 32_768
        private const val MAX_BODY_BYTES = 32_768L
    }
}
