package io.github.kotlin.fibonacci

actual class Counter actual constructor(
    fullName: String,
    help: String,
    labelNames: List<String>,
    unit: String,
    includeCreatedSeries: Boolean
) : SimpleCollector<Counter.Child>(fullName, help, labelNames, unit) {
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
        actual fun inc(amount: Double) {
        }

        actual fun get(): Double {
            TODO("Not yet implemented")
        }

        actual fun created(): Long {
            TODO("Not yet implemented")
        }

    }

    actual fun inc(amount: Double): Unit? {
        TODO("Not yet implemented")
    }

    actual fun get(): Double {
        TODO("Not yet implemented")
    }

    actual override fun collect(): List<MetricFamilySamples> {
        TODO("Not yet implemented")
    }


}