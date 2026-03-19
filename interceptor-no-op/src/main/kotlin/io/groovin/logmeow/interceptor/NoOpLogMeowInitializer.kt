package io.groovin.logmeow.interceptor

import android.content.Context

object NoOpLogMeowInitializer : LogMeowInitializer {
    override fun initialize(context: Context, port: Int, mockSupportType: MockSupportType) = Unit
}
