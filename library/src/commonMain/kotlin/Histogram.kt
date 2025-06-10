package io.github.kotlin.fibonacci

import io.github.kotlin.fibonacci.exemplars.Exemplar
import io.github.kotlin.fibonacci.exemplars.HistogramExemplarSample
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.Runnable
import kotlinx.datetime.Clock
import kotlin.math.pow

public fun histogram(
    name: String,
    block: HistogramBuilder.() -> Unit
): Histogram {
    return HistogramBuilder(name).apply(block).build()
}

public fun histogramBuckets(
    name:String,
    block: HistogramBuilder.() -> Unit,
    buckets: List<Double>
): Histogram {
    return HistogramBuilder(name,buckets).apply(block).build()
}

public fun linearHistogramBuckets(
    name: String,
    block: HistogramBuilder.() -> Unit,
    start: Double,
    width: Double,
    count: Int
): Histogram {
    val buckets =(0 until count).map { start + it * width }
    return HistogramBuilder(name,buckets).apply(block).build()
}

public fun exponentialHistogramBuckets(
    name:String,
    block: HistogramBuilder.() -> Unit,
    start: Double,
    factor: Double,
    count: Int
): Histogram {
    val buckets= (0 until count).map { start * factor.pow(it.toDouble()) }
    return HistogramBuilder(name, buckets).apply(block).build()
}

public data class Value(
    public val sum: Double,
    public val buckets: List<Double>,
    public val created: Long
)


public class Histogram internal constructor(
    fullName: String,
    help: String,
    labelNames: List<String> = emptyList(),
    unit: String = "",
    includeCreatedSeries: Boolean = false,
    buckets: List<Double>
):SimpleCollector<Histogram.Child>(fullName, help, labelNames, unit) {

    override val suffixes: Set<String> = setOf()
    override val name: String = fullName
    override val type: Type = Type.HISTOGRAM

    private var sortedBuckets: List<Double> = buckets
    private var examplersEnabled: Boolean = false
    private var exemplarSampler: HistogramExemplarSample? = null

    override fun newChild(): Child {
        return Child(sortedBuckets,examplersEnabled,exemplarSampler)
    }

    init {
        for (i in 0.. buckets.size - 2) {
            if (buckets[i] >= buckets[i + 1]) {
                throw IllegalStateException("Histogram buckets must be in increasing order: "
                        + buckets[i] + " >= " + buckets[i + 1]);
            }
        }
        if (buckets.isEmpty()) {
            throw IllegalStateException("Histogram must have at least one bucket.");
        }
        for (label in labelNames) {
            if (label.equals("le")) {
                throw IllegalStateException ("Histogram cannot have a label named 'le'.");
            }
        }
        if (buckets[buckets.size - 1] != Double.POSITIVE_INFINITY) {
            sortedBuckets += Double.POSITIVE_INFINITY
        }
    }

    public class Timer: AutoCloseable{

        private val child: Child
        private val start: Long

        private val simpleTimer = SimpleTimer()

        public constructor(child: Histogram.Child,start: Long) {
            this.child = child
            this.start = start
        }

        public fun observeDuration():Double{
            return observeDurationWithExemplar(listOf())
        }

        public fun observeDurationWithExemplar(exemplarLabels: List<String>): Double {
            val elapsed = simpleTimer.elapsedSecondsFromNanos(start, simpleTimer.defaultTimeProvider.milliTime)
            child.observeWithExemplar(elapsed, exemplarLabels)
            return elapsed
        }

        public override fun close() {
            observeDuration()
        }

    }

    public inner class Child {
        private val bucketCounts: MutableMap<Double, Long> = mutableMapOf()
        private var count = atomic(0L)

        init {
            for (bucket in sortedBuckets) {
                bucketCounts[bucket] = 0
            }
        }

        public fun startTimer():Timer{
            return Timer(this, Clock.System.now().toEpochMilliseconds())
        }

        public fun time(runnable: Runnable):Double{
            return timeWithExemplar(runnable, listOf())
        }

        public fun timeWithExemplar(runnable: Runnable, exemplarLabels: List<String>): Double {
            val start = startTimer()
            val final: Double;
            try {
                runnable.run()
            }catch (e: Exception) {
                throw RuntimeException("Error while running timed block", e)
            } finally {
                final = start.observeDurationWithExemplar(exemplarLabels)
            }
            return final
        }

        public constructor(buckets: List<Double>,
                            exemplarsEnabled: Boolean = false,
                            exemplarSampler: HistogramExemplarSample? = null) {
            this.upperBounds = buckets
            this.exemplarsEnabled = exemplarsEnabled
            this.exemplarSampler = exemplarSampler
            this.exemplars = mutableListOf<AtomicRef<Exemplar>>()
            this.cumulativeCounts = mutableListOf<AtomicLong>()
            for (i in buckets.indices) {
                cumulativeCounts.add(atomic(0.0.toRawBits()))
                exemplars.add(atomic(Exemplar(emptyList(), 0.0)))
            }
        }
        private final val upperBounds: List<Double>
        private final val exemplarsEnabled: Boolean
        private final val exemplars: MutableList<AtomicRef<Exemplar>>
        private final val exemplarSampler: HistogramExemplarSample?
        private final val cumulativeCounts: MutableList<AtomicLong>
        private final val sum = atomic(0.0.toRawBits())
        private final val created = Clock.System.now().toEpochMilliseconds()



        public fun observe(value: Double) {
            observeWithExemplar(value, null)
        }

        public fun observeWithExemplar(value: Double, exemplarLabels: List<String>?){
            val exemplar = exemplarLabels ?.let {
                if (exemplarsEnabled) {
                   Exemplar(exemplarLabels,value, Clock.System.now().toEpochMilliseconds())
                } else {
                    null
                }
            }
            for( i in upperBounds.indices){
                if (value <= upperBounds[i]){
                    cumulativeCounts[i].updateAndGet {
                        val current = Double.fromBits(it)
                        val updated = current + 1.0
                        updated.toRawBits()
                    }
                    //TODO(update Exemplar)
                    break
                }

            }
            sum.updateAndGet { currentBits ->
                val current = Double.fromBits(currentBits)
                val updated = current + value
                updated.toRawBits()
            }
        }

        public fun get():Value{
            val buckets = mutableListOf<Double>()
            val exemplars = mutableListOf<Exemplar>()
            var acc = 0.0
            for (i in upperBounds.indices) {
                acc+= Double.fromBits(cumulativeCounts[i].value)
                buckets.add(acc)
                exemplars.add(this.exemplars[i].value)
            }
            return Value(
                sum = Double.fromBits(sum.value),
                buckets = buckets,
                created = created
            )
        }

    }

    public fun observe(value: Double){
        noLabelsChild?.observe(value)
    }

    public fun observeWithExemplar(value: Double, exemplarLabels: List<String>){
        noLabelsChild?.observeWithExemplar(value, exemplarLabels)
    }

    public fun time(runnable: Runnable): Double {
        return noLabelsChild?.time(runnable) ?: 0.0
    }

    public fun timeWithExemplar(runnable: Runnable, exemplarLabels: List<String>): Double {
        return noLabelsChild?.timeWithExemplar(runnable, exemplarLabels) ?: 0.0
    }

    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for((labels,childs) in childMetrics){
            val value = childs.get()
            val labelNamesWithLe = labelNames + "le"
            for (i in value.buckets.indices) {
                val labelValuesWithLe = labels + ((value.buckets[i]).toString())
                samples.add(
                    Sample(name = fullName + "_bucket",
                        labelNames = labelNamesWithLe,
                        labelValues = labelValuesWithLe,
                        value = value.buckets[i],
                        timestamp = value.created
                    )
                )
            }
            samples.add(Sample(name = fullName + "_count",
                labelNames = labelNames,
                labelValues = labels,
                value = value.buckets[sortedBuckets.size-1],
                timestamp = value.created
            ))
            samples.add(Sample(name = fullName + "_sum",
                labelNames = labelNames,
                labelValues = labels,
                value = value.sum,
                timestamp = value.created
            ))

        }
        return familySamplesList(samples)
    }
}