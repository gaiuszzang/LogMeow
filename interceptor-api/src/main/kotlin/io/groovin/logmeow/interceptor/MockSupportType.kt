package io.groovin.logmeow.interceptor

enum class MockSupportType {
    /** Save mock settings to SharedPreferences. Mocking works even without LogMeow connection. */
    ALWAYS,
    /** Save mock settings to SharedPreferences. Mocking only works while connected to LogMeow. */
    CONNECTED_ONLY,
    /** No mock support. Clears any saved settings. Mock API is disabled. */
    DISABLED
}
