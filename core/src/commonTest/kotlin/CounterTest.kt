import io.github.rxfa.prometheus.core.Counter
import io.github.rxfa.prometheus.core.countExceptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest


class CounterTest {
    @Test
    fun `buildMetricName appends _total when missing`() {
        val counter = Counter(
            fullName = "http_requests",
            help = "HTTP requests counter"
        )

        assertEquals("http_requests_total", counter.name)
    }

    @Test
    fun `buildMetricName keeps _total if already present`() {
        val counter = Counter(
            fullName = "http_requests_total",
            help = "HTTP requests counter"
        )

        assertEquals("http_requests_total", counter.name)
    }

    @Test
    fun `buildMetricName removes existing _total before applying logic`() {
        val counter = Counter(
            fullName = "processed_items_total",
            help = "Processed items",
            unit = "seconds"
        )

        // `_total` is stripped before unit is handled, so we get processed_items_total
        assertEquals("processed_items_seconds_total", counter.name)
    }

    @Test
    fun `counter starts on zero`(){
        val counter = Counter("points_earned_total", "Total points earned by users.")
        assertEquals(0.0, counter.get())
    }

    @Test
    fun `increment increases counter`(){
        runTest {
            val counter = Counter("points_earned_total", "Total points earned by users.")

            counter.inc()
            assertEquals(1.0, counter.get())

            counter.inc(3.5)
            assertEquals(4.5, counter.get())
        }
    }

    @Test
    fun `increment by negative value throws exception`(){
        runTest {
            val counter = Counter("points_earned_total", "Total points earned by users.")

            assertFailsWith<IllegalArgumentException> { counter.inc(-1.0) }
        }
    }

    @Test
    fun `counter with labels starts on zero`(){
        val counter = Counter("requests_total", "Total number of requests.", listOf("method")).labels("get")
        assertEquals(0.0, counter.get())
    }

    @Test
    fun `counter with labels increments correctly`(){
        runTest {
            val counter = Counter("requests_total", "Total number of requests.", listOf("method"))
            val getCounter = counter.labels("GET")
            val postCounter = counter.labels("POST")

            getCounter.inc()
            postCounter.inc(3.0)

            assertEquals(1.0, getCounter.get())
            assertEquals(3.0, postCounter.get())
        }
    }

    @Test
    fun `counter with labels increment with negative value throws exception`(){
        runTest {
            val counterWithLabel = Counter("requests_total", "Total number of requests.", listOf("method")).labels("get")

            assertFailsWith<IllegalArgumentException>{counterWithLabel.inc(-1.0)}
        }
    }

    @Test
    fun `counter can count exceptions raised in a given piece of code`(){
        runTest {
            val counter = Counter("requests_total", "Total number of requests.")
            counter.countExceptions<Any>{
                throw IllegalArgumentException()
            }
            counter.countExceptions<Any>{
                throw IllegalStateException()
            }
            assertEquals(2.0, counter.get())
        }
    }

    @Test
    fun `counter with labels can count exceptions raised in a given piece of code`(){
        runTest {
            val counter = Counter("requests_total", "Total number of requests.", listOf("method")).labels("GET")
            counter.countExceptions<Any>{
                throw IllegalArgumentException()
            }
            counter.countExceptions<Any>{
                throw IllegalStateException()
            }
            assertEquals(2.0, counter.get())
        }
    }

    @Test
    fun `counter can count specific exceptions raised in a given piece of code`(){
        runTest {
            val counter = Counter("requests_total", "Total number of requests.")
            counter.countExceptions<Any>(IllegalArgumentException::class){
                throw IllegalArgumentException()
            }
            counter.countExceptions<Any>(IllegalStateException::class){
                throw IllegalArgumentException()
            }
            assertEquals(1.0, counter.get())
        }
    }

    @Test
    fun `counter with labels can count specific exceptions raised in a given piece of code`(){
        runTest {
            val counter = Counter("requests_total", "Total number of requests.", listOf("method")).labels("GET")
            counter.countExceptions<Any>(IllegalArgumentException::class){
                throw IllegalArgumentException()
            }
            counter.countExceptions<Any>(IllegalStateException::class){
                throw IllegalArgumentException()
            }
            assertEquals(1.0, counter.get())
        }
    }



    @Test
    fun `counter increments are thread safe`() {
        runBlocking {
            /**
             * coroutines can't be higher than 3 because ios has a limit of 3 processors
             */
            val repetitions = 10_000
            val coroutines = 3
            val counter = Counter("points_earned_total", "Total points earned by users.")
            coroutineScope {
                List(coroutines) {
                    async {
                        repeat(repetitions) {
                            counter.inc()
                        }
                    }
                }.awaitAll()
            }
            assertEquals(repetitions * coroutines.toDouble(), counter.get())
        }
    }
}