package io.groovin.logmeow.interceptor

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

object LogMeow {

    const val DEFAULT_PORT = 10087

    var configuredPort: Int = DEFAULT_PORT
        private set
    var mockSupportType: MockSupportType = MockSupportType.ALWAYS
        private set

    private var initializer: LogMeowInitializer? = null
    private val initialized = AtomicBoolean(false)

    fun registerInitializer(initializer: LogMeowInitializer) {
        this.initializer = initializer
    }

    fun init(context: Context, port: Int = DEFAULT_PORT, mockSupportType: MockSupportType = MockSupportType.ALWAYS) {
        configuredPort = port
        this.mockSupportType = mockSupportType
        if (initialized.compareAndSet(false, true)) {
            initializer?.initialize(context, port, mockSupportType)
        }
    }
}
