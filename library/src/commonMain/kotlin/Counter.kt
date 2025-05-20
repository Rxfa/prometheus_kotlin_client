package io.github.kotlin.fibonacci

import kotlin.reflect.KClass

public fun counter(name: String, block: CounterBuilder.() -> Unit): Counter {
    return CounterBuilder(name).apply(block).build()
}

public class Counter internal constructor(
    fullName: String,
    help: String,
    labelNames: List<String> = listOf(),
    unit: String = "",
    public val includeCreatedSeries: Boolean = false,
) : SimpleCollector<Counter.Child>(fullName, help, labelNames, unit) {
    override val suffixes: Set<String> = setOf("_total")
    override val name: String = if(suffixes.any{ fullName.endsWith(it) }) fullName else fullName + "_total"
    override val type: Type = Type.COUNTER

    init {
        initializeNoLabelsChild()
    }

    override fun newChild(): Child {
        return Child()
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