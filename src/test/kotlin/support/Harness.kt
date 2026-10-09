package support

import adb.data.LogcatMessage
import data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import vm.MainViewModel

/**
 * [MainViewModel] creates its own scope on `Dispatchers.Main.immediate`, so tests
 * must install a test dispatcher before constructing it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun installTestMainDispatcher(scope: TestScope) {
    Dispatchers.setMain(StandardTestDispatcher(scope.testScheduler))
}

@OptIn(ExperimentalCoroutinesApi::class)
fun uninstallTestMainDispatcher() {
    Dispatchers.resetMain()
}

/**
 * Builds a [MainViewModel], lets it auto-select the fake device, then streams
 * [logs] through the logcat pipeline and waits until they are all applied.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.loadedViewModel(
    logs: List<LogcatMessage>,
    settings: AppSettings = AppSettings()
): MainViewModel {
    val viewModel = MainViewModel(
        adbService = FakeAdbService(logcat = logs.asFlow()),
        repository = FakeRepository(settings)
    )
    advanceUntilIdle() // device list collected, first device auto-selected
    viewModel.toggleLogging()
    advanceUntilIdle() // logcat flow drained
    return viewModel
}

/**
 * A logcat stream the test drives by hand, for scenarios that need logs to arrive
 * in more than one batch (buffer trimming, live-append behaviour).
 */
class LogSource {
    val flow = MutableSharedFlow<LogcatMessage>(extraBufferCapacity = Int.MAX_VALUE)

    fun emit(logs: List<LogcatMessage>) {
        logs.forEach { check(flow.tryEmit(it)) { "log buffer overflow" } }
    }
}

/** Builds a view model wired to [source] and already collecting from it. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.streamingViewModel(
    source: LogSource,
    settings: AppSettings = AppSettings()
): MainViewModel {
    val viewModel = MainViewModel(
        adbService = FakeAdbService(logcat = source.flow),
        repository = FakeRepository(settings)
    )
    advanceUntilIdle()
    viewModel.toggleLogging()
    advanceUntilIdle() // collector is subscribed before the test emits anything
    return viewModel
}
