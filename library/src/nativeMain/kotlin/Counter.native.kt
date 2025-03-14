package io.github.kotlin.fibonacci

actual class Counter actual constructor(
    val name: String,
    val help: String,
    val labelNames: List<String>
) : SimpleCollector<Counter.Child>(name, help, labelNames) {
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

    actual override fun collect(): List<MetricFamilySamples> {
        TODO("Not yet implemented")
    }

    override fun getName(): String? {
        TODO("Not yet implemented")
    }

}