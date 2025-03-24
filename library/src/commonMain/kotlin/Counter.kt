package io.github.kotlin.fibonacci

expect class Counter(
    fullName: String,
    help: String,
    labelNames: List<String> = emptyList(),
    unit: String = "",
    includeCreatedSeries: Boolean = false,
): SimpleCollector<Counter.Child> {
    override fun newChild(): Child

    inner class Child {
        fun inc(amount: Double = 1.0)

        fun get(): Double

        fun created(): Long
    }


    fun inc(amount: Double = 1.0): Unit?

    fun get(): Double

    override fun collect(): List<MetricFamilySamples>
}