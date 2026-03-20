package io.groovin.logmeow.interceptor

object NoOpLogMeowInitializer : LogMeowInitializer {
    override fun initialize(port: Int, mockSupportType: MockSupportType) = Unit
}
