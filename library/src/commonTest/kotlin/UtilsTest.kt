import io.github.kotlin.fibonacci.doubleQuoteString
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {
    @Test
    fun testDoubleQuoteString() {
        val expected = "\"test\""

        assertEquals(expected, doubleQuoteString("\"test"))
        assertEquals(expected, doubleQuoteString("\"test\""))
        assertEquals(expected, doubleQuoteString("test\""))
        assertEquals(expected, doubleQuoteString("test"))
    }
}