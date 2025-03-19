package io.github.kotlin.fibonacci

/**
 * Common functionality for Gauge and Counter.
 */
actual abstract class SimpleCollector<Child> actual constructor(
    fullName: String,
    help: String,
    labelNames: List<String>,
    unit: String
) : Collector() {
    actual val childMetrics: MutableMap<List<String>, Child>
        get() = TODO("Not yet implemented")
    actual var noLabelsChild: Child?
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun labels(vararg labelValues: String): Child {
        TODO("Not yet implemented")
    }

    actual abstract fun newChild(): Child
    actual fun remove(vararg labelValues: String) {
    }

    actual fun clear() {
    }

    actual fun initializeNoLabelsChild() {
    }

    actual fun familySamplesList(
        type: Type,
        samples: List<Sample>
    ): List<MetricFamilySamples> {
        TODO("Not yet implemented")
    }

}