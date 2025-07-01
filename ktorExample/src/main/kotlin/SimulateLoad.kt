import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Application.simulateTrafficLoad() {
    monitor.subscribe(ApplicationStarted) {
        launch(Dispatchers.IO) {
            val client =
                HttpClient(CIO) {
                    expectSuccess = false
                }

            val baseUrl = "http://localhost:8080"

            val endpoints =
                listOf(
                    "/" to HttpMethod.Get,
                    "/search" to HttpMethod.Get,
                    "/submit" to HttpMethod.Post,
                    "/random" to HttpMethod.Get,
                    "/fail" to HttpMethod.Get,
                )

            while (true) {
                val (path, method) = endpoints.random()

                repeat((1..5).random()) {
                    launch {
                        try {
                            client.request("$baseUrl$path") {
                                this.method = method
                            }
                        } catch (_: Exception) {
                        }
                    }
                    delay((50..300).random().toLong())
                }

                delay((500..1500).random().toLong())
            }
        }
    }
}
