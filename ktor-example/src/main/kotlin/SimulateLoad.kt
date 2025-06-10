package io.github.rxfa.prometheus.ktor_example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.coroutines.*

fun Application.simulateTrafficLoad() {
    monitor.subscribe(ApplicationStarted) {
        launch(Dispatchers.IO) {
            val client = HttpClient(CIO) {
                expectSuccess = false
            }

            val baseUrl = "http://localhost:8080"

            val endpoints = listOf(
                "/" to HttpMethod.Get,
                "/search" to HttpMethod.Get,
                "/submit" to HttpMethod.Post,
                "/random" to HttpMethod.Get,
                "/fail" to HttpMethod.Get
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
                            // Logging optional
                        }
                    }
                    delay((50..300).random().toLong())
                }

                delay((500..1500).random().toLong())
            }
        }
    }
}
