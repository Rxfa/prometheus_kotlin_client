import io.github.kotlin.fibonacci.PrometheusExporter
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class KtorTest{

    @Test
    fun testKtorIntegration(){
        testApplication {
            application {
                installPrometheusMetrics(PrometheusExporter())
            }

            val response = client.get("/metrics")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Text.Plain, response.contentType()!!.withoutParameters())
            assertContains(response.bodyAsText(), "# TYPE http_requests_total counter")
            assertContains(response.bodyAsText(), "# HELP http_requests_total Total HTTP requests received")
            assertContains(response.bodyAsText(), "http_requests_total{} 0.0")
        }
    }

    @Test
    fun testRequestCounter(){
        testApplication {
            application {
                installPrometheusMetrics(PrometheusExporter())
            }

            /**
             * Requests to /metrics do not affect the metrics displayed so we need to make a call to a different
             * endpoint.
             */
            client.get("/")
            val response = client.get("/metrics")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Text.Plain, response.contentType()!!.withoutParameters())
            assertContains(response.bodyAsText(), "# TYPE http_requests_total counter")
            assertContains(response.bodyAsText(), "# HELP http_requests_total Total HTTP requests received")
            assertContains(response.bodyAsText(), "http_requests_total{} 1.0")
        }
    }
}