package io.github.kotlin.fibonacci

public abstract class Collector(
    public val fullName: String,
    public val help: String,
    public val labelNames: List<String> = emptyList(),
    public val unit: String = "",
) {

    public abstract val name: String

    public abstract val type: Type

    public abstract fun collect(): MetricFamilySamples

    public enum class Type(public val typeName: String) {
        COUNTER("counter"),
        UNKNOWN("unknown"),
        GAUGE("gauge"),
        HISTOGRAM("histogram"),
    }

    /**
     * A metric and all of its samples.
     * Think of a metric as a category of observation (e.g 'Number of HTTP requests received')
     */
    public class MetricFamilySamples(
        public val name: String,
        public val unit: String = "",
        public val type: Type,
        public val help: String,
        public val samples: List<Sample>
    )

    /**
     * A single Sample, with a unique name and set of labels.
     * Think of a sample as a single measurement of a metric at a given time
     *
     * i.e 'http_requests_total{method="GET", status="200"} 1243 17195259443250043'
     */
    public class Sample(
        public val name: String,
        public val labelNames: List<String>,
        public val labelValues: List<String>,
        public val value: Double,
        public val timestamp: Long = getCurrentMillis()
    )


    public suspend fun register(registry: CollectorRegistry = CollectorRegistry.defaultRegistry): Collector {
        registry.register(this)
        return this
    }

    public companion object {
        private val metricNameRegex = Regex("[a-zA-Z_:][a-zA-Z0-9_:]*")
        private val metricLabelNameRegex = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
        private val reservedMetricLabelNameRegex = Regex("__.*")

        public fun checkMetricName(name: String){
            require(metricNameRegex.matches(name)) {
                "Metric name '$name' is not valid."
            }
        }

        public fun checkMetricLabelName(labelName: String){
            require(metricLabelNameRegex.matches(labelName)) { "Metric label name '$labelName' is not valid." }
            require(!reservedMetricLabelNameRegex.matches(labelName)){
                "Metric label name '$labelName' is not valid. Reserved for internal use."
            }
        }
    }
}