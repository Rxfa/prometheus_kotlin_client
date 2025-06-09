package gauge
import io.github.kotlin.fibonacci.Counter
import io.github.kotlin.fibonacci.Gauge
import io.github.kotlin.fibonacci.gauge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.openjdk.jmh.annotations.*
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
    fun setup(){
        scope = CoroutineScope(Dispatchers.Default)
        gauge = gauge("test_gauge") {
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
            for (signal in decChannel){
                gauge.dec()
            }
        }

        scope.launch {
            for (signal in setChannel){
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
    fun incrementGauge(){
        incChannel.trySend(Unit)
    }

    @Benchmark
    fun decrementGauge(){
        decChannel.trySend(Unit)
    }

    @Benchmark
    fun setGauge(){
        setChannel.trySend(1.0)
    }

    @Benchmark
    fun getGauge(): Double{
        return gauge.get()
    }


}
