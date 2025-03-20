package io.github.kotlin.fibonacci

abstract class Collector(
    val fullName: String,
    val help: String,
    val labelNames: List<String> = emptyList(),
    val unit: String = "",
) {
    abstract val name: String

    abstract val type: Type

    abstract fun collect(): List<MetricFamilySamples>

    enum class Type(val typeName: String) {
        COUNTER("counter"),
        UNKNOWN("unknown"),
    }

    /**
     * A metric and all of its samples.
     */
    class MetricFamilySamples(
        val name: String,
        val unit: String?,
        val type: Type,
        val help: String,
        val samples: List<Sample>
    )

    /**
     * A single Sample, with a unique name and set of labels.
     */
    class Sample(
        val name: String,
        val labelNames: List<String>,
        val labelValues: List<String>,
        val value: Double,
        val timestamp: Long = getCurrentTime()
    )


    suspend fun register(registry: CollectorRegistry): Collector {
        registry.register(this)
        return this
    }

    companion object {
        private val metricNameRegex = Regex("[a-zA-Z_:][a-zA-Z0-9_:]*")
        private val metricLabelNameRegex = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
        private val reservedMetricLabelNameRegex = Regex("__.*")

        fun checkMetricName(name: String) = require(metricNameRegex.matches(name)) { "Metric name '$name' is not valid." }

        fun checkMetricLabelName(labelName: String){
            require(metricLabelNameRegex.matches(labelName)) { "Metric label name '$labelName' is not valid." }
            require(!reservedMetricLabelNameRegex.matches(labelName)){
                "Metric label name '$labelName' is not valid. Reserved for internal use."
            }
        }
    }
}