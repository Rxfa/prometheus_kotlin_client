package io.github.rxfa.prometheus.ktor

import io.github.rxfa.prometheus.core.CollectorRegistry
import io.github.rxfa.prometheus.core.PrometheusExporter
import io.github.rxfa.prometheus.core.counter
import io.github.rxfa.prometheus.core.exponentialHistogramBuckets
import io.github.rxfa.prometheus.core.gauge
import io.github.rxfa.prometheus.core.histogram
import io.github.rxfa.prometheus.core.linearHistogramBuckets
import io.github.rxfa.prometheus.core.quantile
import io.github.rxfa.prometheus.core.quantiles
import io.github.rxfa.prometheus.core.summary
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking

/**
 * Configuration options for Prometheus metrics installation in a Ktor application.
 *
 * @property exposeEndpoint If `true`, enables the `/metrics` HTTP endpoint for Prometheus scraping.
 * Defaults to `true`.
 * @property metricsPath The HTTP path where Prometheus metrics will be exposed. Defaults to `/metrics`.
 * @property includeTimestamp If `true`, each exported metric will include a timestamp alongside its value.
 * This can be useful for certain Prometheus setups or debugging scenarios. Defaults to `false`.
 */
class PrometheusConfig {
    var exposeEndpoint: Boolean = true
    var metricsPath: String = "/metrics"
    var includeTimestamp: Boolean = false
}

/**
 * Installs Prometheus metrics collection into the Ktor [Application].
 *
 * This function sets up instrumentation to collect HTTP request metrics and exposes them
 * via a configurable HTTP endpoint for Prometheus to scrape.
 *
 * **Metrics collected:**
 * - `http_requests_total{method, path}`: total count of all HTTP requests received
 * - `http_requests_errors_total{method, status_code, path}`: total count of HTTP error responses (status 400-500)
 * - `http_exceptions_total{method, path, exception_class}`: total count of exceptions thrown during request handling
 *
 * **Endpoint:**
 * - Exposes metrics at the path specified in [PrometheusConfig.metricsPath] (default `/metrics`).
 *
 * @param exporter Optional [PrometheusExporter] instance to use for metrics registration and scraping.
 * Defaults to a new exporter with the default collector registry.
 * @param configure Lambda to configure [PrometheusConfig] options such as endpoint path and exposure.
 */
fun Application.installPrometheusMetrics(
    exporter: PrometheusExporter = PrometheusExporter(),
    configure: PrometheusConfig.() -> Unit = {},
) {
    val config = PrometheusConfig().apply(configure)
    val ktorMetrics = KtorMetrics(exporter.registry)
    ktorMetrics.setupMonitoring(this)

    if (config.exposeEndpoint) {
        routing {
            get(config.metricsPath) {
                call.respondText(exporter.scrape(config.includeTimestamp), ContentType.Text.Plain)
            }
        }
    }
}

/**
 * Internal class responsible for registering and updating Prometheus metrics related to
 * Ktor HTTP request lifecycle.
 *
 * @property registry The [CollectorRegistry] to register metrics with.
 */
private class KtorMetrics(
    private val registry: CollectorRegistry,
) {
    private val totalRequests =
        counter("http_requests_total") {
            help("Total HTTP requests received")
            labelNames("method", "path")
        }
    private val totalErrors =
        counter("http_requests_errors_total") {
            help("Total HTTP requests errors")
            labelNames("method", "status_code", "path")
        }
    private val totalExceptions =
        counter("http_exceptions_total") {
            help("Total HTTP exceptions")
            labelNames("method", "path", "exception_class")
        }

    private val currentConnections =
        gauge("http_current_connections") {
            help("Current number of connections")
            labelNames("status")
        }

    private val httpLatencyHistogram =
        histogram("http_request_duration_seconds_histogram") {
            help("Duration of HTTP requests in seconds")
            labelNames("method", "endpoint")
        }

    private val httpLatencyCustomLinear =
        linearHistogramBuckets("http_request_duration_custom_linear_buckets_seconds", {
            help("Quantiles of HTTP request durations in seconds")
            labelNames("method", "endpoint")
        }, 0.0, 2.0, 10)

    private val httpLatencyCustomExponential =
        exponentialHistogramBuckets("http_request_duration_custom_exponential_buckets_seconds", {
            help("Quantiles of HTTP request durations in seconds")
            labelNames("method", "endpoint")
        }, 2.0, 2.0, 5)

    private val httpLatencySummary =
        summary("http_request_duration_seconds_summary") {
            help("Duration of HTTP requests in seconds")
            labelNames("method")
            quantiles(
                // Median with 5% error
                quantile(0.5, 0.05),
                // 95th percentile with 1% error
                quantile(0.95, 0.01),
                // 99th percentile with 0.1% error
                quantile(0.99, 0.001),
            )
        }

    init {
        runBlocking {
            registry.register(totalRequests)
            registry.register(totalErrors)
            registry.register(totalExceptions)
            registry.register(currentConnections)
            registry.register(httpLatencyHistogram)
            registry.register(httpLatencyCustomLinear)
            registry.register(httpLatencyCustomExponential)
            registry.register(httpLatencySummary)
        }
    }

    fun setupMonitoring(application: Application) {

        application.intercept(ApplicationCallPipeline.Monitoring) {
            val method = call.request.httpMethod.value
            val path = normalizePath(call.request.path())
            totalRequests.labels(method, path).inc()

            val startTime = System.nanoTime()
            currentConnections.labels("active").inc()
            proceed()
            currentConnections.labels("active").dec()
            val durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
            httpLatencyHistogram.labels(method, path).observe(durationSeconds)
            httpLatencyCustomLinear.labels(method, path).observe(durationSeconds)
            httpLatencyCustomExponential.labels(method, path).observe(durationSeconds)
            httpLatencySummary.labels(method).observe(durationSeconds)

        }

        // Install status pages to intercept exceptions and error status codes
        application.install(StatusPages) {
            exception<Throwable> { call, cause ->
                val method = call.request.httpMethod.value
                val path = normalizePath(call.request.path())
                val exceptionClass = cause::class.simpleName ?: "UnknownException"
                totalExceptions.labels(method, path, exceptionClass).inc()
            }

            val httpStatusCodes = HttpStatusCode.allStatusCodes.filter { it.value in 400 until 500 }.toTypedArray()
            status(*httpStatusCodes) { call, status ->
                val method = call.request.httpMethod.value
                val path = normalizePath(call.request.path())
                val statusCode = status.value.toString()
                totalErrors.labels(method, statusCode, path).inc()
            }
        }
    }
}

/**
 * Normalizes a URI path by replacing numeric IDs or UUIDs in path segments with `{param}`.
 *
 * Example:
 * - `/users/123/profile` -> `/users/{param}/profile`
 * - `/orders/550e8400-e29b-41d4-a716-446655440000/details` -> `/orders/{param}/details`
 */
private fun normalizePath(path: String): String =
    path
        .split("/")
        .joinToString("/") { segment ->
            when {
                segment.matches(Regex("\\d+")) -> "{param}" // numeric IDs
                segment.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) -> "{param}" // UUID
                else -> segment
            }
        }
