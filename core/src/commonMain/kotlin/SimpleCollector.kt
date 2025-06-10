package io.github.rxfa.prometheus.core

/**
 * Common functionality for the metric types supported by prometheus.
 */
public abstract class SimpleCollector<Child>(
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

    public fun labels(vararg labelValues: String): Child {
        require(labelValues.size == labelNames.size) { "Incorrect number of labels." }
        require(labelValues.all { it.isNotEmpty() }) { "Label cannot be blank." }
        val key = labelValues.asList()
        return childMetrics.getOrPut(key) { newChild() }
    }

    public abstract fun newChild(): Child

    public fun remove(vararg labelValues: String) {
        childMetrics.remove(labelValues.toList())
        initializeNoLabelsChild()
    }

    public fun clear(){
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
    ): MetricFamilySamples {
        return MetricFamilySamples(name, unit, type, help, samples)
    }
}