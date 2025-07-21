import io.github.rxfa.prometheus.core.Collector
import io.github.rxfa.prometheus.core.CollectorRegistry
import io.github.rxfa.prometheus.core.Counter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CollectorRegistryTest {
    private lateinit var registry: CollectorRegistry

    @BeforeTest
    fun setup() {
        registry = CollectorRegistry()
    }

    @Test
    fun `test register collector`() = runTest {
        val collector = Counter("test_metric", "Test metric")
        registry.register(collector)

        val collectors = registry.getCollectors()
        assertEquals(1, collectors.size)
        assertEquals(collector, collectors.first())
    }

    @Test
    fun `test unregister collector`() = runTest {
        val collector = Counter("test_metric", "Test metric")
        registry.register(collector)
        registry.unregister(collector)

        val collectors = registry.getCollectors()
        assertTrue(collectors.isEmpty())
    }


    @Test
    fun `test clear registry`() = runTest {
        val collector1 = Counter("metric1", "Metric 1")
        val collector2 = Counter("metric2", "Metric 2")
        registry.register(collector1)
        registry.register(collector2)

        registry.clear()

        val collectors = registry.getCollectors()
        assertTrue(collectors.isEmpty())
    }
    @Test
    fun `test duplicate registration throws exception`() = runTest {
        val collector = Counter("test_metric", "Test metric")
        registry.register(collector)

        val exception = assertFailsWith<IllegalStateException> {
            registry.register(collector)
        }
        assertEquals("Collector is already registered: test_metric", exception.message)
    }

    @Test
    fun `test concurrency`() = runTest {
        val collectors = mutableListOf<Counter>()

        val jobs = List(100) {
            launch {
                val localCollector = Counter("metric_$it", "Metric $it")
                registry.register(localCollector)
                collectors.add(localCollector)
                registry.unregister(localCollector)
            }
        }

        jobs.forEach { it.join() }

        assertTrue(registry.getCollectors().isEmpty())
    }
}