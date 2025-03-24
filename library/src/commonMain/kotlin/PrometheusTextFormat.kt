package io.github.kotlin.fibonacci

class PrometheusTextFormat {
    fun writeMetrics(collectors: List<Collector>, withTimestamp: Boolean = false): String {
        val stringBuilder = StringBuilder()
        for (collector in collectors) {
            stringBuilder.writeMetricMetadata(collector)
            when (collector) {
                is Counter -> stringBuilder.writeCounter(collector, withTimestamp)
                else -> stringBuilder.writeCustomCollector(collector, withTimestamp)
            }
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

    private fun StringBuilder.writeCustomCollector(collector: Collector, withTimestamp: Boolean) {
        TODO()
    }

    private fun StringBuilder.writeCounter(collector: Counter, withTimestamp: Boolean) {
        val metricFamily = collector.collect()
        for (metric in metricFamily) {
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

    companion object {
        const val CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8"
    }
}