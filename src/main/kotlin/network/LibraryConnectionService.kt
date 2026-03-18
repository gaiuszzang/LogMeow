package network

import adb.AdbService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import network.data.MockApiSetting
import network.data.MockSupportType
import network.data.NetworkTrafficEntry
import network.data.ResponseType
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

class LibraryConnectionService(
    private val adbService: AdbService
) {
    companion object {
        const val DEFAULT_PORT = 10087
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    // Per-client data
    private val _clientTraffic = MutableStateFlow<Map<String, List<NetworkTrafficEntry>>>(emptyMap())
    val clientTraffic = _clientTraffic.asStateFlow()

    private val _clientMockSettings = MutableStateFlow<Map<String, List<MockApiSetting>>>(emptyMap())
    val clientMockSettings = _clientMockSettings.asStateFlow()

    private val _clientMockSupportType = MutableStateFlow<Map<String, MockSupportType>>(emptyMap())
    val clientMockSupportType = _clientMockSupportType.asStateFlow()

    private val _connectedApps = MutableStateFlow<Set<String>>(emptySet())
    val connectedApps = _connectedApps.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning = _isServerRunning.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val nextEntryId = AtomicInteger(0)
    private val mutex = Mutex()
    private var currentDeviceId: String? = null

    // Active client writers for sending messages back
    private val clientWriters = mutableMapOf<String, MutableList<BufferedWriter>>()
    private val clientSockets = mutableListOf<Socket>()
    private val writerLock = Any()

    suspend fun startServer(deviceId: String) {
        stopServer()
        currentDeviceId = deviceId
        serverJob = scope.launch {
            try {
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(DEFAULT_PORT))
                serverSocket = socket
                _isServerRunning.value = true

                adbService.setupAdbReverse(deviceId, DEFAULT_PORT, DEFAULT_PORT)

                while (isActive) {
                    try {
                        val client = socket.accept()
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (isActive) e.printStackTrace()
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isServerRunning.value = false
            }
        }
    }

    suspend fun stopServer() {
        withContext(Dispatchers.IO) {
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        serverSocket = null
        serverJob?.cancel()
        serverJob?.join()
        serverJob = null
        _isServerRunning.value = false
        _connectedApps.value = emptySet()
        synchronized(writerLock) {
            clientWriters.clear()
            clientSockets.forEach { try { it.close() } catch (_: Exception) {} }
            clientSockets.clear()
        }
        val deviceId = currentDeviceId
        if (deviceId != null) {
            currentDeviceId = null
            // Remove ADB reverse in background - don't block or rely on scope
            Thread {
                try { adbService.removeAdbReverseBlocking(deviceId) } catch (_: Exception) {}
            }.start()
        }
    }

    fun clearTraffic(appId: String) {
        scope.launch {
            mutex.withLock {
                _clientTraffic.update { it.toMutableMap().apply { remove(appId) } }
            }
        }
        sendToClient(appId, """{"type":"clear_buffer"}""")
    }

    fun sendMockApiAdd(appId: String, setting: MockApiSetting) {
        scope.launch {
            mutex.withLock {
                _clientMockSettings.update { map ->
                    val current = map[appId].orEmpty().toMutableList()
                    current.add(setting)
                    map + (appId to current)
                }
            }
        }
        val jsonStr = buildMockApiSettingJson(setting)
        sendToClient(appId, """{"type":"mock_api_add","setting":$jsonStr}""")
    }

    fun sendMockApiUpdate(appId: String, setting: MockApiSetting) {
        scope.launch {
            mutex.withLock {
                _clientMockSettings.update { map ->
                    val current = map[appId].orEmpty().toMutableList()
                    val index = current.indexOfFirst { it.id == setting.id }
                    if (index >= 0) current[index] = setting
                    map + (appId to current)
                }
            }
        }
        val jsonStr = buildMockApiSettingJson(setting)
        sendToClient(appId, """{"type":"mock_api_update","setting":$jsonStr}""")
    }

    fun sendMockApiDelete(appId: String, settingId: String) {
        scope.launch {
            mutex.withLock {
                _clientMockSettings.update { map ->
                    val current = map[appId].orEmpty().filter { it.id != settingId }
                    map + (appId to current)
                }
            }
        }
        sendToClient(appId, """{"type":"mock_api_delete","id":"$settingId"}""")
    }

    fun sendMockApiClear(appId: String) {
        scope.launch {
            mutex.withLock {
                _clientMockSettings.update { map ->
                    map + (appId to emptyList())
                }
            }
        }
        sendToClient(appId, """{"type":"mock_api_clear"}""")
    }

    fun onCleared() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        _isServerRunning.value = false
        _connectedApps.value = emptySet()
        synchronized(writerLock) {
            clientWriters.clear()
            clientSockets.forEach { try { it.close() } catch (_: Exception) {} }
            clientSockets.clear()
        }
        scope.cancel()
    }

    private fun sendToClient(appId: String, message: String) {
        val writers = synchronized(writerLock) { clientWriters[appId]?.toList() } ?: return
        scope.launch(Dispatchers.IO) {
            for (writer in writers) {
                try {
                    writer.write(message + "\n")
                    writer.flush()
                } catch (_: Exception) { }
            }
        }
    }

    private fun buildMockApiSettingJson(setting: MockApiSetting): String {
        val headers = setting.responseHeaders.entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\":\"${escapeJson(v)}\""
        }
        val body = setting.responseBody?.let { "\"${escapeJson(it)}\"" } ?: "null"
        return """{"id":"${escapeJson(setting.id)}","method":"${escapeJson(setting.method)}","url":"${escapeJson(setting.url)}","statusCode":${setting.statusCode},"responseHeaders":{$headers},"responseBody":$body,"delayMs":${setting.delayMs}}"""
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")

    private suspend fun handleClient(socket: Socket) {
        synchronized(writerLock) { clientSockets.add(socket) }
        withContext(Dispatchers.IO) {
            var appId = "unknown"
            var writer: BufferedWriter? = null
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, StandardCharsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(socket.outputStream, StandardCharsets.UTF_8))
                var isFirstLine = true
                reader.forEachLine { line ->
                    if (isFirstLine) {
                        isFirstLine = false
                        appId = parseHandshake(line) ?: "unknown"
                        _connectedApps.update { it + appId }
                        synchronized(writerLock) {
                            clientWriters.getOrPut(appId) { mutableListOf() }.add(writer)
                        }
                    } else {
                        parseTrafficJson(line, appId)?.let { entry ->
                            scope.launch {
                                mutex.withLock {
                                    _clientTraffic.update { map ->
                                        val current = map[appId].orEmpty()
                                        map + (appId to current + entry)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) println("Client disconnected: $appId (${e.message})")
            } finally {
                if (writer != null) {
                    synchronized(writerLock) {
                        clientWriters[appId]?.remove(writer)
                        if (clientWriters[appId]?.isEmpty() == true) {
                            clientWriters.remove(appId)
                        }
                    }
                }
                val hasMore = synchronized(writerLock) { clientWriters[appId]?.isNotEmpty() == true }
                if (!hasMore) {
                    _connectedApps.update { it - appId }
                    scope.launch {
                        mutex.withLock {
                            _clientTraffic.update { it - appId }
                            _clientMockSettings.update { it - appId }
                            _clientMockSupportType.update { it - appId }
                        }
                    }
                }
                synchronized(writerLock) { clientSockets.remove(socket) }
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun parseHandshake(jsonStr: String): String? = try {
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val appId = obj["appId"]?.jsonPrimitive?.contentOrNull

        // Parse mock support type
        val mockSupportTypeStr = obj["mockSupportType"]?.jsonPrimitive?.contentOrNull ?: "always"
        val mockSupportType = when (mockSupportTypeStr) {
            "connected_only" -> MockSupportType.CONNECTED_ONLY
            "disabled" -> MockSupportType.DISABLED
            else -> MockSupportType.ALWAYS
        }
        if (appId != null) {
            _clientMockSupportType.update { it + (appId to mockSupportType) }
        }

        // Parse mock API settings
        val mockSettings = obj["mockApiSettings"]?.jsonArray?.mapNotNull { element ->
            try {
                val settingObj = element.jsonObject
                MockApiSetting(
                    id = settingObj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                    method = settingObj["method"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                    url = settingObj["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                    statusCode = settingObj["statusCode"]?.jsonPrimitive?.intOrNull ?: 200,
                    responseHeaders = settingObj["responseHeaders"]?.jsonObject
                        ?.entries?.associate { it.key to (it.value.jsonPrimitive.contentOrNull ?: "") }
                        ?: emptyMap(),
                    responseBody = settingObj["responseBody"]?.jsonPrimitive?.contentOrNull,
                    delayMs = settingObj["delayMs"]?.jsonPrimitive?.longOrNull ?: 0
                )
            } catch (_: Exception) { null }
        } ?: emptyList()

        if (appId != null && mockSettings.isNotEmpty()) {
            scope.launch {
                mutex.withLock {
                    _clientMockSettings.update { map ->
                        map + (appId to mockSettings)
                    }
                }
            }
        }

        appId
    } catch (_: Exception) { null }

    private fun parseTrafficJson(jsonStr: String, appId: String): NetworkTrafficEntry? = try {
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val typeStr = obj["type"]?.jsonPrimitive?.contentOrNull
        if (typeStr != "traffic") null
        else {
            val responseTypeStr = obj["responseType"]?.jsonPrimitive?.contentOrNull
            val responseType = when (responseTypeStr) {
                "mock" -> ResponseType.MOCK
                else -> ResponseType.REAL
            }
            NetworkTrafficEntry(
                id = nextEntryId.getAndIncrement(),
                appId = appId,
                method = obj["method"]?.jsonPrimitive?.contentOrNull ?: "GET",
                url = obj["url"]?.jsonPrimitive?.contentOrNull ?: "",
                requestHeaders = obj["requestHeaders"]?.jsonObject
                    ?.entries?.associate { it.key to (it.value.jsonPrimitive.contentOrNull ?: "") }
                    ?: emptyMap(),
                requestBody = obj["requestBody"]?.jsonPrimitive?.contentOrNull,
                statusCode = obj["statusCode"]?.jsonPrimitive?.intOrNull,
                responseHeaders = obj["responseHeaders"]?.jsonObject
                    ?.entries?.associate { it.key to (it.value.jsonPrimitive.contentOrNull ?: "") }
                    ?: emptyMap(),
                responseBody = obj["responseBody"]?.jsonPrimitive?.contentOrNull,
                durationMs = obj["durationMs"]?.jsonPrimitive?.longOrNull,
                error = obj["error"]?.jsonPrimitive?.contentOrNull,
                responseType = responseType
            )
        }
    } catch (e: Exception) {
        println("JSON parse error: ${e.message}")
        null
    }
}
