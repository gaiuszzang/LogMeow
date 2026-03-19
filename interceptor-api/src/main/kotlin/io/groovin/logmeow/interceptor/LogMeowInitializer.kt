package io.groovin.logmeow.interceptor

import android.content.Context

interface LogMeowInitializer {
    fun initialize(context: Context, port: Int, mockSupportType: MockSupportType)
}
