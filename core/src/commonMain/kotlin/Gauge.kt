package io.github.rxfa.prometheus.core

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.time.measureTime

public fun gauge(name: String, block: GaugeBuilder.() -> Unit): Gauge {
    return GaugeBuilder(name).apply(block).build()
}

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


    public inner class Child {
        private var value = atomic(0.0.toRawBits())

        /**
         * Increment the gauge by the given amount.
         */
        public suspend fun inc(amount: Double){
            require(amount >= 0) { "Increment must be non-negative" }
            withContext(Dispatchers.Default){
                value.updateAndGet { currentBits ->
                    val current = Double.fromBits(currentBits)
                    val updated = current + amount
                    updated.toBits()
                }
            }
        }

        /**
         * Increment the gauge by 1.
         */
        public suspend fun inc(){
            inc(1.0)
        }

        /**
         * Decrement the Gauge by the given amount.
         */
        public suspend fun dec(amount: Double){
            require(amount >= 0) { "Decrement must be non-negative" }
            withContext(Dispatchers.Default){
                value.updateAndGet { currentBits ->
                    val current = Double.fromBits(currentBits)
                    val updated = current - amount
                    updated.toBits()
                }
            }
        }

        /**
         * Decrement the gauge by 1.
         */
        public suspend fun dec(){
            dec(1.0)
        }

        /**
         * Set the gauge to the given value.
         */
        public suspend fun set(amount: Double) {
            withContext(Dispatchers.Default) {
                value.value = amount.toRawBits()
            }
        }

        /**
         * Set the gauge to the current unixtime in seconds.
         */
        public suspend fun setToCurrentTime(){
            withContext(Dispatchers.Default){
                value.value = getCurrentSeconds(clock).toDouble().toRawBits()
            }

        }

        public fun get(): Double = Double.fromBits(value.value)
    }

    /**
     * Increment the gauge by 1.
     */
    public suspend fun inc(){
        noLabelsChild?.inc()
    }

    /**
     * Increment the gauge by the given amount.
     */
    public suspend fun inc(amount: Double){
        noLabelsChild?.inc(amount)
    }

    /**
     * Decrement the gauge by 1.
     */
    public suspend fun dec(){
        noLabelsChild?.dec()
    }

    /**
     * Decrement the Gauge by the given amount.
     */
    public suspend fun dec(amount: Double){
        noLabelsChild?.dec(amount)
    }

    /**
     * Set the gauge to the given value.
     */
    public suspend fun set(amount: Double){
        noLabelsChild?.set(amount)
    }

    /**
     * Set the gauge to the current unixtime in seconds.
     */
    public suspend fun setToCurrentTime() {
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

public suspend inline fun <T> Gauge.track(block: () -> T): T {
    inc()
    try {
        return block()
    } finally {
        dec()
    }
}

public suspend inline fun <T> Gauge.Child.track(block: () -> T): T {
    inc()
    try {
        return block()
    } finally {
        dec()
    }
}


public suspend fun <T> Gauge.setDuration(block: () -> T): T{
    val result: T
    val secondsTaken = measureTime {
        block().also { result = it }
    }.inWholeSeconds.toDouble()
    set(secondsTaken)
    return result
}

public suspend fun <T> Gauge.Child.setDuration(block: () -> T): T{
    val result: T
    val secondsTaken = measureTime {
        block().also { result = it }
    }.inWholeSeconds.toDouble()
    set(secondsTaken)
    return result
}
