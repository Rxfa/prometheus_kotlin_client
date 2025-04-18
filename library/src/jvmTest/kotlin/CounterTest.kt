import io.github.kotlin.fibonacci.Counter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CounterTest {
    @Test
    fun `counter starts on zero`(){
        val counter = Counter("points_earned_total", "Total points earned by users.")
        assertEquals(0.0, counter.get())
    }

    @Test
    fun `increment increases counter`(){
        val counter = Counter("points_earned_total", "Total points earned by users.")
        counter.inc()
        assertEquals(1.0, counter.get())

        counter.inc(3.5)
        assertEquals(4.5, counter.get())
    }

    @Test
    fun `increment by negative value throws exception`(){
        val counter = Counter("points_earned_total", "Total points earned by users.")
        assertFailsWith<IllegalArgumentException>{counter.inc(-1.0)}
    }

    @Test
    fun `counter with labels starts on zero`(){
        val counter = Counter("requests_total", "Total number of requests.", listOf("Method")).labels("get")
        assertEquals(0.0, counter.get())
    }

    @Test
    fun `counter with labels increments correctly`(){
        val counter = Counter("requests_total", "Total number of requests.", listOf("Method"))
        val getCounter = counter.labels("GET")
        val postCounter = counter.labels("POST")

        getCounter.inc()
        postCounter.inc(3.0)

        assertEquals(1.0, getCounter.get())
        assertEquals(3.0, postCounter.get())
    }

    @Test
    fun `counter with labels increment with negative value throws exception`(){
        val counterWithLabel = Counter("requests_total", "Total number of requests.", listOf("method")).labels("get")
        assertFailsWith<IllegalArgumentException>{counterWithLabel.inc(-1.0)}
    }
}