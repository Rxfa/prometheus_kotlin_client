package io.github.kotlin.fibonacci

import kotlinx.datetime.Clock

public abstract class MetricBuilder<T : SimpleCollector<*>> protected constructor(
    protected val name: String
) {
    protected var _help: String = ""
    protected var _labelNames: List<String> = emptyList()
    protected var _unit: String = ""

    public fun help(help: String) {
        apply { _help = help }
    }

    public fun labelNames(vararg labelNames: String) {
        apply { _labelNames = labelNames.toList() }
    }

    public fun unit(unit: String) {
        apply { _unit = unit }
    }

    public abstract fun build(): T
}

public class CounterBuilder internal constructor(name: String) : MetricBuilder<Counter>(name) {
    protected var _includeCreatedSeries: Boolean = false

    public fun includeCreatedSeries(includeCreatedSeries: Boolean) {
        apply { _includeCreatedSeries = includeCreatedSeries }
    }

    override fun build(): Counter {
        return Counter(name, _help, _labelNames, _unit, _includeCreatedSeries)
    }
}

public class GaugeBuilder(name: String) : MetricBuilder<Gauge>(name) {
    protected var _clock: Clock = Clock.System

    public fun clock(clock: Clock) {
        apply { _clock = clock }
    }

    override fun build(): Gauge {
        return Gauge(name, _help, _labelNames, _unit, _clock)
    }
}

public class HistogramBuilder(name:String,private val buckets: List<Double>? = null ): MetricBuilder<Histogram>(name){
    override fun build(): Histogram {
        val defaultBuckets = buckets ?: listOf(
            0.005, 0.01, 0.025, 0.05, 0.1,
            0.25, 0.5, 1.0, 2.5, 5.0, 10.0
        )
        return Histogram(name, _help, _labelNames, _unit, buckets = defaultBuckets)
    }

}

public class SummaryBuilder internal constructor(
    name: String,
    private val includeCreatedSeries: Boolean = false,
    private val quantiles: List<Quantiles.Quantile> = emptyList<Quantiles.Quantile>(),
    private val maxAgeSeconds: Long = 60,
    private val ageBuckets: Int = 5
) : MetricBuilder<Summary>(name) {

    override fun build(): Summary {
        return Summary(name, _help, _labelNames, _unit, includeCreatedSeries,quantiles, maxAgeSeconds, ageBuckets)
    }
}