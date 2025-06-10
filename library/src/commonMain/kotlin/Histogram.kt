package io.github.kotlin.fibonacci

import kotlinx.atomicfu.atomic
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

    override fun newChild(): Child {
        return Child()
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
        private var sum = atomic(0.0)
        private var count = atomic(0L)
        private val created: Long = Clock.System.now().toEpochMilliseconds()

        init {
            for (bucket in sortedBuckets) {
                bucketCounts[bucket] = 0
            }
        }

        public fun observe(value: Double) {
            for (bucket in sortedBuckets) {
                if (value <= bucket) {
                    bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
                }
            }
        }

        public fun observeWithExemplar(value: Double, exemplarLabels: List<String>){

        }

        public fun getBuckets(): Double {
            TODO()
        }

        public fun created(): Long {
            TODO()
        }
    }

    public fun observe(value: Double): Unit? {
        TODO()
    }

    public fun get(): Double {
        TODO()
    }

    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        //TODO()
        return familySamplesList(emptyList())
    }
}