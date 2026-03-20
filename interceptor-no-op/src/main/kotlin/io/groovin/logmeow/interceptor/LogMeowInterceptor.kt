package io.groovin.logmeow.interceptor

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/** No-op interceptor — passes requests through unchanged. Safe for release builds. */
@Suppress("UNUSED_PARAMETER")
class LogMeowInterceptor @JvmOverloads constructor(
    context: Context,
    mockSupportType: MockSupportType = MockSupportType.ALWAYS,
    port: Int = LogMeow.DEFAULT_PORT
) : Interceptor {
    init {
        LogMeow.registerInitializer(NoOpLogMeowInitializer)
        LogMeow.init(port, mockSupportType)
    }
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
