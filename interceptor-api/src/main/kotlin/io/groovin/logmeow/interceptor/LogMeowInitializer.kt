package io.groovin.logmeow.interceptor

interface LogMeowInitializer {
    fun initialize(port: Int, mockSupportType: MockSupportType)
}
