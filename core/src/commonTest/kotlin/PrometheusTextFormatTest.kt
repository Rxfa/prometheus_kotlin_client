import io.github.rxfa.prometheus.core.Counter
import io.github.rxfa.prometheus.core.PrometheusTextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrometheusTextFormatTest {
    private val prometheusTextFormat = PrometheusTextFormat()

    @Test
    fun `writeMetrics should handle empty collectors list`() {
        val result = prometheusTextFormat.writeMetrics(emptyList(), withTimestamp = false)
        assertEquals("", result.trim())
    }

    @Test
    fun `writeMetrics should correctly format a metric without labels`() {
        val counter = Counter("requests", "Total requests")
        counter.inc(10.0)

        val result = prometheusTextFormat.writeMetrics(listOf(counter))

        val expected = """
            # TYPE requests_total counter
            # HELP requests_total Total requests
            requests_total{} 10.0
        """.trimIndent()

        assertEquals(expected, result.trim())
    }

    @Test
    fun `writeMetrics should return valid Prometheus output for Counter`() {
        val counter = Counter(fullName = "http_requests_total", help = "Total HTTP requests", labelNames = listOf("method"))
        counter.labels("GET").inc(100.0)

        val result = prometheusTextFormat.writeMetrics(listOf(counter))

        val expected = """
            # TYPE http_requests_total counter
            # HELP http_requests_total Total HTTP requests
            http_requests_total{method="GET"} 100.0
        """.trimIndent()

        assertEquals(expected, result.trim())
    }

    @Test
    fun `writeMetrics should include timestamp when withTimestamp is true`() {
        val counter = Counter(fullName = "http_requests_total", help = "Total HTTP requests", labelNames = listOf("method"))
        counter.labels("GET").inc(100.0)

        val result = prometheusTextFormat.writeMetrics(collectors = listOf(counter), withTimestamp = true)

        val expectedMetricLine = Regex("""http_requests_total\{method="GET"\} 100\.0 \d+""")
        assertTrue(result.contains("# TYPE http_requests_total counter"), "Type metadata missing")
        assertTrue(result.contains("# HELP http_requests_total Total HTTP requests"), "Help metadata missing")
        assertTrue(expectedMetricLine.containsMatchIn(result), "Timestamp not found in metric output")
    }

    @Test
    fun `should correctly format multiple metrics together`() {
        val counter1 = Counter("http_requests", "Total HTTP requests", listOf("method"))
        val counter2 = Counter("disk_writes", "Total disk writes")

        counter1.labels("GET").inc(5.0)
        counter1.labels("POST").inc(3.0)
        counter2.inc(10240.0)

        val result = prometheusTextFormat.writeMetrics(listOf(counter1, counter2))

        val expected = """
            # TYPE http_requests_total counter
            # HELP http_requests_total Total HTTP requests
            http_requests_total{method="GET"} 5.0
            http_requests_total{method="POST"} 3.0
            
            # TYPE disk_writes_total counter
            # HELP disk_writes_total Total disk writes
            disk_writes_total{} 10240.0
        """.trimIndent()

        assertEquals(expected, result.trim())
    }
}