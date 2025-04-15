package io.github.kotlin.fibonacci

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
        private var value = 0.0

        actual fun inc(amount: Double) {
            require(amount >= 0) { "Value must be positive" }
            value += amount
        }

        actual fun inc(){
            value += 1.0
        }

        actual fun get(): Double = value
    }

    actual fun inc(amount: Double): Unit? {
        require(amount >= 0) { "Amount must be positive" }
        return noLabelsChild?.inc(amount)
    }

    actual fun inc(): Unit? = noLabelsChild?.inc()

    actual fun get(): Double = noLabelsChild?.get() ?: 0.0

    actual override fun collect(): MetricFamilySamples {
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