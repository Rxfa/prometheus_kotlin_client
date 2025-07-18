package summary

import io.github.rxfa.prometheus.core.Summary
import io.github.rxfa.prometheus.core.quantile
import io.github.rxfa.prometheus.core.quantiles
import io.github.rxfa.prometheus.core.summaryQuantiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
open class KotlinClientSummary {
    private lateinit var summary: Summary
    private lateinit var scope: CoroutineScope
    private lateinit var observeChannel: Channel<Double>

    @Setup(Level.Trial)
    fun setup() {
        scope = CoroutineScope(Dispatchers.Default)

        // Setup with quantiles
        val q =
            quantiles(
                quantile(0.5, 0.05),
                quantile(0.95, 0.01),
            )

        summary =
            summaryQuantiles(
                name = "summary_benchmark",
                block = {},
                quantiles = q,
                maxAgeSeconds = 60,
                ageBuckets = 5,
            )

        // Channel to simulate high-throughput observation
        observeChannel = Channel(capacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        scope.launch {
            val child = summary.labels()
            for (value in observeChannel) {
                child.observe(value)
            }
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        observeChannel.close()
        scope.cancel()
    }

    @Benchmark
    fun observeSummary() {
        observeChannel.trySend(Math.random() * 10)
    }
}
