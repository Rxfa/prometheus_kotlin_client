package summary

import io.prometheus.client.Summary
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
open class JavaClientSummary {

    private lateinit var summary: Summary
    private lateinit var scope: CoroutineScope
    private lateinit var observeChannel: Channel<Double>

    @Setup(Level.Trial)
    fun setup() {
        scope = CoroutineScope(Dispatchers.Default)

        summary = Summary.build()
            .name("test_summary")
            .help("Test summary")
            .quantile(0.5, 0.05)
            .quantile(0.9, 0.01)
            .maxAgeSeconds(60)
            .ageBuckets(5)
            .register()

        observeChannel = Channel(capacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        scope.launch {
            val child = summary.labels() // Default child (no labels)
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
