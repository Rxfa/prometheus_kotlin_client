package io.github.rxfa.prometheus.core

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
public fun counter(
    name: String,
    block: CounterBuilder.() -> Unit,
): Counter {
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
            metricName = "${metricName}_$unit"
        }
        return "${metricName}_total"
    }


    /**
     * Represents a labeled child of the counter metric.
     *
     * Use this to operate on a specific label set:
     * ```
     * counter.labels("GET").inc()
     * ```
     */
    public inner class Child {
        /**
         * The current value of the counter, stored as a raw bits representation of a Double.
         * This allows for atomic updates to the counter value, since AtomicDouble nor DoubleAdder is available in Kotlin/Native.
         */
        private var value = atomic(0.0.toRawBits())

        /**
         * Increments the counter by the specified [amount].
         * To update the value we must first tranform from raw bits to Double, then add the [amount],
         * and finally convert back to raw bits for atomic storage.
         *
         * @param amount The value to increment the counter by. Must be non-negative.
         * @throws IllegalArgumentException if [amount] is negative.
         */
        public suspend fun inc(amount: Double) {
            require(amount >= 0) { "Value must be positive" }
            withContext(Dispatchers.Default) {
                value.updateAndGet { currentBits ->
                    val current = Double.fromBits(currentBits)
                    val updated = current + amount
                    updated.toBits()
                }
            }
        }

        /** Increments the counter by 1.*/
        public suspend fun inc() {
            inc(1.0)
        }

        /**
         * Retrieves the current value of the counter.
         *
         * @return The current value of the counter.
         */
        public fun get(): Double =  Double.fromBits(value.value)
    }

    /**
     * Increments the counter by the specified [amount].
     *
     * @param amount The value to increment the counter by. Must be non-negative.
     * @throws IllegalArgumentException if [amount] is negative.
     */
    public suspend fun inc(amount: Double): Unit? = noLabelsChild?.inc(amount)


    /** Increments the counter by 1.*/
    public suspend fun inc(): Unit? = noLabelsChild?.inc()

    /**
     * Retrieves the current value of the counter.
     *
     * @return The current value of the counter, or 0.0 if no labels are defined.
     */
    public fun get(): Double = noLabelsChild?.get() ?: 0.0

    /**
     * Collects the current metric samples for this counter.
     *
     * @return A [MetricFamilySamples] object containing the collected samples.
     */
    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for ((labels, child) in childMetrics) {
            samples += Sample(name = name, labelNames = labelNames, labelValues = labels, value = child.get())
            if (includeCreatedSeries) {
                val createdSeriesName = name.removeSuffix("_total") + "_created"
                samples += Sample(name = createdSeriesName, labelNames = labelNames, labelValues = labels, value = child.get())
            }
        }
        return familySamplesList(samples)
    }
}

/**
 * Executes the given [block] and increments the counter if an exception of the specified types is thrown.
 *
 * @param exceptionTypes The types of exceptions to count. If empty, all exceptions are counted.
 * @param block The block of code to execute.
 * @return The result of the block, or `null` if an exception is caught.
 */
public suspend fun <T> Counter.countExceptions(
    vararg exceptionTypes: KClass<out Throwable>,
    block: () -> T,
): T? {
    return try {
        block()
    } catch (e: Throwable) {
        if (exceptionTypes.isEmpty() || e::class in exceptionTypes) {
            inc()
        }
        null
    }
}

/**
 * Executes the given [block] and increments the counter if an exception of the specified types is thrown.
 *
 * @param exceptionTypes The types of exceptions to count. If empty, all exceptions are counted.
 * @param block The block of code to execute.
 * @return The result of the block, or `null` if an exception is caught.
 */
public suspend fun <T> Counter.Child.countExceptions(
    vararg exceptionTypes: KClass<out Throwable>,
    block: () -> T,
): T? {
    return try {
        block()
    } catch (e: Throwable) {
        if (exceptionTypes.isEmpty() || e::class in exceptionTypes) {
            inc()
        }
        null
    }
}
