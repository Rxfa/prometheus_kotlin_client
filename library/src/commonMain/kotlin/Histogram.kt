package io.github.kotlin.fibonacci

expect class Histogram(
    fullName: String,
    help: String,
    labelNames: List<String> = emptyList(),
    unit: String = "",
    includeCreatedSeries: Boolean = false,
    buckets: List<Double> = emptyList(),
):SimpleCollector<Histogram.Child> {
    override fun newChild(): Child

    inner class Child {
        fun observe(value: Double)

        fun get(): Double

        fun created(): Long
    }

    fun observe(value: Double): Unit?

    fun get(): Double

    override fun collect(): List<MetricFamilySamples>

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