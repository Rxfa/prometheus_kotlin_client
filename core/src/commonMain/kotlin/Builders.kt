package io.github.rxfa.prometheus.core

import kotlinx.datetime.Clock

/**
 * Base class for building [SimpleCollector] instances with a fluent and idiomatic DSL.
 *
 * @param T The specific collector type being built.
 * @property name The name of the metric to be built.
 */
public abstract class MetricBuilder<T : SimpleCollector<*>> protected constructor(
    protected val name: String,
) {
    /** Help text describing what this metric measures. */
    protected var help: String = ""

    /** List of label names associated with the metric. */
    protected var labelNames: List<String> = emptyList()

    /** Unit of measurement (e.g., "seconds", "bytes"). */
    protected var unit: String = ""

    /**
     * Sets the help description for the metric.
     *
     * @param help A human-readable explanation of what the metric represents.
     */
    public fun help(help: String) {
        apply { this.help = help }
    }

    /**
     * Sets the label names that this metric will use.
     *
     * @param labelNames Vararg list of label names (e.g., "method", "status").
     */
    public fun labelNames(vararg labelNames: String) {
        apply { this.labelNames = labelNames.toList() }
    }

    /**
     * Sets the unit of the metric.
     *
     * @param unit The unit of measurement (e.g., "ms", "requests").
     */
    public fun unit(unit: String) {
        apply { this.unit = unit }
    }

    /**
     * Builds and returns the metric instance.
     *
     * @return A fully constructed collector instance of type [T].
     */
    public abstract fun build(): T
}

/**
 * Builder class for creating [Counter] metrics.
 *
 * Counters represent cumulative values that increase over time (and may reset on restart).
 *
 * @constructor Internal constructor used via DSL or factory.
 * @param name The metric name.
 */
public class CounterBuilder internal constructor(
    name: String,
) : MetricBuilder<Counter>(name) {
    /** Whether to include the `_created` series (timestamp of first observation). */
    protected var includeCreatedSeries: Boolean = false

    /**
     * Configures whether the `metric_created` series should be included.
     *
     * This is useful for understanding when a counter was instantiated.
     *
     * @param includeCreatedSeries `true` to include the `_created` series.
     */
    public fun includeCreatedSeries(includeCreatedSeries: Boolean) {
        apply { this.includeCreatedSeries = includeCreatedSeries }
    }

    /**
     * Builds and returns the [Counter] instance.
     */
    override fun build(): Counter = Counter(name, help, labelNames, unit, includeCreatedSeries)
}

/**
 * Builder class for creating [Gauge] metrics.
 *
 * Gauges represent a value that can increase and decrease, such as memory usage or number of active sessions.
 *
 * @constructor Constructs a gauge builder.
 * @param name The metric name.
 */
public class GaugeBuilder(
    name: String,
) : MetricBuilder<Gauge>(name) {
    /** Clock used to capture timestamps (default is [Clock.System]). */
    protected var clock: Clock = Clock.System

    /**
     * Sets a custom [Clock] for this metric, useful for testing or time manipulation.
     *
     * @param clock The [Clock] instance to use for time-based operations.
     */
    public fun clock(clock: Clock) {
        apply { this.clock = clock }
    }

    /**
     * Builds and returns the [Gauge] instance.
     */
    override fun build(): Gauge = Gauge(name, help, labelNames, unit, clock)
}

public class HistogramBuilder(
    name: String,
    private val buckets: List<Double>? = null,
) : MetricBuilder<Histogram>(name) {
    override fun build(): Histogram {
        val defaultBuckets =
            buckets ?: listOf(
                0.005,
                0.01,
                0.025,
                0.05,
                0.1,
                0.25,
                0.5,
                1.0,
                2.5,
                5.0,
                10.0,
            )
        return Histogram(name, help, labelNames, unit, buckets = defaultBuckets)
    }
}

public class SummaryBuilder internal constructor(
    name: String,
    private val includeCreatedSeries: Boolean = false,
    private val quantiles: List<Quantiles.Quantile> = emptyList<Quantiles.Quantile>(),
    private val maxAgeSeconds: Long = 60,
    private val ageBuckets: Int = 5,
) : MetricBuilder<Summary>(name) {
    override fun build(): Summary = Summary(name, help, labelNames, unit, includeCreatedSeries, quantiles, maxAgeSeconds, ageBuckets)
}
