package io.github.kotlin.fibonacci

abstract class Collector(
    val fullName: String,
    val help: String,
    val labelNames: List<String> = emptyList(),
    val unit: String = "",
) {
    abstract val name: String

    abstract val type: Type

    abstract fun collect(): MetricFamilySamples

    enum class Type(val typeName: String) {
        COUNTER("counter"),
        UNKNOWN("unknown"),
        GAUGE("gauge"),
    }

    /**
     * A metric and all of its samples.
     * Think of a metric as a category of observation (e.g 'Number of HTTP requests received')
     */
    class MetricFamilySamples(
        val name: String,
        val unit: String = "",
        val type: Type,
        val help: String,
        val samples: List<Sample>
    )

    /**
     * A single Sample, with a unique name and set of labels.
     * Think of a sample as a single measurement of a metric at a given time
     *
     * i.e 'http_requests_total{method="GET", status="200"} 1243 17195259443250043'
     */
    class Sample(
        val name: String,
        val labelNames: List<String>,
        val labelValues: List<String>,
        val value: Double,
        val timestamp: Long = getCurrentMillis()
    )


    suspend fun register(registry: CollectorRegistry = CollectorRegistry.defaultRegistry): Collector {
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