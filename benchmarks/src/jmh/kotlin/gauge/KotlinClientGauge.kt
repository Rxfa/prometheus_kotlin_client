package gauge

import io.github.rxfa.prometheus.core.Gauge
import io.github.rxfa.prometheus.core.gauge
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
open class KotlinClientGauge {
    private lateinit var gauge: Gauge
    private lateinit var scope: CoroutineScope
    private lateinit var incChannel: Channel<Unit>
    private lateinit var decChannel: Channel<Unit>
    private lateinit var setChannel: Channel<Double>

    @Setup
    fun setup() {
        scope = CoroutineScope(Dispatchers.Default)
        gauge =
            gauge("test_gauge") {
                help("Test gauge")
            }
        incChannel = Channel(capacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        decChannel = Channel(capacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        setChannel = Channel(capacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        scope.launch {
            for (signal in incChannel) {
                gauge.inc()
            }
        }
        scope.launch {
            for (signal in decChannel) {
                gauge.dec()
            }
        }

        scope.launch {
            for (signal in setChannel) {
                gauge.set(signal)
            }
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        incChannel.close()
        decChannel.close()
        setChannel.close()
        scope.cancel()
    }

    @Benchmark
    fun incrementGauge() {
        incChannel.trySend(Unit)
    }

    @Benchmark
    fun decrementGauge() {
        decChannel.trySend(Unit)
    }

    @Benchmark
    fun setGauge() {
        setChannel.trySend(1.0)
    }

    @Benchmark
    fun getGauge(): Double = gauge.get()
}
