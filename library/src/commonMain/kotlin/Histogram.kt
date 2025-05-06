package io.github.kotlin.fibonacci

import kotlinx.atomicfu.atomic
import kotlinx.datetime.Clock

class Histogram(
    fullName: String,
    help: String,
    labelNames: List<String> = emptyList(),
    unit: String = "",
    includeCreatedSeries: Boolean = false,
    buckets: List<Double> = listOf(
        0.005, 0.01, 0.025, 0.05, 0.1,
        0.25, 0.5, 1.0, 2.5, 5.0, 10.0
    )
):SimpleCollector<Histogram.Child>(fullName, help, labelNames, unit){

    //TODO(eventually change this version to the buffer one)

    override val suffixes: Set<String> = setOf()

    /**
     * Histograms and summaries
     *
     * The histogram and summary types are difficult to represent in the text format. The following conventions apply:
     *
     *     The sample sum for a summary or histogram named x is given as a separate sample named x_sum.
     *     The sample count for a summary or histogram named x is given as a separate sample named x_count.
     *     Each quantile of a summary named x is given as a separate sample line with the same name x and a label {quantile="y"}.
     *     Each bucket count of a histogram named x is given as a separate sample line with the name x_bucket and a label {le="y"} (where y is the upper bound of the bucket).
     *     A histogram must have a bucket with {le="+Inf"}. Its value must be identical to the value of x_count.
     *     The buckets of a histogram and the quantiles of a summary must appear in increasing numerical order of their label values (for the le or the quantile label, respectively).
     *
     */

    override val name: String = fullName

    override val type: Type = Type.HISTOGRAM

    private val sortedBuckets: List<Double> = (buckets.sorted() + Double.POSITIVE_INFINITY).distinct()

    override fun newChild(): Child {
        return Child()
    }

    inner class Child {
        private val bucketCounts: MutableMap<Double, Long> = mutableMapOf()
        private var sum= atomic(0.0)
        private var count= atomic(0L)
        private val created: Long = Clock.System.now().toEpochMilliseconds()

        init {
            for (bucket in sortedBuckets) {
                bucketCounts[bucket] = 0
            }
        }
        fun observe(value: Double){
            for (bucket in sortedBuckets) {
                if (value <= bucket) {
                    bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
                }
            }
        }

        fun getBuckets(): Double{
            TODO()
        }

        fun created(): Long{
            TODO()
        }
    }

    fun observe(value: Double): Unit?{
        TODO()
    }

    fun get(): Double{
        TODO()
    }

    override fun collect(): MetricFamilySamples{
        val samples = mutableListOf<Sample>()
        //TODO()
        return familySamplesList(emptyList())
    }
/*
override fun collect(): List<MetricFamilySamples> {
    val samples = mutableListOf<MetricFamilySamples.Sample>()

    for ((labelValues, child) in children) {
        val value = child.get() // returns Child.Value

        val labelNamesWithLe = labelNames + "le"
        for (i in buckets.indices) {
            val labelValuesWithLe = labelValues + doubleToGoString(buckets[i])
            samples.add(
                MetricFamilySamples.Sample(
                    "${fullName}_bucket",
                    labelNamesWithLe,
                    labelValuesWithLe,
                    value.buckets[i],
                    value.exemplars?.get(i)
                )
            )
        }

        samples.add(
            MetricFamilySamples.Sample(
                "${fullName}_count",
                labelNames,
                labelValues,
                value.buckets.last()
            )
        )

        samples.add(
            MetricFamilySamples.Sample(
                "${fullName}_sum",
                labelNames,
                labelValues,
                value.sum
            )
        )

        if (Environment.includeCreatedSeries()) {
            samples.add(
                MetricFamilySamples.Sample(
                    "${fullName}_created",
                    labelNames,
                    labelValues,
                    value.created / 1000.0
                )
            )
        }
    }

    return familySamplesList(MetricFamilySamples.Type.HISTOGRAM, samples)
}

 */
}

/**
 * Histogram
 *
 * Histograms measure distributions of discrete events. Common examples are the latency of HTTP requests, function runtimes, or I/O request sizes.
 *
 * A Histogram MetricPoint MUST contain at least one bucket, and SHOULD contain Sum, and Created values. Every bucket MUST have a threshold and a value.
 *
 * Histogram MetricPoints MUST have one bucket with an +Inf threshold.
 * Buckets MUST be cumulative.
 * As an example for a metric representing request latency in seconds its values for buckets with thresholds 1, 2, 3,
 * and +Inf MUST follow value_1 <= value_2 <= value_3 <= value_+Inf. If ten requests took 1 second each, the values of the 1, 2, 3,
 * and +Inf buckets MUST equal 10.
 *
 * The +Inf bucket counts all requests. If present, the Sum value MUST equal the Sum of all the measured event values.
 * Bucket thresholds within a MetricPoint MUST be unique.
 *
 * Semantically, Sum, and buckets values are counters so MUST NOT be NaN or negative.
 * Negative threshold buckets MAY be used, but then the Histogram MetricPoint MUST NOT contain a sum value as it would no longer be a counter semantically.
 * Bucket thresholds MUST NOT equal NaN. Count and bucket values MUST be integers.
 *
 * A Histogram MetricPoint SHOULD have a Timestamp value called Created.
 * This can help ingestors discern between new metrics and long-running ones it did not see before.
 *
 * A Histogram's Metric's LabelSet MUST NOT have a "le" label name.
 *
 * Bucket values MAY have exemplars.
 * Buckets are cumulative to allow monitoring systems to drop any non-+Inf bucket for performance/anti-denial-of-service reasons
 * in a way that loses granularity but is still a valid Histogram.
 *
 * EDITOR’S NOTE: The second sentence is a consideration, it can be moved if needed
 *
 * Each bucket covers the values less and or equal to it, and the value of the exemplar MUST be within this range. Exemplars SHOULD be put into the bucket with the highest value. A bucket MUST NOT have more than one exemplar.
 */