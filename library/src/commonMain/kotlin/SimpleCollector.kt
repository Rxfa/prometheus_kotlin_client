package io.github.kotlin.fibonacci

/**
 * Common functionality for the metric types supported by prometheus.
 */
abstract class SimpleCollector<Child>(
    fullName: String,
    help: String,
    labelNames: List<String>,
    unit: String
) : Collector(fullName, help, labelNames, unit) {
    init {
        checkMetricName(fullName)
        labelNames.forEach{ checkMetricLabelName(it) }
    }

    protected val childMetrics: MutableMap<List<String>, Child> = mutableMapOf()
    protected var noLabelsChild: Child? = null
    protected abstract val suffixes: Set<String>

    fun labels(vararg labelValues: String): Child {
        require(labelValues.size == labelNames.size) { "Incorrect number of labels." }
        require(labelValues.all { it.isNotEmpty() }) { "Label cannot be blank." }
        val key = labelValues.asList()
        return childMetrics.getOrPut(key) { newChild() }
    }

    abstract fun newChild(): Child

    fun remove(vararg labelValues: String) {
        childMetrics.remove(labelValues.toList())
        initializeNoLabelsChild()
    }

    fun clear(){
        childMetrics.clear()
        initializeNoLabelsChild()
    }

    protected fun initializeNoLabelsChild(){
        if(labelNames.isEmpty()){
            noLabelsChild = labels()
        }
    }

    protected fun familySamplesList(
        samples: List<Sample>
    ): List<MetricFamilySamples> {
        return mutableListOf(
            MetricFamilySamples(name, unit, type, help, samples)
        )
    }
}