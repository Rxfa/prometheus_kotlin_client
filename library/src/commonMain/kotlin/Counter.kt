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
        fun inc(amount: Double)

        fun inc()

        fun get(): Double
    }


    fun inc(amount: Double): Unit?

    fun inc(): Unit?

    fun get(): Double

    override fun collect(): MetricFamilySamples
}