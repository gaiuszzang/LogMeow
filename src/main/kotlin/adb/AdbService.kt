package adb

import adb.data.AdbDevice
import adb.data.AdbDeviceState
import adb.data.LogcatMessage
import adb.data.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

open class AdbService {

    companion object {
        private val ADB_COMMAND: String by lazy {
            findAdbPath() ?: "adb"
        }

        private val SCRCPY_COMMAND: String by lazy {
            findScrcpyPath() ?: "scrcpy"
        }

        private fun findAdbPath(): String? {
            // 1. ANDROID_HOME 환경변수 확인
            val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
            if (androidHome != null) {
                val adbPath = "$androidHome/platform-tools/adb"
                if (java.io.File(adbPath).exists()) {
                    println("Found ADB at: $adbPath")
                    return adbPath
                }
            }

            // 2. 일반적인 macOS 경로 확인
            val userHome = System.getProperty("user.home")
            val commonPaths = listOf(
                "$userHome/Library/Android/sdk/platform-tools/adb",
                "/usr/local/bin/adb",
                "/opt/homebrew/bin/adb"
            )

            for (path in commonPaths) {
                if (java.io.File(path).exists()) {
                    println("Found ADB at: $path")
                    return path
                }
            }

            // 3. PATH에서 찾기 시도
            try {
                val process = ProcessBuilder("which", "adb").start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val path = reader.readLine()?.trim()
                if (!path.isNullOrEmpty() && java.io.File(path).exists()) {
                    println("Found ADB in PATH: $path")
                    return path
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            println("ADB not found in common locations. Using 'adb' from PATH.")
            return null
        }

        private fun findScrcpyPath(): String? {
            // 1. Homebrew prefix로 찾기
            try {
                val brewProcess = ProcessBuilder("/bin/bash", "-c", "brew --prefix scrcpy 2>/dev/null").start()
                brewProcess.waitFor(5, TimeUnit.SECONDS)
                val reader = BufferedReader(InputStreamReader(brewProcess.inputStream))
                val brewPrefix = reader.readLine()?.trim()
                if (!brewPrefix.isNullOrEmpty()) {
                    val scrcpyPath = "$brewPrefix/bin/scrcpy"
                    if (java.io.File(scrcpyPath).exists()) {
                        println("Found scrcpy at: $scrcpyPath")
                        return scrcpyPath
                    }
                }
            } catch (e: Exception) {
                // Ignore and try next method
            }

            // 2. 일반적인 설치 경로 확인
            val userHome = System.getProperty("user.home")
            val commonPaths = listOf(
                "/opt/homebrew/bin/scrcpy",  // Apple Silicon Mac (Homebrew)
                "/usr/local/bin/scrcpy",     // Intel Mac (Homebrew)
                "/usr/bin/scrcpy",           // System path
                "$userHome/.local/bin/scrcpy" // User local install
            )

            for (path in commonPaths) {
                if (java.io.File(path).exists()) {
                    println("Found scrcpy at: $path")
                    return path
                }
            }

            // 3. PATH에서 찾기 시도 (셸을 통해 실행하여 환경변수 로드)
            try {
                val shell = System.getenv("SHELL") ?: "/bin/zsh"
                val process = ProcessBuilder(shell, "-l", "-c", "which scrcpy").start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val path = reader.readLine()?.trim()
                if (!path.isNullOrEmpty() && java.io.File(path).exists()) {
                    println("Found scrcpy in PATH: $path")
                    return path
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            println("scrcpy not found in common locations. Using 'scrcpy' from PATH.")
            return null
        }
    }

    private var nextLogId = 0L
    private var screenRecordProcess: Process? = null

    open fun getDevicesFlow(): Flow<List<AdbDevice>> = flow {
        while (true) {
            try {
                val process = ProcessBuilder(ADB_COMMAND, "devices").start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val devices = reader.readLines()
                    .drop(1) // "List of devices attached" 제외
                    .mapNotNull { line ->
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            AdbDevice(id = parts[0], state = AdbDeviceState.fromString(parts[1]))
                        } else {
                            null
                        }
                    }
                emit(devices)
            } catch (e: Exception) {
                // ADB 명령어를 실행할 수 없는 경우 빈 리스트 방출
                emit(emptyList())
                e.printStackTrace()
            }
            delay(2.seconds)
        }
    }.flowOn(Dispatchers.IO)

    open suspend fun clearLogcat(deviceId: String) {
        withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(ADB_COMMAND, "-s", deviceId, "logcat", "-c").start()
                process.waitFor(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    open suspend fun captureScreenshot(deviceId: String, outputPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val tempPath = "/sdcard/screenshot_temp.png"

                // 1. Take screenshot on device
                val screencapProcess = ProcessBuilder(ADB_COMMAND, "-s", deviceId, "shell", "screencap", "-p", tempPath).start()
                screencapProcess.waitFor(10, TimeUnit.SECONDS)

                // 2. Pull screenshot to local
                val pullProcess = ProcessBuilder(ADB_COMMAND, "-s", deviceId, "pull", tempPath, outputPath).start()
                pullProcess.waitFor(10, TimeUnit.SECONDS)

                // 3. Remove screenshot from device
                val rmProcess = ProcessBuilder(ADB_COMMAND, "-s", deviceId, "shell", "rm", tempPath).start()
                rmProcess.waitFor(5, TimeUnit.SECONDS)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    open fun startScreenRecording(deviceId: String): Boolean {
        return try {
            val videoFileName = "/sdcard/logmeow.mp4"
            screenRecordProcess = ProcessBuilder(
                ADB_COMMAND, "-s", deviceId, "shell", "screenrecord", videoFileName
            ).start()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    open suspend fun stopScreenRecording(deviceId: String, outputPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val videoFileName = "/sdcard/logmeow.mp4"

                // 1. Kill the recording process
                screenRecordProcess?.destroy()
                screenRecordProcess?.waitFor(5, TimeUnit.SECONDS)
                screenRecordProcess = null

                // 2. Wait for file to be saved
                delay(1000)

                // 3. Pull video to local
                val pullProcess = ProcessBuilder(
                    ADB_COMMAND, "-s", deviceId, "pull", videoFileName, outputPath
                ).start()
                pullProcess.waitFor(30, TimeUnit.SECONDS)

                // 4. Remove video from device
                val rmProcess = ProcessBuilder(
                    ADB_COMMAND, "-s", deviceId, "shell", "rm", videoFileName
                ).start()
                rmProcess.waitFor(5, TimeUnit.SECONDS)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                screenRecordProcess = null
                false
            }
        }
    }

    open suspend fun launchScrcpy(deviceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // scrcpy가 adb를 찾을 수 있도록 ADB 환경변수 설정
                val processBuilder = ProcessBuilder(SCRCPY_COMMAND, "-s", deviceId, "--legacy-paste")
                processBuilder.environment()["ADB"] = ADB_COMMAND
                processBuilder.start()

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    open suspend fun executeDeepLink(deviceId: String, scheme: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val shellCommand = "am start -a android.intent.action.VIEW -d \"$scheme\""
                val process = ProcessBuilder(
                    ADB_COMMAND, "-s", deviceId, "shell", shellCommand
                ).start()

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText().trim()
                process.waitFor(10, TimeUnit.SECONDS)

                output
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }
    }

    fun getLogcatFlow(deviceId: String): Flow<LogcatMessage> = callbackFlow<LogcatMessage> {
        val process = ProcessBuilder(ADB_COMMAND, "-s", deviceId, "logcat", "-v", "threadtime").start()
        val job = launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.useLines { lines ->
                    lines.forEach { line ->
                        parseLogcatLine(line)?.let {
                            trySend(it)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                close(e)
            }
        }

        awaitClose {
            job.cancel()
            process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
        }
    }.flowOn(Dispatchers.IO)


    private val logcatRegex = Regex(
        "(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\d+)\\s+(\\d+)\\s+([VDIWEFS])\\s+(.*?):\\s+(.*)"
    )

    private fun parseLogcatLine(line: String): LogcatMessage? {
        val match = logcatRegex.matchEntire(line) ?: return null
        val (timestamp, pid, tid, levelChar, tag, message) = match.destructured
        return LogcatMessage(
            id = nextLogId++,
            timestamp = timestamp,
            pid = pid.toIntOrNull() ?: 0,
            tid = tid.toIntOrNull() ?: 0,
            level = LogLevel.fromChar(levelChar.first()),
            tag = tag.trim(),
            message = message
        )
    }
}
