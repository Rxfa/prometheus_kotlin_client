package io.github.rxfa.prometheus.core

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