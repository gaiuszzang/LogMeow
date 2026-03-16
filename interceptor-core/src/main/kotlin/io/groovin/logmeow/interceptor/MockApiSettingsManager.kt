package io.groovin.logmeow.interceptor

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

object MockApiSettingsManager {

    private const val PREFS_NAME = "logmeow_mock_api_settings"
    private const val KEY_SETTINGS = "mock_settings"

    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(MockApiSettingDto.serializer())

    private var prefs: SharedPreferences? = null
    private val settings = mutableListOf<MockApiSettingDto>()
    private val lock = Any()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        load()
    }

    fun initAndClear(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        synchronized(lock) { settings.clear() }
        prefs?.edit()?.remove(KEY_SETTINGS)?.commit()
    }

    private fun load() {
        val raw = prefs?.getString(KEY_SETTINGS, null) ?: return
        try {
            val loaded = json.decodeFromString(listSerializer, raw)
            synchronized(lock) {
                settings.clear()
                settings.addAll(loaded)
            }
        } catch (_: Exception) { }
    }

    private fun save() {
        if (LogMeow.mockSupportType == MockSupportType.DISABLED) return
        val data = synchronized(lock) { settings.toList() }
        try {
            val encoded = json.encodeToString(listSerializer, data)
            prefs?.edit()?.putString(KEY_SETTINGS, encoded)?.commit()
        } catch (_: Exception) { }
    }

    fun getAllSettings(): List<MockApiSettingDto> {
        return synchronized(lock) { settings.toList() }
    }

    fun addOrUpdate(setting: MockApiSettingDto) {
        if (LogMeow.mockSupportType == MockSupportType.DISABLED) return
        synchronized(lock) {
            val index = settings.indexOfFirst { it.id == setting.id }
            if (index >= 0) {
                settings[index] = setting
            } else {
                settings.add(setting)
            }
        }
        save()
    }

    fun delete(id: String) {
        if (LogMeow.mockSupportType == MockSupportType.DISABLED) return
        synchronized(lock) {
            settings.removeAll { it.id == id }
        }
        save()
    }

    fun clearAll() {
        synchronized(lock) {
            settings.clear()
        }
        save()
    }

    fun findMatch(method: String, url: String): MockApiSettingDto? {
        if (LogMeow.mockSupportType == MockSupportType.DISABLED) return null
        if (LogMeow.mockSupportType == MockSupportType.CONNECTED_ONLY && !LogMeowClient.isConnected) return null
        return synchronized(lock) {
            settings.find { it.method.equals(method, ignoreCase = true) && it.url == url }
        }
    }
}
