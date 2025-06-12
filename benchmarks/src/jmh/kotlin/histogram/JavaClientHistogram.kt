package histogram;

import io.prometheus.client.Histogram
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
open class JavaClientHistogram {

    private lateinit var histogram: Histogram
    private lateinit var scope: CoroutineScope
    private lateinit var observeChannel: Channel<Double>

    @Setup(Level.Trial)
    fun setup() {
        scope = CoroutineScope(Dispatchers.Default)

        histogram =  Histogram.build()
            .name("test_histogram")
            .help("Test histogram")
            .buckets(0.1, 1.0, 2.5, 5.0, 10.0)
            .register();
        observeChannel = Channel(capacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        scope.launch {
            for (value in observeChannel) {
                histogram.observe(value)
            }
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        observeChannel.close()
        scope.cancel()
    }

    @Benchmark
    fun observeHistogram() {
        observeChannel.trySend(Math.random() * 10)
    }




}
