package io.github.rxfa.prometheus.core

/**
 * A Prometheus text format encoder (version 0.0.4).
 *
 * This class is responsible for converting a list of [Collector]s into
 * a textual representation that conforms to the Prometheus exposition format.
 *
 * It is typically used by [PrometheusExporter] to expose metrics over HTTP.
 */
public class PrometheusTextFormat {

    /**
     * Writes the metrics collected from the provided [collectors] into the
     * Prometheus text exposition format.
     *
     * This format is compatible with Prometheus' `/metrics` HTTP scraping interface.
     *
     * Example output:
     * ```
     * # TYPE http_requests_total counter
     * # HELP http_requests_total Total number of HTTP requests
     * http_requests_total{method="GET", status="200"} 1243
     * ```
     *
     * @param collectors The list of [Collector] instances to serialize.
     * @param withTimestamp If `true`, appends the collection timestamp to each sample line.
     * @return A string in Prometheus-compatible text format.
     */
    public fun writeMetrics(collectors: List<Collector>, withTimestamp: Boolean = false): String {
        val stringBuilder = StringBuilder()
        for (collector in collectors) {
            stringBuilder.writeMetricMetadata(collector)
            stringBuilder.writeMetricData(collector, withTimestamp)
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    /**
     * Writes metric metadata such as `# TYPE`, `# UNIT`, and `# HELP` to the output.
     *
     * @receiver The [StringBuilder] that receives the formatted metadata.
     * @param collector The [Collector] whose metadata is written.
     */
    private fun StringBuilder.writeMetricMetadata(collector: Collector) {
        this.append("# TYPE ${collector.name} ${collector.type.typeName}\n")
        if(collector.unit.isNotBlank()) {
            this.append("# UNIT ${collector.name} ${collector.unit}\n")
        }
        this.append("# HELP ${collector.name} ${collector.help}\n")
    }

    /**
     * Writes the actual sample data for a given [Collector].
     *
     * Each line represents a labeled sample (time series) with an optional timestamp.
     *
     * @receiver The [StringBuilder] to append sample data to.
     * @param collector The [Collector] whose samples are written.
     * @param withTimestamp Whether to include a millisecond-precision timestamp.
     */
    private fun StringBuilder.writeMetricData(collector: Collector, withTimestamp: Boolean) {
        val metric = collector.collect()
        for(sample in metric.samples) {
            val sampleLabelValues = sample.labelValues.map { doubleQuoteString(it) }
            val labels = (sample.labelNames zip sampleLabelValues)
                .joinToString(prefix = "{", separator = ",", postfix = "}") { (key, value) -> "$key=$value" }
            if(withTimestamp) {
                this.append("${sample.name}$labels ${sample.value} ${sample.timestamp}\n")
            } else {
                this.append("${sample.name}$labels ${sample.value}\n")
            }
        }
    }
}