package io.github.kotlin.fibonacci

public class PrometheusExporter {
    public val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val parser: PrometheusTextFormat = PrometheusTextFormat()

    public suspend fun scrape(withTimestamp: Boolean = false): String {
        return parser.writeMetrics(registry.collect(), withTimestamp)
    }
}