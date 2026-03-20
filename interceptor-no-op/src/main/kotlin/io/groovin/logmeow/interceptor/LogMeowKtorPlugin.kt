package io.groovin.logmeow.interceptor

import android.content.Context
import io.ktor.client.plugins.api.*

class LogMeowKtorPluginConfig {
    lateinit var context: Context
    var port: Int = LogMeow.DEFAULT_PORT
    var mockSupportType: MockSupportType = MockSupportType.ALWAYS
}

/** No-op Ktor plugin — does nothing. Safe for release builds. */
val LogMeowPlugin = createClientPlugin("LogMeow", ::LogMeowKtorPluginConfig) {
    LogMeow.registerInitializer(NoOpLogMeowInitializer)
    LogMeow.init(pluginConfig.port, pluginConfig.mockSupportType)
}
