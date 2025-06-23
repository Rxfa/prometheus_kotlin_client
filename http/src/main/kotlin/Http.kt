package io.github.rxfa.prometheus.http

import io.github.rxfa.prometheus.ktor.installPrometheusMetrics
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * Configuration for the Prometheus metrics endpoint.
 */
class MetricsConfig {
    var path: String = "/metrics"
    var includeTimestamp: Boolean = false
}

/**
 * Configuration for launching an embedded Prometheus metrics HTTP server.
 */
class PrometheusHttpServerConfig {
    var port: Int = 8080
    val metrics: MetricsConfig = MetricsConfig()

    fun metrics(block: MetricsConfig.() -> Unit) = metrics.apply(block)
}

/**
 * Launches an embedded HTTP server exposing Prometheus metrics.
 */
fun httpServer(
    block: PrometheusHttpServerConfig.() -> Unit = {},
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val config = PrometheusHttpServerConfig().apply(block)
    return embeddedServer(Netty, port = config.port) {
        installPrometheusMetrics {
            metricsPath = config.metrics.path
            includeTimestamp = config.metrics.includeTimestamp
        }
    }.apply { start() }
}
