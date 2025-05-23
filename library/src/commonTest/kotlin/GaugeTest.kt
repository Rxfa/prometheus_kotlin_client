import io.github.kotlin.fibonacci.Gauge
import io.github.kotlin.fibonacci.getCurrentSeconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GaugeTest {
    private val validFullName = "current_temperature"
    private val validHelpText = "Current temperature."

    object FixedClock : Clock{
        override fun now() = Instant.fromEpochSeconds(0)
    }

    @Test
    fun `Gauge starts on zero`(){
        val gauge = Gauge(validFullName, validHelpText)
        assertEquals(0.0, gauge.get())
    }

    @Test
    fun `Gauge can be incremented`(){
        val gauge = Gauge(validFullName, validHelpText)
        gauge.inc()

        assertEquals(1.0, gauge.get())
        gauge.inc(2.0)
        assertEquals(3.0, gauge.get())
    }

    @Test
    fun `Incrementing gauge by negative value throws exception`(){
        val gauge = Gauge(validFullName, validHelpText)
        assertFailsWith<IllegalArgumentException> {
            gauge.inc(-1.0)
        }
    }

    @Test
    fun `Gauge can be decremented`(){
        val gauge = Gauge(validFullName, validHelpText)
        gauge.dec()

        assertEquals(-1.0, gauge.get())
        gauge.dec(2.0)
        assertEquals(-3.0, gauge.get())
    }

    @Test
    fun `Decrementing gauge by negative value throws exception`(){
        val gauge = Gauge(validFullName, validHelpText)
        assertFailsWith<IllegalArgumentException> {
            gauge.dec(-1.0)
        }
    }

    @Test
    fun `Gauge can be set to a given value`(){
        val gauge = Gauge(validFullName, validHelpText)
        gauge.set(5.0)
        assertEquals(5.0, gauge.get())
    }

    @Test
    fun `Gauge can be set to the current unixtime in seconds`(){
        val gauge = Gauge(validFullName, validHelpText, clock = FixedClock)
        gauge.setToCurrentTime()
        assertEquals(getCurrentSeconds(FixedClock).toDouble(), gauge.get())
    }

}