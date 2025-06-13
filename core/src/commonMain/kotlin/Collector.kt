package io.github.rxfa.prometheus.core

/**
 * Abstract base class for all Prometheus metric collectors.
 *
 * A collector gathers and exposes one or more [MetricFamilySamples], representing the data for
 * a specific metric (e.g., counters, gauges).
 *
 * @property fullName Full metric name (including any suffix or unit).
 * @property help Description of the metric, explaining what it measures.
 * @property labelNames List of label names associated with this metric (e.g., "method", "status").
 * @property unit Unit of the metric (e.g., "seconds", "bytes").
 */
public abstract class Collector(
    public val fullName: String,
    public val help: String,
    public val labelNames: List<String> = emptyList(),
    public val unit: String = "",
) {
    /**
     * The base name of the metric (without unit suffix).
     */
    public abstract val name: String
    /**
     * The metric type (e.g., counter, gauge).
     */
    public abstract val type: Type
    /**
     * Collects the current set of metric samples.
     *
     * @return A [MetricFamilySamples] containing all samples for this metric.
     */
    public abstract fun collect(): MetricFamilySamples
    /**
     * Enum representing the types of Prometheus metrics.
     *
     * @property typeName Name used by Prometheus exposition format.
     */
    public enum class Type(public val typeName: String) {
        COUNTER("counter"),
        UNKNOWN("unknown"),
        GAUGE("gauge"),
    }

    /**
     * Represents a metric family and all of its associated [Sample]s.
     *
     * A metric family groups together samples with the same metric name.
     *
     * @property name Name of the metric.
     * @property unit Unit of measurement (e.g., "seconds").
     * @property type Type of the metric.
     * @property help Help text describing the metric.
     * @property samples List of individual metric samples.
     */
    public class MetricFamilySamples(
        public val name: String,
        public val unit: String = "",
        public val type: Type,
        public val help: String,
        public val samples: List<Sample>
    )

    /**
     * Represents a single measurement of a metric.
     *
     * A sample includes a metric name, associated label pairs, a value, and optionally a timestamp.
     *
     * Example:
     * ```
     * http_requests_total{method="GET", status="200"} 1243 17195259443250043
     * ```
     *
     * @property name Name of the metric.
     * @property labelNames List of label names.
     * @property labelValues List of label values (must match the order and size of [labelNames]).
     * @property value Value of the sample.
     * @property timestamp Optional timestamp in milliseconds (defaults to current time).
     */
    public class Sample(
        public val name: String,
        public val labelNames: List<String>,
        public val labelValues: List<String>,
        public val value: Double,
        public val timestamp: Long = getCurrentMillis()
    )


    /**
     * Registers this collector in the given [CollectorRegistry].
     *
     * @param registry The registry to register into (defaults to [CollectorRegistry.defaultRegistry]).
     * @return This collector instance for chaining.
     */
    public suspend fun register(registry: CollectorRegistry = CollectorRegistry.defaultRegistry): Collector {
        registry.register(this)
        return this
    }

    public companion object {
        private val validBaseUnits =
            setOf(
                "seconds", "celsius", "kelvin", "meters", "bytes", "ratio", "volts", "amperes",
                "joules", "grams"
            )
        private val metricNameRegex = Regex("[a-zA-Z_:][a-zA-Z0-9_:]*")
        private val metricLabelNameRegex = Regex("[a-zA-Z_][a-zA-Z0-9_]*")
        private val reservedMetricLabelNameRegex = Regex("__.*")

        /**
         * Validates a metric name according to Prometheus naming rules.
         *
         * @param name The metric name to check.
         * @throws IllegalArgumentException if the name is invalid.
         */
        public fun checkMetricName(name: String){
            require(metricNameRegex.matches(name)) {
                "Metric name '$name' is not valid."
            }
        }

        /**
         * Validates a metric unit name according to Prometheus naming rules.
         *
         * @param unit The unit name to check.
         * @throws IllegalArgumentException if the unit name is invalid.
         */
        public fun checkUnitName(unit: String){
            require(unit.isEmpty() || unit in validBaseUnits) {
                "Metric name '$unit' is not valid."
            }
        }

        /**
         * Validates a metric label name according to Prometheus naming rules.
         *
         * Label names must match the regular expression `[a-zA-Z_][a-zA-Z0-9_]*`
         * and cannot start with double underscores (`__`).
         *
         * @param labelName The label name to check.
         * @throws IllegalArgumentException if the label name is invalid or reserved.
         */
        public fun checkMetricLabelName(labelName: String){
            require(metricLabelNameRegex.matches(labelName)) { "Metric label name '$labelName' is not valid." }
            require(!reservedMetricLabelNameRegex.matches(labelName)){
                "Metric label name '$labelName' is not valid. Reserved for internal use."
            }
        }
    }
}