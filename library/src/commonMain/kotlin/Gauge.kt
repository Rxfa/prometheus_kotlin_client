package io.github.kotlin.fibonacci

import kotlinx.datetime.Clock
import kotlin.time.measureTime

public class Gauge(
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


    public inner class Child {
        private var value = 0.0

        /**
         * Increment the gauge by the given amount.
         */
        public fun inc(amount: Double){
            require(amount >= 0) { "Increment must be non-negative" }
            value += amount
        }

        /**
         * Increment the gauge by 1.
         */
        public fun inc(){
            inc(1.0)
        }

        /**
         * Decrement the Gauge by the given amount.
         */
        public fun dec(amount: Double){
            require(amount >= 0) { "Decrement must be non-negative" }
            value -= amount
        }

        /**
         * Decrement the gauge by 1.
         */
        public fun dec(){
            dec(1.0)
        }

        /**
         * Set the gauge to the given value.
         */
        public fun set(amount: Double){
            value = amount
        }

        /**
         * Set the gauge to the current unixtime in seconds.
         */
        public fun setToCurrentTime(){
            value = getCurrentSeconds(clock).toDouble()
        }

        public fun get(): Double = value
    }

    /**
     * Increment the gauge by 1.
     */
    public fun inc(){
        noLabelsChild?.inc()
    }

    /**
     * Increment the gauge by the given amount.
     */
    public fun inc(amount: Double){
        noLabelsChild?.inc(amount)
    }

    /**
     * Decrement the gauge by 1.
     */
    public fun dec(){
        noLabelsChild?.dec()
    }

    /**
     * Decrement the Gauge by the given amount.
     */
    public fun dec(amount: Double){
        noLabelsChild?.dec(amount)
    }

    /**
     * Set the gauge to the given value.
     */
    public fun set(amount: Double){
        noLabelsChild?.set(amount)
    }

    /**
     * Set the gauge to the current unixtime in seconds.
     */
    public fun setToCurrentTime() {
        noLabelsChild?.setToCurrentTime()
    }

    public fun get(): Double {
        return noLabelsChild?.get() ?: 0.0
    }

    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for ((labels, child) in childMetrics){
            samples += Sample(name = name, labelNames = labelNames, labelValues = labels, value = child.get())
        }
        return familySamplesList(samples)
    }
}

public inline fun <T> Gauge.track(block: () -> T): T {
    inc()
    try {
        return block()
    } finally {
        dec()
    }
}

public inline fun <T> Gauge.Child.track(block: () -> T): T {
    inc()
    try {
        return block()
    } finally {
        dec()
    }
}


public fun <T> Gauge.setDuration(block: () -> T): T{
    val result: T
    val secondsTaken = measureTime {
        block().also { result = it }
    }.inWholeSeconds.toDouble()
    set(secondsTaken)
    return result
}

public fun <T> Gauge.Child.setDuration(block: () -> T): T{
    val result: T
    val secondsTaken = measureTime {
        block().also { result = it }
    }.inWholeSeconds.toDouble()
    set(secondsTaken)
    return result
}
