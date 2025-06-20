import io.github.rxfa.prometheus.ktor.installPrometheusMetrics
import io.github.rxfa.prometheus.ktor_example.configureRouting
import io.github.rxfa.prometheus.ktor_example.simulateTrafficLoad
import io.ktor.server.testing.*
import kotlin.test.Test

class MainTest {
    @Test
    fun testAppStartsSuccessfully(): Unit = testApplication {
        application {
            configureRouting()
            installPrometheusMetrics()
            simulateTrafficLoad()
        }
    }
}