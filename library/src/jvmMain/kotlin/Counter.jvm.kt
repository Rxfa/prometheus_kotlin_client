package io.github.kotlin.fibonacci

actual class Counter actual constructor(
    name: String,
    help: String,
    labelNames: List<String>
) : SimpleCollector<Counter.Child>(name, help, labelNames) {

    init {
        initializeNoLabelsChild()
    }

    constructor(name: String, help: String) : this(name, help, emptyList())

    actual override fun newChild(): Child {
        return Child()
    }

    actual class Child {
        private var value = 0.0
        private var timestamp = System.currentTimeMillis()

        actual fun inc(amount: Double) {
            require(amount >= 0) { "Value must be positive" }
            value += amount
        }

        actual fun get(): Double = value

        actual fun created(): Long = timestamp
    }

    actual fun inc(amount: Double): Unit? = noLabelsChild?.inc(amount)

    actual fun get(): Double = noLabelsChild?.get() ?: 0.0

    actual override fun collect(): List<MetricFamilySamples> {
        val samples = mutableListOf<Sample>()
        for ((labels, child) in childMetrics){
            samples += Sample(
                name = fullName + "_total",
                labelNames = labelNames,
                labelValues = labels,
                value = child.get(),
            )
        }
        return familySamplesList(Type.COUNTER, samples)
    }

    override fun getName(): String? {
        TODO("Not yet implemented")
    }

}