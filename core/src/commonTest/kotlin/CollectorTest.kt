import io.github.kotlin.fibonacci.CollectorRegistry
import io.github.kotlin.fibonacci.Counter
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectorTest {
    @Test
    fun `test register`(){
        val counter = Counter("messages_total", "Total number of messages")
        val collectors = runBlocking {
            counter.register()
            CollectorRegistry.defaultRegistry.getCollectors()
        }
        assertEquals(1, collectors.size)
        assertEquals(collectors.first(), counter)
    }
}