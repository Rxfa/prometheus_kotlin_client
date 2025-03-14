package io.github.kotlin.fibonacci

/**
 * Common functionality for Gauge and Counter.
 */
expect abstract class SimpleCollector<Child>(
    fullName: String,
    help: String,
    labelNames: List<String>
) : Collector {

    val childMetrics: MutableMap<List<String>, Child>
    var noLabelsChild: Child?

    fun labels(vararg labelValues: String): Child

    abstract fun newChild(): Child

    fun remove(vararg labelValues: String)

    fun clear()

    fun initializeNoLabelsChild()

    fun setChild(child: Child, vararg labelValues: String): Collector

    fun familySamplesList(type: Type, samples: List<Sample>): List<MetricFamilySamples>
}