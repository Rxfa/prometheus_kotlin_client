import io.github.rxfa.prometheus.core.SimpleCollector
import kotlin.test.*

class SimpleCollectorTest {
    private val validNameText = "valid_metric"
    private val validHelpText = "This represents valid help."

    private class DummyCollector(
        fullName: String,
        help: String,
        labelNames: List<String> = emptyList(),
        unit: String = "",
    ): SimpleCollector<DummyCollector.Child>(fullName, help, labelNames, unit) {
        init {
            initializeNoLabelsChild()
        }

        override val suffixes: Set<String> = setOf()
        override val name: String = buildMetricName()
        override val type: Type = Type.UNKNOWN

        override fun newChild(): Child {
            return Child()
        }

        override fun collect(): MetricFamilySamples {
            val samples = mutableListOf<Sample>()
            for((labels, child) in childMetrics){
                samples += Sample(name = name, labelNames = labelNames, labelValues = labels, value = child.value)
            }
            return familySamplesList(samples)
        }

        inner class Child(){
            var value: Double = 0.0
                private set
        }

        fun getNoLabelsChild() = noLabelsChild
    }

    @Test
    fun `invalid unit name throws`() {
        assertFailsWith<IllegalArgumentException> {
            DummyCollector("metric", "help", unit = "!!!")
        }
    }

    @Test
    fun `buildMetricName appends unit if not present`() {
        val c = DummyCollector("request_duration", "help", unit = "seconds")
        assertEquals("request_duration_seconds", c.name)
    }

    @Test
    fun `buildMetricName does not double-append unit`() {
        val c = DummyCollector("request_duration_seconds", "help", unit = "seconds")
        assertEquals("request_duration_seconds", c.name)
    }

    @Test
    fun `invalid name throws`(){
        assertFailsWith<IllegalArgumentException> {
            DummyCollector("@metric", validHelpText)
        }
        assertFailsWith<IllegalArgumentException> {
            DummyCollector("metric name", validHelpText)
        }
        assertFailsWith<IllegalArgumentException> {
            DummyCollector("metric-name", validHelpText)
        }
        assertFailsWith<IllegalArgumentException> {
            DummyCollector("123metric_name", validHelpText)
        }
        assertTrue{ runCatching { DummyCollector(validNameText, validHelpText) }.isSuccess }
    }

    @Test
    fun `invalid label name throws`(){
        assertFailsWith<IllegalArgumentException> {
            DummyCollector(validNameText, validHelpText, listOf("-invalid_name", "_valid_metric"))
        }
        assertFailsWith<IllegalArgumentException> {
            DummyCollector(validNameText, validHelpText, listOf("metric name"))
        }
        assertFailsWith<IllegalArgumentException> {
            DummyCollector(validNameText, validHelpText, listOf("metric-name"))
        }
        assertFailsWith<IllegalArgumentException> {
            DummyCollector(validNameText, validHelpText, listOf("__metric_name"))
        }
        assertTrue{ runCatching { DummyCollector(validNameText, validHelpText, listOf("valid")) }.isSuccess }
    }

    @Test
    fun `no labels after clear`(){
        val collector = DummyCollector(validNameText, validHelpText, listOf("Method"))
        collector.labels("GET")
        assertEquals(1, collector.collect().samples.size)
        collector.clear()
        assertEquals(0, collector.collect().samples.size)
    }

    @Test
    fun `no label after remove`(){
        val collector = DummyCollector(validNameText, validHelpText, listOf("Method", "Status_Code"))
        collector.labels("GET", "404")
        collector.labels("POST", "200")
        assertEquals(2, collector.collect().samples.size)
        collector.remove("GET", "404")
        assertEquals(1, collector.collect().samples.size)
    }

    @Test
    fun `too many labels throws`(){
        val collector = DummyCollector(validNameText, validHelpText, listOf("Method"))
        assertFailsWith<IllegalArgumentException> {
            collector.labels("GET", "404")
        }
    }

    @Test
    fun `too few labels throws`(){
        val collector = DummyCollector(validNameText, validHelpText, listOf("Method", "Status_Code"))
        assertFailsWith<IllegalArgumentException> {
            collector.labels("GET")
        }
    }

    @Test
    fun `noLabelsChild is initialized when labelNames is empty`() {
        val collector = DummyCollector(validNameText, validHelpText)
        assertNotNull(collector.getNoLabelsChild())
    }
}