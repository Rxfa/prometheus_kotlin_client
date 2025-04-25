package io.github.kotlin.fibonacci

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.DoubleAdder

actual class Counter actual constructor(
    fullName: String,
    help: String,
    labelNames: List<String>,
    unit: String,
    val includeCreatedSeries: Boolean,
) : SimpleCollector<Counter.Child>(fullName, help, labelNames, unit) {
    override val suffixes: Set<String> = setOf("_total")

    override val name: String = if(suffixes.any{ fullName.endsWith(it) }) fullName else fullName + "_total"

    override val type: Type = Type.COUNTER

    init {
        initializeNoLabelsChild()
    }

    actual override fun newChild(): Child {
        return Child()
    }

    actual inner class Child {
        private var value = DoubleAdder()
        private val created = getCurrentTime()

        actual fun inc(amount: Double) {
            require(amount >= 0) { "Value must be positive" }
            value.add(amount)
        }

        actual fun get(): Double = value.sum()

        actual fun created(): Long = created
    }

    actual fun inc(amount: Double): Unit? = noLabelsChild?.inc(amount)

    actual fun get(): Double = noLabelsChild?.get() ?: 0.0

    actual override fun collect(): List<MetricFamilySamples> {
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