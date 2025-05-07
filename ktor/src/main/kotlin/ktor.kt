import io.github.kotlin.fibonacci.CollectorRegistry
import io.github.kotlin.fibonacci.PrometheusExporter
import io.github.kotlin.fibonacci.Counter
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.installPrometheusMetrics(exporter: PrometheusExporter){
    val ktorMetrics = KtorMetrics(exporter.registry)
    ktorMetrics.setupMonitoring(this)

    routing {
        get("/metrics") {
            call.respondText(exporter.scrape(), ContentType.Text.Plain)
        }
    }
}

private class KtorMetrics(private val registry: CollectorRegistry){
    private val requestCounter = Counter("http_requests_total", "Total HTTP requests received")

    init {
        runBlocking {
           registry.register(requestCounter)
        }
    }

    fun setupMonitoring(application: Application) {
        application.intercept(ApplicationCallPipeline.Monitoring) {
            proceed()
            requestCounter.inc()
        }
    }
}