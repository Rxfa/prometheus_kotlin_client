package io.github.kotlin.fibonacci

actual class Counter actual constructor(
    name: String,
    help: String,
    labelNames: List<String>,
    unit: String
) : SimpleCollector<Counter.Child>(name, help, labelNames, unit) {
    actual override fun newChild(): Child {
        TODO("Not yet implemented")
    }

    actual class Child {
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

    actual override fun collect(): List<Collector.MetricFamilySamples> {
        TODO("Not yet implemented")
    }


}