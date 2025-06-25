package io.github.rxfa.prometheus.core

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

/**
 * Creates a quantile for use in summaries.
 *
 * @param quantile The desired quantile value (e.g., 0.5 for median). Must be between 0.0 and 1.0.
 * @param error The acceptable error margin for the quantile. Defaults to 0.01. Must be between 0.0 and 1.0.
 * @return A [Quantiles.Quantile] instance representing the specified quantile and error.
 * @throws IllegalArgumentException if [quantile] or [error] is outside the range [0.0, 1.0].
 */
public fun quantile(
    quantile: Double,
    error: Double = 0.01
): Quantiles.Quantile {
    if (quantile < 0.0 || quantile > 1.0) {
        throw IllegalArgumentException("Quantile " + quantile + " invalid: Expected number between 0.0 and 1.0.")
    }
    if (error < 0.0 || error > 1.0) {
        throw IllegalArgumentException("Error " + error + " invalid: Expected number between 0.0 and 1.0.")
    }
    return Quantiles.Quantile(quantile, error)
}

/**
 * Converts a variable number of quantiles into a list.
 *
 * @param quantiles A variable number of [Quantiles.Quantile] instances.
 * @return A [List] containing the provided quantiles.
 */
public fun quantiles(
    vararg quantiles: Quantiles.Quantile
): List<Quantiles.Quantile> {
    return quantiles.toList()
}

/**
 * Creates a summary with predefined quantiles.
 *
 * @param name The name of the summary.
 * @param block A configuration block for the summary.
 * @param quantiles A list of [Quantiles.Quantile] to include in the summary.
 * @param maxAgeSeconds The maximum age of observations in seconds. Defaults to 60.
 * @param ageBuckets The number of buckets to divide the observation window into. Defaults to 5.
 * @return A configured [Summary] instance.
 */
public fun summaryQuantiles(
    name: String,
    block: SummaryBuilder.() -> Unit,
    quantiles: List<Quantiles.Quantile>,
    maxAgeSeconds: Long = 60,
    ageBuckets: Int = 5
): Summary {
    return SummaryBuilder(name,false,quantiles,maxAgeSeconds,ageBuckets).apply {block}.build()
}

/**
 * Creates a summary without predefined quantiles.
 *
 * @param name The name of the summary.
 * @param block A configuration block for the summary.
 * @return A configured [Summary] instance.
 */
public fun summary(
    name: String,
    block: SummaryBuilder.() -> Unit
): Summary {
    return SummaryBuilder(name).apply(block).build()
}

/**
 * Represents the current state of a summary, including the count, sum, quantiles, and creation timestamp.
 *
 * @property count The total number of observations.
 * @property sum The sum of all observed values.
 * @property quantiles A map of quantile values to their observed values, or null if no quantiles are defined.
 * @property created The timestamp when the summary was created.
 */
public data class ValueSummary(
    val count: Double,
    val sum : Double,
    val quantiles: Map<Double,Double>?,
    val created: Long

)

/**
 * A [Summary] is a metric that provides statistical information about observed values, including quantiles.
 *
 * This implementation supports quantiles, sum, count, and allows for time-based observation tracking.
 *
 * @param fullName The full name of the summary metric.
 * @param help A description of what the summary measures.
 * @param labelNames Optional list of label names for the summary.
 * @param unit Optional unit of measurement for the summary.
 * @param includeCreatedSeries If `true`, also emits a `_created` series per label set.
 * @param quantiles A list of [Quantiles.Quantile] to include in the summary.
 * @param maxAgeSeconds The maximum age of observations in seconds. Defaults to 60.
 * @param ageBuckets The number of buckets to divide the observation window into. Defaults to 5.
 */
public class Summary internal constructor(
    fullName: String,
    help: String,
    labelNames: List<String> = emptyList(),
    unit: String = "",
    includeCreatedSeries: Boolean = false,
    private val quantiles: List<Quantiles.Quantile>,
    private val maxAgeSeconds: Long ,
    private val ageBuckets: Int
): SimpleCollector<Summary.Child>(fullName, help, labelNames, unit) {

    override val suffixes: Set<String> = setOf()
    override val name: String = fullName
    override val type: Type = Type.SUMMARY

    override fun newChild(): Child {
        return Child(quantiles)
    }

    /**
     * Makes sure that there ano labels named 'quantile' in the labelNames.
     */
    init{
        for (label in labelNames) {
            if (label.equals("quantile")) {
                throw IllegalStateException("Summary cannot have a label named 'quantile'.")
            }
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

        public constructor(child: Summary.Child,start: Long) {
            this.child = child
            this.start = start
        }

        public suspend fun observeDuration():Double{
            val elapsed = simpleTimer.elapsedSecondsFromNanos(start, simpleTimer.defaultTimeProvider.milliTime)
            child.observe(elapsed)
            return elapsed
        }


    }


    /**
     * Represents a labeled child of the summary metric.
     *
     * Use this to operate on a specific label set:
     *
     * summary.labels("GET").startTimer().observeDuration()
     */
    public inner class Child{

        public fun startTimer():Timer{
            return Timer(this, Clock.System.now().toEpochMilliseconds())
        }

        public suspend fun time(runnable: Runnable):Double{
            val start = startTimer()
            val final : Double;
            try {
                runnable.run()
            } catch (e: Exception) {
                throw RuntimeException("Error while running timed block", e)
            } finally {
                final = start.observeDuration()
            }
            return final
        }

        public constructor(
            quantiles: List<Quantiles.Quantile>
        ) {
            this.quantiles = quantiles
            if(quantiles.isNotEmpty()){
                quantilesValues = TimeWindowQuantiles(quantiles.toTypedArray(), maxAgeSeconds, ageBuckets)
            }else quantilesValues = null
        }

        private val quantiles: List<Quantiles.Quantile>
        private val quantilesValues: TimeWindowQuantiles?
        private val sum: AtomicLong = atomic(0.0.toRawBits())
        private val count: AtomicLong = atomic(0L)
        private val created: Long = Clock.System.now().toEpochMilliseconds()



        /**
         * Returns the current value of the summary as a [ValueSummary] object.
         *
         * This includes the count, sum, quantiles, and creation timestamp.
         */
        public fun get():ValueSummary{
            val map = mutableMapOf<Double, Double>()
            for(quantile in quantiles){
                map[quantile.quantile] = quantilesValues?.get(quantile.quantile) ?: 0.0
            }


            return ValueSummary(
                count.value.toDouble(),
                Double.fromBits(sum.value),
                map,
                created
                )
        }


        /**
         * Observes a value and updates the summary metrics accordingly.
         * This method is thread-safe and can be called concurrently.
         *
         * @param value The value to observe. Must be a finite double.
         * @throws IllegalArgumentException if the value is NaN or infinite.
         */
        public suspend fun observe(value:Double){
            withContext(Dispatchers.Default) {
                count.getAndIncrement()
                sum.updateAndGet { currentBits ->
                    val current = Double.fromBits(currentBits)
                    val updated = current + value
                    updated.toBits()
                }
                quantilesValues?.insert(value)
            }

        }

    }
    public suspend fun observe(value: Double) {
        noLabelsChild?.observe(value)
    }

    public suspend fun time(runnable: Runnable): Double {
        return noLabelsChild?.time(runnable) ?: 0.0
    }

    public fun get(): ValueSummary {
        return noLabelsChild?.get() ?: throw IllegalStateException("No labels child is not initialized.")
    }


    public override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for ((labels, childs) in childMetrics) {
            val value = childs.get()
            val labelNamesWithQuantiles = labelNames + "quantile"
            if(value.quantiles != null){
            for (quantile in value.quantiles) {
                val labelValuesWithQuantiles = labels + quantile.key.toString()
                samples.add(
                    Sample(name = fullName + "_quantile",
                        labelNames = labelNamesWithQuantiles,
                        labelValues = labelValuesWithQuantiles,
                        value = quantile.value,
                        timestamp = value.created
                    )
                )
            }
            }
            samples.add(Sample(name = fullName + "_count",
                labelNames = labelNames,
                labelValues = labels,
                value = value.count,
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


