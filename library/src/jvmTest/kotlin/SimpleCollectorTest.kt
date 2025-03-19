import io.github.kotlin.fibonacci.Counter
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SimpleCollectorTest {
    private val validNameText = "valid_metric"
    private val validHelpText = "This represents valid help."

    @Test
    fun `invalid name throws`(){
        assertFailsWith<IllegalArgumentException> {
            Counter("@metric", validHelpText)
        }
        assertFailsWith<IllegalArgumentException> {
            Counter("metric name", validHelpText)
        }
        assertFailsWith<IllegalArgumentException> {
            Counter("metric-name", validHelpText)
        }
        assertFailsWith<IllegalArgumentException> {
            Counter("123metric_name", validHelpText)
        }
        assertTrue{ runCatching { Counter(validNameText, validHelpText) }.isSuccess }
    }

    @Test
    fun `invalid label name throws`(){
        assertFailsWith<IllegalArgumentException> {
            Counter(validNameText, validHelpText, listOf("-invalid_name", "_valid_metric"))
        }
        assertFailsWith<IllegalArgumentException> {
            Counter(validNameText, validHelpText, listOf("metric name"))
        }
        assertFailsWith<IllegalArgumentException> {
            Counter(validNameText, validHelpText, listOf("metric-name"))
        }
        assertFailsWith<IllegalArgumentException> {
            Counter(validNameText, validHelpText, listOf("__metric_name"))
        }
        assertTrue{ runCatching { Counter(validNameText, validHelpText, listOf("valid")) }.isSuccess }
    }

    @Test
    fun `no labels after clear()`(){
        TODO()
    }

    @Test
    fun `no label after remove()`(){
        TODO()
    }

    @Test
    fun `too many labels throws`(){
        TODO()
    }

    @Test
    fun `too few labels throws`(){
        TODO()
    }
}