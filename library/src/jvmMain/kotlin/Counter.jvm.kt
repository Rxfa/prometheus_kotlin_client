package io.github.kotlin.fibonacci

actual class Counter actual constructor(
    name: String,
    help: String,
    labelNames: List<String>,
    unit: String
) : SimpleCollector<Counter.Child>(name, help, labelNames, unit) {

    init {
        initializeNoLabelsChild()
    }

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
            val sampleName = if(fullName.endsWith("_total")) fullName else fullName + "_total"
            samples += Sample(
                name = sampleName,
                labelNames = labelNames,
                labelValues = labels,
                value = child.get(),
            )
        }
        return familySamplesList(Type.COUNTER, samples)
    }
}