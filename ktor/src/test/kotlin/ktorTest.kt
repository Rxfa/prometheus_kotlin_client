import io.github.rxfa.prometheus.core.CollectorRegistry
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.github.rxfa.prometheus.ktor.installPrometheusMetrics
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.test.runTest

class KtorTest{
    @BeforeTest
    fun clearMetrics() {
        runTest {
            CollectorRegistry.defaultRegistry.clear()
        }
    }

    @Test
    fun `metrics endpoint is exposed and returns data`() = testApplication {
        application {
            installPrometheusMetrics()

            routing {
                get("/hello") {
                    call.respondText("Hello")
                }
            }
        }

        client.get("/metrics").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertContains(bodyAsText(), "http_requests_total")
        }

        client.get("/hello").apply {
            assertEquals(HttpStatusCode.OK, status)
        }

    }

    @Test
    fun `custom metricsPath is respected`() = testApplication {
        application {
            installPrometheusMetrics {
                metricsPath = "/custom-metrics"
            }
        }

        assertEquals(HttpStatusCode.OK, client.get("/custom-metrics").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/metrics").status)
    }

    @Test
    fun `exposeEndpoint false disables metrics endpoint`() = testApplication {
        application {
            installPrometheusMetrics {
                exposeEndpoint = false
            }
        }

        assertEquals(HttpStatusCode.NotFound, client.get("/metrics").status)
    }


    @Test
    fun `metrics output includes timestamp when includeTimestamp is true`() = testApplication {
        application {
            installPrometheusMetrics {
                includeTimestamp = true
            }
        }

        val metrics = client.get("/metrics").bodyAsText()
        val expectedMetricLine = Regex("""http_requests_total\{method="GET"\,path="/metrics"} 1\.0 \d+""")

        assertTrue(expectedMetricLine.containsMatchIn(metrics))
    }

    @Test
    fun `exceptions are recorded in metrics`() = testApplication {
        application {
            installPrometheusMetrics()

            routing {
                get("/boom") {
                    error("Crash!")
                }
            }
        }

        client.get("/boom")

        val metrics = client.get("/metrics").bodyAsText()

        assertContains(metrics, "http_exceptions_total{method=\"GET\",path=\"/boom\",exception_class=\"IllegalStateException\"} 1.0")
    }

    @Test
    fun `dynamic path segments are normalized`() = testApplication {
        application {
            installPrometheusMetrics()

            routing {
                get("/users/{id}/profile") {
                    call.respondText("Profile")
                }
            }
        }

        client.get("/users/123/profile")

        val metrics = client.get("/metrics").bodyAsText()
        assertContains(metrics, """http_requests_total{method="GET",path="/users/{param}/profile"} 1.0""")
    }
}
