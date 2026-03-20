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
import io.groovin.logmeow.interceptor.ClearBufferMessage
import io.groovin.logmeow.interceptor.HandshakeMessage
import io.groovin.logmeow.interceptor.LogMeowJson
import io.groovin.logmeow.interceptor.LogMeowMessage
import io.groovin.logmeow.interceptor.MockApiAddMessage
import io.groovin.logmeow.interceptor.MockApiClearMessage
import io.groovin.logmeow.interceptor.MockApiDeleteMessage
import io.groovin.logmeow.interceptor.MockApiSettingDto
import io.groovin.logmeow.interceptor.MockApiUpdateMessage
import io.groovin.logmeow.interceptor.MockSupportType
import io.groovin.logmeow.interceptor.TrafficMessage
import network.data.NetworkTrafficEntry
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

    // Per-client data
    private val _clientTraffic = MutableStateFlow<Map<String, List<NetworkTrafficEntry>>>(emptyMap())
    val clientTraffic = _clientTraffic.asStateFlow()

    private val _clientMockSettings = MutableStateFlow<Map<String, List<MockApiSettingDto>>>(emptyMap())
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
            try { adbService.removeAdbReverse(deviceId) } catch (_: Exception) {}
        }
    }

    fun clearTraffic(appId: String) {
        scope.launch {
            mutex.withLock {
                _clientTraffic.update { it.toMutableMap().apply { remove(appId) } }
            }
        }
        sendToClient(appId, encodeMessage(ClearBufferMessage()))
    }

    fun sendMockApiAdd(appId: String, setting: MockApiSettingDto) {
        scope.launch {
            mutex.withLock {
                _clientMockSettings.update { map ->
                    val current = map[appId].orEmpty().toMutableList()
                    current.add(setting)
                    map + (appId to current)
                }
            }
        }
        sendToClient(appId, encodeMessage(MockApiAddMessage(setting)))
    }

    fun sendMockApiUpdate(appId: String, setting: MockApiSettingDto) {
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
        sendToClient(appId, encodeMessage(MockApiUpdateMessage(setting)))
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
        sendToClient(appId, encodeMessage(MockApiDeleteMessage(settingId)))
    }

    fun sendMockApiClear(appId: String) {
        scope.launch {
            mutex.withLock {
                _clientMockSettings.update { map ->
                    map + (appId to emptyList())
                }
            }
        }
        sendToClient(appId, encodeMessage(MockApiClearMessage()))
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

    private fun encodeMessage(message: LogMeowMessage): String =
        LogMeowJson.encodeToString(LogMeowMessage.serializer(), message)

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
        val handshake = LogMeowJson.decodeFromString<HandshakeMessage>(jsonStr)
        val appId = handshake.appId

        val mockSupportType = when (handshake.mockSupportType) {
            "connected_only" -> MockSupportType.CONNECTED_ONLY
            "disabled" -> MockSupportType.DISABLED
            else -> MockSupportType.ALWAYS
        }
        _clientMockSupportType.update { it + (appId to mockSupportType) }

        if (handshake.mockApiSettings.isNotEmpty()) {
            scope.launch {
                mutex.withLock {
                    _clientMockSettings.update { map ->
                        map + (appId to handshake.mockApiSettings)
                    }
                }
            }
        }

        appId
    } catch (_: Exception) { null }

    private fun parseTrafficJson(jsonStr: String, appId: String): NetworkTrafficEntry? = try {
        val message = LogMeowJson.decodeFromString<LogMeowMessage>(jsonStr)
        if (message is TrafficMessage) {
            NetworkTrafficEntry(
                id = nextEntryId.getAndIncrement(),
                appId = appId,
                method = message.method,
                url = message.url,
                requestHeaders = message.requestHeaders,
                requestBody = message.requestBody,
                statusCode = message.statusCode,
                responseHeaders = message.responseHeaders,
                responseBody = message.responseBody,
                durationMs = message.durationMs,
                error = message.error,
                responseType = message.responseType
            )
        } else null
    } catch (e: Exception) {
        println("JSON parse error: ${e.message}")
        null
    }
}
