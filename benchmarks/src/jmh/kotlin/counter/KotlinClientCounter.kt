package counter

import io.github.rxfa.prometheus.core.Counter
import io.github.rxfa.prometheus.core.counter
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
open class KotlinClientCounter {
    private lateinit var counter: Counter
    private lateinit var scope: CoroutineScope
    private lateinit var incChannel: Channel<Unit>

    @Setup(Level.Trial)
    fun setup() {
        scope = CoroutineScope(Dispatchers.Default)

        counter =
            counter("test_total") {
                help("Test counter")
            }
        incChannel = Channel(capacity = 1024, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        scope.launch {
            for (signal in incChannel) {
                counter.inc()
            }
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        incChannel.close()
        scope.cancel()
    }

    @Benchmark
    fun incrementCounter() {
        incChannel.trySend(Unit)
    }

    @Benchmark
    fun getCounterValue(): Double = counter.get()
}
