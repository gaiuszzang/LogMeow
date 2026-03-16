package io.groovin.logmeow.interceptor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope as childScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.BufferedWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

object LogMeowClient {

    private const val MAX_BUFFER_SIZE = 200
    private const val RECONNECT_DELAY_MS = 3_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }
    private val messageSerializer = serializer<LogMeowMessage>()

    private fun encode(message: LogMeowMessage): String =
        json.encodeToString(messageSerializer, message)

    // Ring buffer for traffic history
    private val trafficBuffer = ArrayDeque<TrafficMessage>(MAX_BUFFER_SIZE)
    private val bufferLock = Any()

    // Channel for notifying the writer that new traffic arrived
    private val newTrafficSignal = Channel<Unit>(Channel.CONFLATED)

    private val started = AtomicBoolean(false)

    @Volatile var appId: String = "unknown"
    @Volatile private var port: Int = LogMeow.DEFAULT_PORT
    @Volatile var isConnected: Boolean = false
        private set

    fun start(appId: String, port: Int) {
        this.appId = appId
        this.port = port
        if (started.compareAndSet(false, true)) {
            scope.launch { connectionLoop() }
        }
    }

    fun enqueue(message: TrafficMessage) {
        synchronized(bufferLock) {
            if (trafficBuffer.size >= MAX_BUFFER_SIZE) {
                trafficBuffer.removeFirst()
            }
            trafficBuffer.addLast(message)
        }
        newTrafficSignal.trySend(Unit)
    }

    fun clearBuffer() {
        synchronized(bufferLock) {
            trafficBuffer.clear()
        }
    }

    private fun getBufferSnapshot(): List<TrafficMessage> {
        return synchronized(bufferLock) {
            trafficBuffer.toList()
        }
    }

    private suspend fun connectionLoop() {
        while (scope.isActive) {
            try {
                connect()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Connection failed or dropped — retry after delay
            } finally {
                isConnected = false
            }
            delay(RECONNECT_DELAY_MS)
        }
    }

    private suspend fun connect() {
        val socket = Socket("localhost", port)
        try {
            val writer = socket.getOutputStream().bufferedWriter(StandardCharsets.UTF_8)
            val reader = socket.getInputStream().bufferedReader(StandardCharsets.UTF_8)

            // Handshake — include current mock API settings and mock support type
            val mockTypeStr = when (LogMeow.mockSupportType) {
                MockSupportType.ALWAYS -> "always"
                MockSupportType.CONNECTED_ONLY -> "connected_only"
                MockSupportType.DISABLED -> "disabled"
            }
            val mockSettings = MockApiSettingsManager.getAllSettings()
            writer.write(encode(HandshakeMessage(
                appId = appId,
                mockSupportType = mockTypeStr,
                mockApiSettings = mockSettings
            )) + "\n")
            writer.flush()

            isConnected = true

            // Flush entire buffer history
            var sentCount = flushBuffer(writer)

            childScope {
                // Writer: sends new traffic as it arrives
                val writerJob = launch {
                    while (isActive) {
                        newTrafficSignal.receive()
                        val snapshot = getBufferSnapshot()
                        // Only send items we haven't sent yet
                        val newItems = snapshot.drop(sentCount)
                        for (item in newItems) {
                            writer.write(encode(item) + "\n")
                        }
                        if (newItems.isNotEmpty()) {
                            writer.flush()
                        }
                        sentCount = snapshot.size
                    }
                }
                // Reader: receives messages from LogMeow server
                launch(Dispatchers.IO) {
                    try {
                        reader.forEachLine { line ->
                            runCatching { json.decodeFromString(messageSerializer, line) }
                                .getOrNull()
                                ?.let { handleIncoming(it) }
                        }
                    } finally {
                        // Close socket to unblock writer if it's stuck in write/flush
                        try { socket.close() } catch (_: Exception) {}
                        writerJob.cancel()
                    }
                }
            }
        } finally {
            isConnected = false
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun flushBuffer(writer: BufferedWriter): Int {
        val snapshot = getBufferSnapshot()
        for (msg in snapshot) {
            writer.write(encode(msg) + "\n")
        }
        if (snapshot.isNotEmpty()) {
            writer.flush()
        }
        return snapshot.size
    }

    private fun handleIncoming(message: LogMeowMessage) {
        when (message) {
            is MockApiAddMessage -> MockApiSettingsManager.addOrUpdate(message.setting)
            is MockApiUpdateMessage -> MockApiSettingsManager.addOrUpdate(message.setting)
            is MockApiDeleteMessage -> MockApiSettingsManager.delete(message.id)
            is MockApiClearMessage -> MockApiSettingsManager.clearAll()
            is ClearBufferMessage -> clearBuffer()
            else -> Unit
        }
    }
}
