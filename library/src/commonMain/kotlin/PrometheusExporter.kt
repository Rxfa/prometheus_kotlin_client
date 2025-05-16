package io.github.kotlin.fibonacci

class PrometheusExporter {
    val registry = CollectorRegistry.defaultRegistry
    private val parser = PrometheusTextFormat()

    suspend fun scrape(withTimestamp: Boolean = false) = parser.writeMetrics(registry.collect(), withTimestamp)
}