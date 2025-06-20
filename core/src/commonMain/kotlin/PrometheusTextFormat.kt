package io.github.rxfa.prometheus.core

public class PrometheusTextFormat {
    public fun writeMetrics(collectors: List<Collector>, withTimestamp: Boolean = false): String {
        val stringBuilder = StringBuilder()
        for (collector in collectors) {
            stringBuilder.writeMetricMetadata(collector)
            stringBuilder.writeMetricData(collector, withTimestamp)
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    private fun StringBuilder.writeMetricMetadata(collector: Collector) {
        this.append("# TYPE ${collector.name} ${collector.type.typeName}\n")
        if(collector.unit.isNotBlank()) {
            this.append("# UNIT ${collector.name} ${collector.unit}\n")
        }
        this.append("# HELP ${collector.name} ${collector.help}\n")
    }

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