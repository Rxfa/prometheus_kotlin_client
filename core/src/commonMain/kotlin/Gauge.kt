package io.github.rxfa.prometheus.core

import kotlinx.datetime.Clock
import kotlin.time.measureTime

/**
 * DSL-style function to build and register a [Gauge] using a [GaugeBuilder].
 *
 * Example:
 * ```
 * val temperature = gauge("room_temperature_celsius") {
 *     help("Current room temperature")
 *     labelNames("room")
 * }
 * ```
 *
 * @param name The name of the gauge metric.
 * @param block Configuration block for the [GaugeBuilder].
 * @return A configured [Gauge] instance.
 */
public fun gauge(name: String, block: GaugeBuilder.() -> Unit): Gauge {
    return GaugeBuilder(name).apply(block).build()
}

/**
 * A [Gauge] is a metric that represents a single numerical value that can go up and down.
 *
 * @param fullName Full name of the metric.
 * @param help Description of what the metric measures.
 * @param labelNames List of label names.
 * @param unit Optional unit of measurement.
 * @param clock Clock used to set the current time as gauge value.
 */
public class Gauge internal constructor(
    fullName: String,
    help: String,
    labelNames: List<String> = listOf(),
    unit: String = "",
    private val clock: Clock = Clock.System,
) : SimpleCollector<Gauge.Child>(fullName, help, labelNames, unit) {
    override val suffixes: Set<String> = setOf()
    override val name: String = fullName
    override val type: Type = Type.GAUGE

    init {
        initializeNoLabelsChild()
    }

    override fun newChild(): Child {
        return Child()
    }

    /**
     * Represents a labeled child of the gauge metric.
     *
     * Use this to operate on a specific label set:
     * ```
     * gauge.labels("GET").inc()
     * ```
     */
    public inner class Child {
        private var value = 0.0

        /** Increments the gauge by the specified amount (must be non-negative). */
        public fun inc(amount: Double){
            require(amount >= 0) { "Increment must be non-negative" }
            value += amount
        }

        /** Increments the gauge by 1. */
        public fun inc(){
            inc(1.0)
        }

        /** Decrements the gauge by the specified amount (must be non-negative). */
        public fun dec(amount: Double){
            require(amount >= 0) { "Decrement must be non-negative" }
            value -= amount
        }

        /** Decrements the gauge by 1. */
        public fun dec(){
            dec(1.0)
        }

        /** Sets the gauge to a specific value. */
        public fun set(amount: Double){
            value = amount
        }

        /** Sets the gauge value to the current Unix time in seconds. */
        public fun setToCurrentTime(){
            value = getCurrentSeconds(clock).toDouble()
        }

        /** Gets the current value of the gauge. */
        public fun get(): Double = value
    }

    /** Increments the unlabeled gauge by 1. */
    public fun inc(){
        noLabelsChild?.inc()
    }

    /** Increments the unlabeled gauge by a specific amount. */
    public fun inc(amount: Double){
        noLabelsChild?.inc(amount)
    }


    /** Decrements the unlabeled gauge by 1. */
    public fun dec(){
        noLabelsChild?.dec()
    }

    /** Decrements the unlabeled gauge by a specific amount. */
    public fun dec(amount: Double){
        noLabelsChild?.dec(amount)
    }

    /** Sets the unlabeled gauge to a specific value. */
    public fun set(amount: Double){
        noLabelsChild?.set(amount)
    }

    /** Sets the unlabeled gauge to the current Unix time in seconds. */
    public fun setToCurrentTime() {
        noLabelsChild?.setToCurrentTime()
    }

    /** Returns the current value of the unlabeled gauge. */
    public fun get(): Double {
        return noLabelsChild?.get() ?: 0.0
    }

    /**
     * Collects all current samples from this gauge, across all label sets.
     *
     * @return A [MetricFamilySamples] representing all current values of this gauge.
     */
    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for ((labels, child) in childMetrics){
            samples += Sample(name = name, labelNames = labelNames, labelValues = labels, value = child.get())
        }
        return familySamplesList(samples)
    }
}

/**
 * Increments the unlabeled gauge while the block is running,
 * and decrements it when the block completes.
 *
 * This is useful for tracking concurrency or in-flight tasks.
 */
public inline fun <T> Gauge.track(block: () -> T): T {
    inc()
    try {
        return block()
    } finally {
        dec()
    }
}

/**
 * Increments the labeled gauge while the block is running,
 * and decrements it when the block completes.
 *
 * This is useful for tracking concurrency or in-flight tasks by label.
 */
public inline fun <T> Gauge.Child.track(block: () -> T): T {
    inc()
    try {
        return block()
    } finally {
        dec()
    }
}

/**
 * Measures the duration of a block and stores it in the unlabeled gauge.
 *
 * @param block The block to execute.
 * @return The result of the block.
 */
public fun <T> Gauge.setDuration(block: () -> T): T{
    val result: T
    val secondsTaken = measureTime {
        block().also { result = it }
    }.inWholeSeconds.toDouble()
    set(secondsTaken)
    return result
}

/**
 * Measures the duration of a block and stores it in the labeled [Gauge.Child].
 *
 * @param block The block to execute.
 * @return The result of the block.
 */
public fun <T> Gauge.Child.setDuration(block: () -> T): T{
    val result: T
    val secondsTaken = measureTime {
        block().also { result = it }
    }.inWholeSeconds.toDouble()
    set(secondsTaken)
    return result
}
