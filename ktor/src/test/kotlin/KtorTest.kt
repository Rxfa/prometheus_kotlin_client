import io.github.rxfa.prometheus.core.CollectorRegistry
import io.github.rxfa.prometheus.ktor.installPrometheusMetrics
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorTest {
    @BeforeTest
    fun clearMetrics() {
        runTest {
            CollectorRegistry.defaultRegistry.clear()
        }
    }

    @Test
    fun `metrics endpoint is exposed and returns data`() =
        testApplication {
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
    fun `custom metricsPath is respected`() =
        testApplication {
            application {
                installPrometheusMetrics {
                    metricsPath = "/custom-metrics"
                }
            }

            assertEquals(HttpStatusCode.OK, client.get("/custom-metrics").status)
            assertEquals(HttpStatusCode.NotFound, client.get("/metrics").status)
        }

    @Test
    fun `exposeEndpoint false disables metrics endpoint`() =
        testApplication {
            application {
                installPrometheusMetrics {
                    exposeEndpoint = false
                }
            }

            assertEquals(HttpStatusCode.NotFound, client.get("/metrics").status)
        }

    @Test
    fun `metrics output includes timestamp when includeTimestamp is true`() =
        testApplication {
            application {
                installPrometheusMetrics {
                    includeTimestamp = true
                }
            }

            val metrics = client.get("/metrics").bodyAsText()
            val expectedMetricLine = Regex("""http_requests_total\{method="GET",path="/metrics"} 1\.0 \d+""")

            assertTrue(expectedMetricLine.containsMatchIn(metrics))
        }

    @Test
    fun `exceptions are recorded in metrics`() =
        testApplication {
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
    fun `dynamic path segments are normalized`() =
        testApplication {
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

    @Test
    fun `current users gauge is updated`() =
        testApplication {
            application {
                installPrometheusMetrics()

                routing {
                    get("/active") {
                        call.respondText("Active")
                    }
                }
            }

            client.get("/active").apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            val metrics = client.get("/metrics").bodyAsText()
            assertContains(metrics, """http_current_connections{status="active"} 1.0""")
        }

    @Test
    fun `http request duration histogram is updated`() =
        testApplication {
            application {
                installPrometheusMetrics()

                routing {
                    get("/histogram") {
                        call.respondText("Histogram")
                    }
                }
            }

            client.get("/histogram").apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            val metrics = client.get("/metrics").bodyAsText()
            assertContains(metrics, """http_request_duration_seconds_histogram""")
        }

    @Test
    fun `http request duration custom linear buckets are updated`() =
        testApplication {
            application {
                installPrometheusMetrics()

                routing {
                    get("/linear") {
                        call.respondText("Linear")
                    }
                }
            }

            client.get("/linear").apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            val metrics = client.get("/metrics").bodyAsText()
            assertContains(metrics, """http_request_duration_custom_linear_buckets_seconds""")
        }

    @Test
    fun `http request duration custom exponential buckets are updated`() =
        testApplication {
            application {
                installPrometheusMetrics()

                routing {
                    get("/exponential") {
                        call.respondText("Exponential")
                    }
                }
            }

            client.get("/exponential").apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            val metrics = client.get("/metrics").bodyAsText()
            assertContains(metrics, """http_request_duration_custom_exponential_buckets_seconds""")
        }

    @Test
    fun `http request duration summary is updated`() =
        testApplication {
            application {
                installPrometheusMetrics()

                routing {
                    get("/summary") {
                        call.respondText("Summary")
                    }
                }
            }

            client.get("/summary").apply {
                assertEquals(HttpStatusCode.OK, status)
            }

            val metrics = client.get("/metrics").bodyAsText()
            assertContains(metrics, """http_request_duration_seconds_summary""")
        }
}
