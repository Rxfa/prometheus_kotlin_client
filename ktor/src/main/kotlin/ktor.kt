import io.github.kotlin.fibonacci.CollectorRegistry
import io.github.kotlin.fibonacci.PrometheusExporter
import io.github.kotlin.fibonacci.counter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

fun Application.installPrometheusMetrics(exporter: PrometheusExporter = PrometheusExporter()) {
    val ktorMetrics = KtorMetrics(exporter.registry)
    ktorMetrics.setupMonitoring(this)

    routing {
        get("/metrics") {
            call.respondText(exporter.scrape(), ContentType.Text.Plain)
        }
    }
}

private class KtorMetrics(private val registry: CollectorRegistry) {
    private val totalRequests = counter("http_requests_total") {
        help("Total HTTP requests received")
        labelNames("method", "path")
    }
    private val totalErrors = counter("http_requests_errors_total") {
        help("Total HTTP requests errors")
        labelNames("method", "status_code", "path")
    }
    private val totalExceptions = counter("http_exceptions_total") {
        help("Total HTTP exceptions")
        labelNames("method", "path", "exception_class")
    }

    init {
        runBlocking {
            registry.register(totalRequests)
            registry.register(totalErrors)
            registry.register(totalExceptions)
        }
    }

    fun setupMonitoring(application: Application) {

        application.intercept(ApplicationCallPipeline.Monitoring) {
            val method = call.request.httpMethod.value
            val path = call.request.uri
            totalRequests.labels(method, path).inc()
            proceed()
        }

        // Runs after the entire request pipeline, including interceptors.
        application.install(StatusPages) {
            exception<Throwable> { call, cause ->
                val method = call.request.httpMethod.value
                val path = call.request.uri
                val exceptionClass = cause::class.simpleName ?: "UnknownException"
                totalExceptions.labels(method, path, exceptionClass).inc()
            }

            val httpStatusCodes = HttpStatusCode.allStatusCodes.filter { it.value in 400..500 }.toTypedArray()
            status(*httpStatusCodes) { call, status ->
                val method = call.request.httpMethod.value
                val path = call.request.uri
                val statusCode = status.value.toString()
                totalErrors.labels(method, statusCode, path).inc()
            }
        }
    }
}