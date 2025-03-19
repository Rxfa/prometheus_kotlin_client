package io.github.kotlin.fibonacci

actual abstract class Collector {
    actual abstract fun collect(): List<MetricFamilySamples>

    actual enum class Type {
        COUNTER
    }

    /**
     * A metric and all of its samples.
     */
    actual class MetricFamilySamples actual constructor(
        val name: String,
        val unit: String?,
        val type: Type,
        val help: String,
        val samples: List<Sample>
    )

    /**
     * A single Sample, with a unique name and set of labels.
     */
    actual class Sample actual constructor(
        val name: String,
        val labelNames: List<String>,
        val labelValues: List<String>,
        val value: Double,
        val timestamp: Long
    ){
        constructor(
            name: String,
            labelNames: List<String>,
            labelValues: List<String>,
            value: Double,
        ): this(name, labelNames, labelValues, value, System.currentTimeMillis())
    }

    actual suspend fun register(registry: CollectorRegistry): Collector {
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