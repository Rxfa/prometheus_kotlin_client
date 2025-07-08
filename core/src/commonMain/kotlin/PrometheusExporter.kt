package io.github.rxfa.prometheus.core

/**
 * Exports Prometheus metrics from a [CollectorRegistry] in the Prometheus text exposition format.
 *
 * The [PrometheusExporter] is responsible for producing a textual representation of all
 * registered metrics that can be scraped by Prometheus or exposed through an HTTP endpoint.
 */
public class PrometheusExporter {
    /**
     * The [CollectorRegistry] from which metrics are collected.
     *
     * Defaults to [CollectorRegistry.defaultRegistry].
     */
    public val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val parser: PrometheusTextFormat = PrometheusTextFormat()

    /**
     * Scrapes all current metrics from the [registry] and encodes them in Prometheus text format.
     *
     * This method is suspendable and safe to use in coroutine-based environments.
     *
     * Example usage in a Ktor route:
     * ```
     * get("/metrics") {
     *     call.respondText(PrometheusExporter().scrape(), ContentType.Text.Plain)
     * }
     * ```
     *
     * @param withTimestamp If `true`, includes timestamps in the output for each sample.
     * @return A string containing the full Prometheus-formatted exposition of all collected metrics.
     */
    public suspend fun scrape(withTimestamp: Boolean = false): String {
        return parser.writeMetrics(registry.collect(), withTimestamp)
    }
}
