package io.groovin.logmeow.interceptor

import android.content.Context

class CoreLogMeowInitializer(context: Context) : LogMeowInitializer {

    private val appContext: Context = context.applicationContext

    override fun initialize(port: Int, mockSupportType: MockSupportType) {
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
