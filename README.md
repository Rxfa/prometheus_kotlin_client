# Prometheus Kotlin Client

The **Prometheus Kotlin Client** (or as we like to call it, Klient) is a modern, idiomatic Prometheus client library 
written in Kotlin. 
Designed from the ground up for Kotlin Multiplatform and coroutine-based applications, it offers native support for Prometheus metric instrumentation with a clean DSL and coroutine-friendly architecture.

## Features

-  **Kotlin-first API** — Uses expressive DSLs and idiomatic Kotlin constructs.
-  **Coroutine-safe metrics** — Designed for structured concurrency and non-blocking operation.
-  **Supports all core Prometheus types** — Counters, Gauges, Histograms, and Summaries.
-  **Pluggable exposition** —  Use a lightweight HTTP server or integrate with any of the supported frameworks.
-  **Testable & extensible** — Suitable for libraries, services, and microservices alike.

---

## Why Prometheus Kotlin Client?

While the official Prometheus Java client works across the JVM ecosystem, it's designed around Java idioms and concurrency models. When used in Kotlin, especially in coroutine-based applications, it often feels verbose, blocking, and unintuitive.

The **Prometheus Kotlin Client** offers a native alternative — designed from the ground up for Kotlin, not just made to work with it.

-  **Coroutine-safe**: Built with structured concurrency in mind — no blocking, no race conditions.
-  **Idiomatic Kotlin DSL**: Define metrics with expressive, concise syntax that feels like Kotlin — not Java with Kotlin syntax.
-  **Integration with Kotlin-first frameworks**: Take advantage of integrations with Kotlin Frameworks such as Ktor.

### A Side-by-Side Comparison

#### Using the official Java client from Kotlin

```kotlin
val counter = Counter.build()
    .name("http_requests_total")
    .help("Total number of HTTP requests")
    .labelNames("method", "status")
    .register()

counter.labels("GET", "200").inc()
```

#### Using Prometheus Kotlin Client

```kotlin
val requests = counter("http_requests_total") {
    help("Total number of HTTP requests")
    labelNames("method", "status")
}

requests.labels("GET", "200").inc()
```

No builders, no boilerplate — just clean, idiomatic Kotlin code. 
The Prometheus Kotlin Client makes observability in Kotlin applications feel natural, safe, and productive.

## Getting Started

### 1. Add dependencies

```kotlin
dependencies {
    implementation("io.github.rxfa.prometheus:core:<version>")
    
    // Optional: add for Ktor-based applications
    implementation("io.github.rxfa.prometheus:ktor:<version>")
    
    // Optional: add for standalone HTTP exposition
    implementation("io.github.rxfa.prometheus:http:<version>")
}
```
## Define Metrics
        
```Kotlin
val requests = counter("http_requests_total") {
    help("Total number of HTTP requests")
    labelNames("method", "status")
}

requests.labels("GET", "200").inc()
```

## Expose metrics to Prometheus

You have two main options:

### Ktor Integration

If you are using Ktor, the library offers a seamless integration:
```kotlin
import io.github.rxfa.prometheus.ktor.installPrometheusMetrics
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    installPrometheusMetrics{
        exposeEndpoint = True
        includeTimestamp = True
        metricsPath = "/metrics"
    }
}
```

### HTTP Standalone Server


If you are not using Ktor or any other supported web framework, you can use the standalone HTTP server:
```
import io.github.rxfa.prometheus.http.httpServer

fun main() {
    startMetricsHttpServer{
        port = 8080
        metrics {
            path = "/metrics"
            includeTimestamp = True
        }
    }
}
```

## Metric Types

### Counter

Tracks cumulative values that only go up (e.g., requests or errors).

```kotlin
val requests = counter("http_requests_total") {
    help("Total number of HTTP requests")
    labelNames("method", "status")
}

requests.labels("GET", "200").inc()
requests.labels("POST", "500").inc(2.0)
```

### Gauge

Tracks values that can increase or decrease (e.g., memory usage, active sessions).

```kotlin
val memoryUsage = gauge("memory_usage_bytes") {
    help("Current memory usage in bytes")
}

memoryUsage.set(150_000_000.0)
memoryUsage.inc(1024.0)
memoryUsage.dec(512.0)
```

### Summary

Tracks quantiles over time (e.g., response size percentiles).

```kotlin
val responseSizes = summary("http_response_size_bytes") {
    help("HTTP response sizes in bytes")
}

responseSizes.observe(512.0)
responseSizes.observe(1024.0)

```

### Histogram

Tracks value distributions using buckets (e.g., request duration).

```kotlin
val requestLatency = histogram("http_request_duration_seconds") {
    help("HTTP request latency in seconds")
    buckets(0.1, 0.3, 1.5, 10.0)
}

requestLatency.observe(0.42)
requestLatency.observe(1.2)

```

### Custom metric types

Feel free to create your own custom metric types by inheriting the appropriate classes (**SimpleCollector** or 
**Collector**).

## Demos

Want to see the Prometheus Kotlin Client in action? We've included a complete example in the [`ktor-example`](./ktor-example) module.

It includes:

- A Ktor web app instrumented with Prometheus metrics
- Preconfigured Prometheus and Grafana instances
- Sample dashboards and synthetic load generation

### 🔧 Running the Demo

Just run the following from the project root:

```bash
docker compose up
```
> Note: Before running this, make sure you have Docker installed and running.

## License

[Apache](/LICENSE)
