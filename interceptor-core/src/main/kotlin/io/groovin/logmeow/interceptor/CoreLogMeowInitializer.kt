package io.groovin.logmeow.interceptor

import android.content.Context

class CoreLogMeowInitializer : LogMeowInitializer {
    override fun initialize(context: Context, port: Int, mockSupportType: MockSupportType) {
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
