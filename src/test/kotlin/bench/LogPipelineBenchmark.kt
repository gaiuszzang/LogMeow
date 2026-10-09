package bench

import data.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import support.LogSource
import support.generateLogs
import support.installTestMainDispatcher
import support.streamingViewModel
import support.uninstallTestMainDispatcher
import vm.MainViewModel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Wall-clock micro-benchmarks for the log pipeline. No device and no Compose involved.
 *
 * Opt-in so the regular test run stays fast:
 *
 *     ./gradlew test -Dbenchmark=true --tests 'bench.*'
 *     ./gradlew test -Dbenchmark=true -Dbenchmark.sizes=10000,50000,100000,200000 --tests 'bench.*'
 *
 * Numbers are only meaningful as before/after ratios — absolute values move with
 * JIT warm-up and GC.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogPipelineBenchmark {

    private val enabled = System.getProperty("benchmark") == "true"
    private val sizes = (System.getProperty("benchmark.sizes") ?: "10000,50000")
        .split(",").map { it.trim().toInt() }

    @AfterTest
    fun tearDown() = uninstallTestMainDispatcher()

    @Test
    fun `inject N logs`() = bench { size ->
        val source = LogSource()
        val viewModel = streamingViewModel(source)
        val logs = generateLogs(size)

        val nanos = measure {
            source.emit(logs)
            advanceUntilIdle()
        }
        report("inject", size, nanos)
        viewModel.onCleared()
    }

    @Test
    fun `append one log past the trim threshold`() = bench { size ->
        val source = LogSource()
        val viewModel = streamingViewModel(source, AppSettings(maxLogCount = size))
        source.emit(generateLogs(size))
        advanceUntilIdle()

        // buffer is exactly full; every further line now hits the trim path
        val extra = generateLogs(200, startId = size.toLong())
        val nanos = measure {
            source.emit(extra)
            advanceUntilIdle()
        }
        report("trim-append (per log)", size, nanos / 200)
        viewModel.onCleared()
    }

    @Test
    fun `select a single log`() = bench { size ->
        val viewModel = loaded(size)
        val mid = (size / 2).toLong()
        var n = 0L

        val nanos = measureMedian { viewModel.selectSingleLog(mid + (n++ % 32)) }

        report("selectSingleLog", size, nanos)
        viewModel.onCleared()
    }

    @Test
    fun `select a range of logs`() = bench { size ->
        val viewModel = loaded(size)
        val mid = (size / 2).toLong()
        viewModel.selectSingleLog(mid)
        var n = 0L

        val nanos = measureMedian { viewModel.selectRangeLog(mid + 100 + (n++ % 32)) }

        report("selectRangeLog", size, nanos)
        viewModel.onCleared()
    }

    @Test
    fun `apply a message filter`() = bench { size ->
        val viewModel = loaded(size)
        var n = 0

        val nanos = measureMedian { viewModel.updateMessageFilter("body ${n++ % 32}") }

        report("updateMessageFilter", size, nanos)
        viewModel.onCleared()
    }

    // --- harness ---------------------------------------------------------------

    private fun bench(body: suspend TestScope.(Int) -> Unit) {
        if (!enabled) {
            println("[bench] skipped (pass -Dbenchmark=true to run)")
            return
        }
        sizes.forEach { size ->
            runTest(timeout = 30.minutes) {
                installTestMainDispatcher(this)
                body(size)
            }
        }
    }

    private fun TestScope.loaded(size: Int): MainViewModel {
        val source = LogSource()
        val viewModel = streamingViewModel(source)
        source.emit(generateLogs(size))
        advanceUntilIdle()
        return viewModel
    }

    private inline fun measure(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return System.nanoTime() - start
    }

    /**
     * Median of [iterations] timed runs after [warmup] untimed ones. Single-shot
     * timings here are dominated by JIT warm-up and GC pauses — an earlier version
     * of this benchmark reported 10k as slower than 50k for the same operation.
     */
    private inline fun measureMedian(
        warmup: Int = 20,
        iterations: Int = 21,
        block: () -> Unit
    ): Long {
        repeat(warmup) { block() }
        val samples = LongArray(iterations) { measure(block) }
        samples.sort()
        return samples[iterations / 2]
    }

    private fun report(label: String, size: Int, nanos: Long) {
        val ms = nanos / 1_000_000.0
        println("[bench] %-24s size=%-7d %10.3f ms".format(label, size, ms))
    }
}
