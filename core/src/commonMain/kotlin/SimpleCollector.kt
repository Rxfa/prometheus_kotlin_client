package io.github.rxfa.prometheus.core

/**
 * Abstract base class for Prometheus metric types that support labeled children.
 *
 * This class provides common functionality for metric types like [Counter] and [Gauge],
 * including label management, child instance tracking, and sample collection.
 *
 * @param Child The type of the labeled metric child (e.g., [Counter.Child], [Gauge.Child]).
 * @param fullName The full metric name (including suffix, if applicable).
 * @param help A human-readable description of the metric's purpose.
 * @param labelNames The list of label names (must be valid Prometheus identifiers).
 * @param unit The unit of the metric (optional, e.g., "seconds").
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

    /**
     * Map of label value combinations to corresponding [Child] instances.
     */
    protected val childMetrics: MutableMap<List<String>, Child> = mutableMapOf()

    /**
     * Internal reference to the child with no labels (if applicable).
     */
    protected var noLabelsChild: Child? = null

    /**
     * Set of suffixes automatically applied or checked on metric names.
     * Typically used for enforcing Prometheus naming conventions (e.g., `_total` for counters).
     */
    protected abstract val suffixes: Set<String>

    /**
     * Retrieves or creates a [Child] associated with the given label values.
     *
     * @param labelValues Values corresponding to the declared label names.
     * @return The child instance representing this label set.
     * @throws IllegalArgumentException If the number of values does not match the number of labels,
     * or if any label value is blank.
     */
    public fun labels(vararg labelValues: String): Child {
        require(labelValues.size == labelNames.size) { "Incorrect number of labels." }
        require(labelValues.all { it.isNotEmpty() }) { "Label cannot be blank." }
        val key = labelValues.asList()
        return childMetrics.getOrPut(key) { newChild() }
    }

    /**
     * Creates a new child instance. Called internally when a new label set is first seen.
     */
    public abstract fun newChild(): Child

    /**
     * Removes the child instance associated with the given label values.
     *
     * Reinitializes the no-label child if applicable.
     *
     * @param labelValues Label values identifying the child to remove.
     */
    public fun remove(vararg labelValues: String) {
        childMetrics.remove(labelValues.toList())
        initializeNoLabelsChild()
    }

    /**
     * Removes all labeled child instances and resets the no-label child (if applicable).
     */
    public fun clear(){
        childMetrics.clear()
        initializeNoLabelsChild()
    }

    /**
     * Initializes the [noLabelsChild] if no labels are defined for this collector.
     *
     * Used to support unlabeled usage such as:
     * ```
     * myGauge.inc()
     * ```
     */
    protected fun initializeNoLabelsChild(){
        if(labelNames.isEmpty()){
            noLabelsChild = labels()
        }
    }

    /**
     * Utility method to wrap a list of [Sample]s into a [MetricFamilySamples] object.
     *
     * @param samples The list of collected samples from child instances.
     * @return A [MetricFamilySamples] representing this collector’s state.
     */
    protected fun familySamplesList(
        samples: List<Sample>
    ): MetricFamilySamples {
        return MetricFamilySamples(name, unit, type, help, samples)
    }
}