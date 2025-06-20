
import io.github.rxfa.prometheus.core.quantile
import io.github.rxfa.prometheus.core.quantiles
import io.github.rxfa.prometheus.core.summary
import io.github.rxfa.prometheus.core.summaryQuantiles
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest

class SummaryTest {

    @Test
    fun `summary starts with zero observations`() {
        val summary = summary("request_duration") {}

        val value = summary.get()
        assertEquals(0.0, value.count)
        assertEquals(0.0, value.sum)
        assertNotNull(value.created)
    }

    @Test
    fun `observe increases count and sum`() = runTest {
        val summary = summary("response_time") {}

        summary.observe(1.5)
        summary.observe(2.5)

        val value = summary.get()
        assertEquals(2.0, value.count)
        assertEquals(4.0, value.sum)
    }

    @Test
    fun `observe with labels tracks independently`() = runTest {
        val summary = summary("labeled_response_time") {
            labelNames("method")
        }

        val get = summary.labels("GET")
        val post = summary.labels("POST")

        get.observe(1.0)
        post.observe(2.0)

        assertEquals(1.0, get.get().count)
        assertEquals(1.0, get.get().sum)

        assertEquals(1.0, post.get().count)
        assertEquals(2.0, post.get().sum)
    }

    @Test
    fun `summary with quantiles reports approximated values`() = runTest {
        val q = quantiles(quantile(0.1,0.01),quantile(0.5, 0.05), quantile(0.95, 0.01))
        val summary = summaryQuantiles("quantile_summary", {}, q)

        repeat(100) { summary.observe(it.toDouble()) }

        val value = summary.get()
        assertEquals(100.0, value.count)

        assertTrue{(9.0..11.0).contains(value.quantiles!!.get(0.1)!!)}
        assertTrue{(45.0..55.0).contains(value.quantiles!!.get(0.5)!!)}
        assertTrue{(94.0..96.0).contains(value.quantiles!!.get(0.95)!!)}
        assertEquals((0 until 100).sum().toDouble(), value.sum)
    }

    @Test
    fun `summary rejects invalid quantile name label`() {
        val ex = assertFailsWith<IllegalStateException> {
            summary("bad_summary") {
                labelNames("quantile")
            }
        }
        assertTrue(ex.message!!.contains("quantile"))
    }

    @Test
    fun `observe is thread safe`() = runTest {
        val summary = summary("concurrent_summary") {}

        val reps = 1000
        val workers = 100

        coroutineScope {
            repeat(workers) {
                launch {
                    repeat(reps) {
                        summary.observe(1.0)
                    }
                }
            }
        }

        val value = summary.get()
        assertEquals((reps * workers).toDouble(), value.count)
        assertEquals((reps * workers).toDouble(), value.sum)
    }

    @Test
    fun `time measures duration and observes`() = runTest {
        val summary = summary("timed_summary") {}

        val duration = summary.time(Runnable {
            runBlocking {
                delay(100)
            }
        })

        val value = summary.get()
        assertEquals(1.0, value.count)
        assertTrue(value.sum >= 0.1, "Expected at least 0.1s but got ${value.sum}")
        assertTrue(duration >= 0.1)
    }

    @Test
    fun `get returns consistent timestamp`() = runTest {
        val summary = summary("summary_timestamp") {}

        summary.observe(0.5)
        val value = summary.get()

        assertTrue(value.created > 0)
    }
}
