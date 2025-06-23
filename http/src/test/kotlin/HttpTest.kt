import io.github.rxfa.prometheus.core.CollectorRegistry
import io.github.rxfa.prometheus.http.httpServer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import java.net.ServerSocket
import kotlin.test.*

fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

class HttpTest {
    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    @BeforeTest
    fun clearMetrics() {
        runTest {
            CollectorRegistry.defaultRegistry.clear()
        }
    }

    @AfterTest
    fun closeServer() {
        server.stop()
    }

    @Test
    fun `default configuration exposes metrics on port 8080`() {
        runBlocking {
            server =
                httpServer {
                    metrics {
                        path = "/metrics"
                    }
                }

            client
                .get("/metrics") {
                    this.port = 8080
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertContains(bodyAsText(), "http_requests_total")
                }
        }
    }

    @Test
    fun `server starts on custom port`() {
        runBlocking {
            val port = findFreePort()
            server =
                httpServer {
                    this.port = port
                }

            client
                .get("/metrics") {
                    this.port = port
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertContains(bodyAsText(), "http_requests_total")
                }
        }
    }

    @Test
    fun `fails to start on occupied port`() {
        val port = findFreePort()
        server = httpServer { this.port = port }

        assertFailsWith<Exception> {
            httpServer { this.port = port }
        }
    }

    companion object {
        private val client = HttpClient(CIO)

        @JvmStatic
        @AfterAll
        fun afterAll() {
            client.close()
        }
    }
}
