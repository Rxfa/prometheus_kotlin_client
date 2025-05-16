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
                installPrometheusMetrics()
            }

            val response = client.get("/metrics")
            val responseBody = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Text.Plain, response.contentType()!!.withoutParameters())
            assertContains(responseBody, "# TYPE http_requests_total counter")
            assertContains(responseBody, "# HELP http_requests_total Total HTTP requests received")
            assertContains(responseBody, "http_requests_total{method=\"GET\",path=\"/metrics\"} 1.0")
        }
    }

    @Test
    fun testTotalHTTPRequestCount(){
        testApplication {
            application {
                installPrometheusMetrics()
            }

            /**
             * Requests to /metrics do not affect the metrics displayed so we need to make a call to a different
             * endpoint.
             */
            client.get("/")
            val response = client.get("/metrics")
            val responseBody = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Text.Plain, response.contentType()!!.withoutParameters())
            assertContains(responseBody, "# TYPE http_requests_total counter")
            assertContains(responseBody, "# HELP http_requests_total Total HTTP requests received")
            assertContains(responseBody, "http_requests_total{method=\"GET\",path=\"/metrics\"} 1.0")
        }
    }

    @Test
    fun testTotalErrorCount(){
        testApplication {
            application {
                installPrometheusMetrics()
            }

            client.get("/non-existing-path").apply {
                assertEquals(HttpStatusCode.NotFound, status)
            }
            val response = client.get("/metrics")
            val responseBody = response.bodyAsText()
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Text.Plain, response.contentType()!!.withoutParameters())
            assertContains(responseBody, "# TYPE http_requests_errors_total counter")
            assertContains(responseBody, "# HELP http_requests_errors_total Total HTTP requests errors")
            assertContains(
                responseBody,
                "http_requests_errors_total{method=\"GET\",status_code=\"404\",path=\"/non-existing-path\"} 1.0"
            )
        }
    }
}
