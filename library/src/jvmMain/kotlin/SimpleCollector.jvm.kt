package io.github.kotlin.fibonacci

/**
 * Common functionality for all Prometheus metric types.
 */
actual abstract class SimpleCollector<Child> actual constructor(
    val fullName: String,
    val help: String,
    val labelNames: List<String>,
    val unit: String
) : Collector() {
    init {
        checkMetricName(fullName)
        labelNames.forEach{ checkMetricLabelName(it) }
    }

    actual val childMetrics: MutableMap<List<String>, Child> = mutableMapOf()
    actual var noLabelsChild: Child? = null

    actual fun labels(vararg labelValues: String): Child {
        require(labelValues.size == labelNames.size) { "Incorrect number of labels." }
        require(labelValues.all { it.isNotEmpty() }) { "Label cannot be blank." }
        val key = labelValues.asList()
        return childMetrics.getOrPut(key) { newChild() }
    }

    actual abstract fun newChild(): Child

    actual fun remove(vararg labelValues: String) {
        childMetrics.remove(labelValues.toList())
        initializeNoLabelsChild()
    }

    actual fun clear(){
        childMetrics.clear()
        initializeNoLabelsChild()
    }

    actual fun initializeNoLabelsChild(){
        if(labelNames.isEmpty()){
            noLabelsChild = labels()
        }
    }

    actual fun familySamplesList(
        type: Type,
        samples: List<Sample>
    ): List<MetricFamilySamples> {
        return mutableListOf(
            MetricFamilySamples(fullName, unit, type, help, samples)
        )
    }

}