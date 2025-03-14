package io.github.kotlin.fibonacci

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class CollectorRegistry {
    private val mutex = Mutex()
    private val collectorNames = mutableSetOf<String>()
    private val collectors = mutableListOf<Collector>()

    actual suspend fun register(collector: Collector) {
        val collectorName = collector.getName()
        if (collectorName != null) {
            val addedToRegistry = mutex.withLock { collectorNames.add(collectorName) }
            if (addedToRegistry) {
                throw IllegalStateException("Collector is already registered: $collectorName")
            }
        }
        mutex.withLock { this.collectors.add(collector) }
    }

    actual suspend fun unregister(collector: Collector) {
        val collectorName = collector.getName()
        mutex.withLock {
            if(this.collectorNames.contains(collectorName)) {
                this.collectors.remove(collector)
            }
            this.collectorNames.remove(collectorName)
        }
    }

    actual suspend fun clear() {
        mutex.withLock {
            this.collectors.clear()
            this.collectorNames.clear()
        }
    }

    actual suspend fun export() {
        TODO()
    }

    actual companion object {
        actual val defaultRegistry = CollectorRegistry()
    }

    actual suspend fun collectors(): Set<Collector> {
        TODO()
    }
}