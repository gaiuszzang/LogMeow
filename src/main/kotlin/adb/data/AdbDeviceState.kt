package adb.data

enum class AdbDeviceState {
    DEVICE, OFFLINE, UNAUTHORIZED, UNKNOWN;

    companion object {
        fun fromString(state: String): AdbDeviceState {
            return when (state) {
                "device" -> DEVICE
                "offline" -> OFFLINE
                "unauthorized" -> UNAUTHORIZED
                else -> UNKNOWN
            }
        }
    }
}
