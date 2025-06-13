package io.github.rxfa.prometheus.core

import kotlin.reflect.KClass

/**
 * DSL-style function to build and register a [Counter] using a [CounterBuilder].
 *
 * Example:
 * ```
 * val requests = counter("http_requests_total") {
 *     help("Total HTTP requests")
 *     labelNames("method", "status")
 * }
 * ```
 *
 * @param name The base name of the counter metric.
 * @param block Configuration block for the [CounterBuilder].
 * @return A configured [Counter] instance.
 */
public fun counter(name: String, block: CounterBuilder.() -> Unit): Counter {
    return CounterBuilder(name).apply(block).build()
}

/**
 * A [Counter] is a metric that only increases (monotonically), typically used for counting occurrences.
 *
 * This implementation supports optional creation of a `_created` time series and allows
 * for use with and without labels.
 *
 * @param fullName The full name of the metric.
 * @param help Help text describing what the counter measures.
 * @param labelNames List of label names for this counter.
 * @param unit The unit of the metric (e.g., "meters", "seconds").
 * @param includeCreatedSeries If `true`, also emits a `_created` series per label set.
 */
public class Counter internal constructor(
    fullName: String,
    help: String,
    labelNames: List<String> = listOf(),
    unit: String = "",
    public val includeCreatedSeries: Boolean = false,
) : SimpleCollector<Counter.Child>(fullName, help, labelNames, unit) {
    override val suffixes: Set<String> = setOf("_total")
    override val name: String = buildMetricName()
    override val type: Type = Type.COUNTER

    init {
        initializeNoLabelsChild()
    }

    override fun newChild(): Child {
        return Child()
    }

    override fun buildMetricName(): String {
        var metricName: String = fullName.removeSuffix("_total")
        if (unit.isNotBlank() && !metricName.endsWith(unit)) {
            metricName = "${metricName}_${unit}"
        }
        return "${metricName}_total"
    }

    public inner class Child {
        private var value = 0.0

        public fun inc(amount: Double) {
            require(amount >= 0) { "Value must be positive" }
            value += amount
        }

        public fun inc(){
            inc(1.0)
        }

        public fun get(): Double = value
    }

    public fun inc(amount: Double): Unit? = noLabelsChild?.inc(amount)

    public fun inc(): Unit? = noLabelsChild?.inc()

    public fun get(): Double = noLabelsChild?.get() ?: 0.0

    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for ((labels, child) in childMetrics){
            samples += Sample(name = name, labelNames = labelNames, labelValues = labels, value = child.get())
            if(includeCreatedSeries){
                val createdSeriesName = name.removeSuffix("_total") + "_created"
                samples += Sample(name = createdSeriesName, labelNames = labelNames, labelValues = labels, value = child.get())
            }
        }
        return familySamplesList(samples)
    }
}

public fun <T> Counter.countExceptions(vararg exceptionTypes: KClass<out Throwable>, block: () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        if (exceptionTypes.isEmpty() || e::class in exceptionTypes) {
            inc()
        }
        null
    }
}

public fun <T> Counter.Child.countExceptions(vararg exceptionTypes: KClass<out Throwable>, block: () -> T): T? {
    return try {
        block()
    } catch (e: Throwable) {
        if (exceptionTypes.isEmpty() || e::class in exceptionTypes) {
            inc()
        }
        null
    }
}