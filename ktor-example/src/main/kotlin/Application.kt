package io.github.rxfa.prometheus.ktor_example

import io.ktor.server.application.*
import io.github.rxfa.prometheus.ktor.installPrometheusMetrics

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureRouting()
    installPrometheusMetrics()
    simulateTrafficLoad()
}

