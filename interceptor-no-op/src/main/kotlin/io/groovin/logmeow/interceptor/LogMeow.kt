package io.groovin.logmeow.interceptor

import android.content.Context

enum class MockSupportType {
    ALWAYS, CONNECTED_ONLY, DISABLED
}

object LogMeow {
    const val DEFAULT_PORT = 10087

    @Suppress("UNUSED_PARAMETER")
    fun init(context: Context, port: Int = DEFAULT_PORT, mockSupportType: MockSupportType = MockSupportType.ALWAYS) = Unit
}
