package io.github.rxfa.prometheus.core

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.math.pow

/**
 * A DSL-style function to create and register a [Histogram] using a [HistogramBuilder].
 * Creates a histogram with the specified [name] and configuration defined in the [block].
 *
 * @param name The name of the histogram.
 * @param block A configuration block for the histogram.
 * @return A configured [Histogram] instance.
 */
public fun histogram(
    name: String,
    block: HistogramBuilder.() -> Unit
): Histogram {
    return HistogramBuilder(name).apply(block).build()
}

/**
 * Creates a histogram with predefined [buckets].
 *
 * @param name The name of the histogram.
 * @param block A configuration block for the histogram.
 * @param buckets A list of bucket boundaries in Doubles.
 * @return A configured [Histogram] instance.
 */
public fun histogramBuckets(
    name:String,
    block: HistogramBuilder.() -> Unit,
    buckets: List<Double>
): Histogram {
    return HistogramBuilder(name,buckets).apply(block).build()
}

/**
 * Creates a histogram with linearly spaced buckets.
 *
 * @param name The name of the histogram.
 * @param block A configuration block for the histogram.
 * @param start The starting value of the first bucket.
 * @param width The width of each bucket.
 * @param count The number of buckets.
 * @return A configured [Histogram] instance.
 */
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

/**
 * Creates a histogram with exponentially spaced buckets.
 *
 * @param name The name of the histogram.
 * @param block A configuration block for the histogram.
 * @param start The starting value of the first bucket.
 * @param factor The factor by which bucket sizes increase.
 * @param count The number of buckets.
 * @return A configured [Histogram] instance.
 */
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

/**
 * Represents the current state of a histogram, including the sum of observed values,
 * the cumulative counts for each bucket, and the creation timestamp.
 *
 * @property sum The sum of all observed values.
 * @property buckets The cumulative counts for each bucket.
 * @property created The timestamp when the histogram was created.
 */
public data class ValueHistogram(
    public val sum: Double,
    public val buckets: List<Double>,
    public val created: Long
)

/**
 * A histogram metric that tracks the distribution of values over a set of buckets.
 *
 * @param fullName The full name of the histogram.
 * @param help A description of the histogram's purpose.
 * @param labelNames The names of the labels for the histogram.
 * @param unit The unit of measurement for the histogram.
 * @param includeCreatedSeries Whether to include a `_created` time series.
 * @param buckets The bucket boundaries for the histogram.
 */
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

    /**
     * Initializes the histogram with the provided bucket boundaries and validates them.
     * Ensures that the buckets are in strictly increasing order and that the last bucket is positive infinity.
     * Throws an exception if the buckets are not valid or if a label named "le" is provided.
     * @param buckets The bucket boundaries for the histogram.
     */
    init {
        for (i in 0 until buckets.lastIndex) {
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
        initializeNoLabelsChild()
    }

    /**
     * A utility class for measuring the duration of operations and recording the observed duration
     * in the associated histogram.
     *
     * @property child The histogram child instance to record the duration.
     * @property start The start time of the operation in milliseconds.
     */
    public class Timer{

        private val child: Child
        private val start: Long

        private val simpleTimer = SimpleTimer()

        public constructor(child: Histogram.Child,start: Long) {
            this.child = child
            this.start = start
        }

        /**
         * Records the duration of the operation and returns the elapsed time.
         *
         * @return The elapsed time in seconds.
         */
        public suspend fun observeDuration():Double{
            return observeDurationWithExemplar(listOf())
        }

        /**
         * Records the duration of the operation with exemplar labels and returns the elapsed time.
         *
         * @param exemplarLabels The labels to associate with the exemplar.
         * @return The elapsed time in seconds.
         */
        public suspend fun observeDurationWithExemplar(exemplarLabels: List<String>): Double {
            val elapsed = simpleTimer.elapsedSecondsFromNanos(start, simpleTimer.defaultTimeProvider.milliTime)
            child.observeWithExemplar(elapsed, exemplarLabels)
            return elapsed
        }

    }

    /**
     * Represents a labeled instance of the histogram, allowing for operations on specific label sets.
     *
     * @property upperBounds The bucket boundaries for the histogram.
     * @property exemplarsEnabled Whether exemplars are enabled for the histogram.
     * @property exemplars The list of exemplars for each bucket.
     * @property exemplarSampler The sampler used for generating exemplars.
     * @property cumulativeCounts The cumulative counts for each bucket.
     * @property sum The sum of all observed values.
     * @property created The timestamp when the histogram was created.
     */
    public inner class Child {

        /**
         * Starts a timer for measuring the duration of an operation.
         *
         * @return A [Timer] instance.
         */
        public fun startTimer():Timer{
            return Timer(this, Clock.System.now().toEpochMilliseconds())
        }


        /**
         * Starts a timer for measuring the duration of an operation and returns the elapsed time in seconds.
         *
         * @param runnable The operation to time.
         * @return A double representing the elapsed time in seconds.
         */
        public suspend fun time(runnable: Runnable):Double{
            return timeWithExemplar(runnable, listOf())
        }

        /**
         * Starts a timer for measuring the duration of an operation with exemplar labels and returns the elapsed time in seconds.
         *
         * @param runnable The operation to time.
         * @param exemplarLabels The labels to associate with the exemplar.
         * @return A double representing the elapsed time in seconds.
         */
        public suspend fun timeWithExemplar(runnable: Runnable, exemplarLabels: List<String>): Double {
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
            this.exemplars = mutableListOf<AtomicRef<Exemplar?>>()
            this.cumulativeCounts = mutableListOf<AtomicLong>()
            for (i in buckets.indices) {
                cumulativeCounts.add(atomic(0L))
                exemplars.add(atomic(Exemplar(emptyList(), 0.0)))
            }
        }
        private val upperBounds: List<Double>
        private val exemplarsEnabled: Boolean
        private val exemplars: MutableList<AtomicRef<Exemplar?>>
        private val exemplarSampler: HistogramExemplarSample?
        private val cumulativeCounts: MutableList<AtomicLong>
        private val sum = atomic(0.0.toRawBits())
        private val created = Clock.System.now().toEpochMilliseconds()


        /**
         * Observes a value and records it in the histogram.
         *
         * @param value The value to observe.
         */
        public suspend fun observe(value: Double) {
            observeWithExemplar(value, null)
        }


        /**
         * Observes a value with exemplar labels and records it in the histogram.
         *
         * @param value The value to observe.
         * @param exemplarLabels The labels to associate with the exemplar.
         */
        public suspend fun observeWithExemplar(value: Double, exemplarLabels: List<String>?){
            withContext(Dispatchers.Default) {
                val exemplar = exemplarLabels?.let {
                    if (exemplarsEnabled) {
                        Exemplar(exemplarLabels, value, Clock.System.now().toEpochMilliseconds())
                    } else {
                        null
                    }
                }
                for (i in upperBounds.indices) {
                    if (value <= upperBounds[i]) {
                        cumulativeCounts[i].getAndIncrement()
                        updateExemplar(value, i, exemplar)
                        break
                    }
                }
                sum.updateAndGet { currentBits ->
                    val current = Double.fromBits(currentBits)
                    val updated = current + value
                    updated.toRawBits()
                }
            }
        }

        private fun updateExemplar(value: Double, i: Int, userProvidedExemplar: Exemplar?) {
            val exemplar = exemplars[i]
            val bucketFrom = if (i == 0) Double.NEGATIVE_INFINITY else upperBounds[i - 1]
            val bucketTo = upperBounds[i]
            var prev: Exemplar?
            var next: Exemplar?
            do {
                prev = exemplar.value
                next = userProvidedExemplar ?: sampleNextExemplar(value, bucketFrom, bucketTo, prev)
                if (next == null || next == prev) {
                    return
                }
            } while (!exemplar.compareAndSet(prev, next))
        }

        private fun sampleNextExemplar(
            value: Double,
            bucketFrom: Double,
            bucketTo: Double,
            prev: Exemplar?
        ): Exemplar? {
            if (!exemplarsEnabled) {
                return null
            }

            if(exemplarSampler != null){
                return exemplarSampler.sample(value, bucketFrom, bucketTo, prev)
            }

            if(exemplarsEnabled || ExemplarConfig.isEnabled()){
                val sampler = ExemplarConfig.getHistogramExemplarSampler()
                if(sampler != null){
                    return sampler.sample(value, bucketFrom, bucketTo, prev)
                }
            }
            return null
        }

        /**
         * Retrieves the current state of the histogram.
         *
         * @return A [ValueHistogram] instance representing the current state.
         */
        public fun get():ValueHistogram{
            val buckets = mutableListOf<Double>()
            val exemplars = mutableListOf<Exemplar?>()
            var acc = 0.0
            for (i in upperBounds.indices) {
                acc+= cumulativeCounts[i].value
                buckets.add(acc)
                exemplars.add(this.exemplars[i].value)
            }
            return ValueHistogram(
                sum = Double.fromBits(sum.value),
                buckets = buckets,
                created = created
            )
        }

    }

    /**
     * Observes a value and records it in the histogram without labels.
     */
    public suspend fun observe(value: Double){
        noLabelsChild?.observe(value)
    }

    /**
     * Observes a value with exemplar labels and records it in the histogram without labels.
     *
     * @param value The value to observe.
     * @param exemplarLabels The labels to associate with the exemplar.
     */
    public suspend fun observeWithExemplar(value: Double, exemplarLabels: List<String>){
        noLabelsChild?.observeWithExemplar(value, exemplarLabels)
    }

    /**
     * Starts a timer for measuring the duration of an operation without labels.
     *
     * @return A double representing the elapsed time in seconds.
     */
    public suspend fun time(runnable: Runnable): Double {
        return noLabelsChild?.time(runnable) ?: 0.0
    }

    /**
     * Starts a timer for measuring the duration of an operation with exemplar labels.
     *
     * @param runnable The operation to time.
     * @param exemplarLabels The labels to associate with the exemplar.
     * @return A double representing the elapsed time in seconds.
     */
    public suspend fun timeWithExemplar(runnable: Runnable, exemplarLabels: List<String>): Double {
        return noLabelsChild?.timeWithExemplar(runnable, exemplarLabels) ?: 0.0
    }

    /**
     * Retrieves the current state of the histogram without labels.
     *
     * @return A [ValueHistogram] instance representing the current state.
     */
    public fun get(): ValueHistogram {
        return noLabelsChild?.get() ?: ValueHistogram(0.0, emptyList(), Clock.System.now().toEpochMilliseconds())
    }


    /**
     * Collects the current metric samples for this histogram.
     *
     * @return A [MetricFamilySamples] object containing the collected samples.
     */
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