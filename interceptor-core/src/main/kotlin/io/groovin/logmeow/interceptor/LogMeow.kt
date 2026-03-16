package io.groovin.logmeow.interceptor

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

enum class MockSupportType {
    /** Save mock settings to SharedPreferences. Mocking works even without LogMeow connection. */
    ALWAYS,
    /** Save mock settings to SharedPreferences. Mocking only works while connected to LogMeow. */
    CONNECTED_ONLY,
    /** No mock support. Clears any saved settings. Mock API is disabled. */
    DISABLED
}

object LogMeow {

    const val DEFAULT_PORT = 10087

    internal var configuredPort: Int = DEFAULT_PORT
    internal var mockSupportType: MockSupportType = MockSupportType.ALWAYS
    private val initialized = AtomicBoolean(false)

    fun init(context: Context, port: Int = DEFAULT_PORT, mockSupportType: MockSupportType = MockSupportType.ALWAYS) {
        configuredPort = port
        this.mockSupportType = mockSupportType
        if (initialized.compareAndSet(false, true)) {
            val appContext = context.applicationContext
            when (mockSupportType) {
                MockSupportType.ALWAYS, MockSupportType.CONNECTED_ONLY -> {
                    MockApiSettingsManager.init(appContext)
                }
                MockSupportType.DISABLED -> {
                    MockApiSettingsManager.initAndClear(appContext)
                }
            }
            LogMeowClient.start(appContext.packageName, port)
        }
    }
}
