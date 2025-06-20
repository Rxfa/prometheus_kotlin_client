
import io.github.rxfa.prometheus.core.histogramBuckets
import io.github.rxfa.prometheus.core.linearHistogramBuckets
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest

class HistogramTest {

    @Test
    fun `histogram starts with zero observations`() {
        val histogram = linearHistogramBuckets("request_duration_seconds", {}, 0.0, 1.0, 3)

        val value = histogram.get().buckets

        assertEquals(listOf(0.0, 0.0, 0.0, 0.0), value)
    }


    @Test
    fun `observe adds value to the first bucket`() {
        runTest {
            val histogram = linearHistogramBuckets("request_duration_seconds", { labelNames("method") }, 0.0, 1.0, 1)

            val get = histogram.labels("GET")
            get.observe(0.5)

            val post = histogram.labels("POST")

            val valueGet = get.get().buckets
            assertEquals(listOf(0.0, 1.0), valueGet)
            val valuePost = post.get().buckets
            assertEquals(listOf(0.0, 0.0), valuePost)
        }
    }

    @Test
    fun `observe adds values to appropriate buckets`() {
        runTest {
            val histogram = linearHistogramBuckets("response_time", {}, 0.0, 2.0, 3)
            histogram.observe(1.0)
            histogram.observe(3.0)
            histogram.observe(5.0)
            histogram.observe(7.0)
            histogram.observe(10.0)

            val value = histogram.get()
            assertEquals(4, value.buckets.size)
            assertEquals(listOf(0.0, 1.0, 2.0, 5.0), value.buckets)
            assertEquals(26.0, value.sum)
        }

    }

    @Test
    fun `histogram rejects unsorted buckets`() {
        assertFailsWith<IllegalStateException> {
            histogramBuckets("invalid_histogram", {}, listOf(3.0, 2.0, 1.0))
        }
    }

    @Test
    fun `histogram with label adds values independently`() {
        runTest {
            val histogram = linearHistogramBuckets("response_time", {
                labelNames("method")
            }, 0.0, 1.0, 2)

            val get = histogram.labels("GET")
            val post = histogram.labels("POST")

            get.observe(0.5)
            post.observe(1.5)

            assertEquals(listOf(0.0, 1.0, 1.0), get.get().buckets)
            assertEquals(listOf(0.0, 0.0, 1.0), post.get().buckets)
        }
    }

    @Test
    fun `time measures execution duration in seconds`() {
        runTest {
            val histogram = linearHistogramBuckets("execution_duration", {}, 0.0, 1.0, 10)
            val duration = histogram.time(Runnable {
                runBlocking {
                    delay(100)
                }
            })
            assertTrue(duration >= 0.1)
        }
    }

    @Test
    fun `observe is thread safe`() = runTest {
        val histogram = linearHistogramBuckets("concurrent_observe", {}, 0.0, 1.0, 2)
        val reps = 1000
        val workers = 100

        coroutineScope {
            repeat(workers) {
                async {

                    repeat(reps) {
                        histogram.observe(0.5)
                    }

                }
            }
        }

        val value = histogram.get()
        assertEquals(reps * workers.toDouble(), value.buckets.last())
    }

    @Test
    fun `timer with exemplar labels tracks duration`() {
        runTest {
            val histogram = linearHistogramBuckets("timed_with_labels", {}, 0.0, 1.0, 10)
            val duration = histogram.timeWithExemplar(Runnable {
                runBlocking {
                    delay(500)
                }
            }, listOf("labelA"))

            assertTrue(duration >= 0.5)
        }
    }

    @Test
    fun `get returns correct sum and created timestamp`() {
        runTest {
            val histogram = linearHistogramBuckets("value_info", {}, 0.0, 1.0, 10)
            histogram.observe(0.5)
            histogram.observe(1.5)

            val value = histogram.get()
            assertEquals(2.0, value.sum)
            assertTrue(value.created > 0)
        }
    }
}
