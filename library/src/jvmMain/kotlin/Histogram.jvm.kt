package io.github.kotlin.fibonacci

actual class Histogram actual constructor(
    fullName: String,
    help: String,
    labelNames: List<String>,
    unit: String,
    includeCreatedSeries: Boolean,
    buckets: List<Double>
) : SimpleCollector<Histogram.Child>(fullName, help, labelNames, unit) {
    override val suffixes: Set<String>
        get() = TODO("Not yet implemented")

    actual override fun newChild(): Child {
        TODO("Not yet implemented")
    }

    override val name: String
        get() = TODO("Not yet implemented")
    override val type: Type
        get() = TODO("Not yet implemented")

    actual inner class Child {
        actual fun observe(value: Double) {
            if (value < 0) {
                throw IllegalArgumentException("Value must be positive")
            }
            TODO ("FINISH NEED THE BUFFER FIRST")
        }

        actual fun get(): Double {
            TODO("Not yet implemented")
        }

        actual fun created(): Long {
            TODO("Not yet implemented")
        }

    }

    actual fun observe(value: Double): Unit? {
        return noLabelsChild?.observe(value)
    }

    actual fun get(): Double {
        return noLabelsChild?.get() ?: 0.0
    }

    actual override fun collect(): List<MetricFamilySamples> {
        return familySamplesList(emptyList())
    }

}