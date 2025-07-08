import io.github.rxfa.prometheus.ktor.installPrometheusMetrics
import io.ktor.server.testing.testApplication
import kotlin.test.Test

class MainTest {
    @Test
    fun testAppStartsSuccessfully(): Unit =
        testApplication {
            application {
                configureRouting()
                installPrometheusMetrics()
                simulateTrafficLoad()
            }
        }
}
