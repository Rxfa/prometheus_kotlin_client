package io.github.kotlin.fibonacci

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class CollectorRegistry {
    private val mutex = Mutex()
    private val collectorNames = mutableSetOf<String>()
    private var collectors = listOf<Collector>()

    actual suspend fun register(collector: Collector) {
        val collectorName = collector.javaClass.simpleName
        val addedToRegistry = mutex.withLock { collectorNames.add(collectorName) }
        if (!addedToRegistry) {
            throw IllegalStateException("Collector is already registered: $collectorName")
        }
        mutex.withLock { this.collectors += collector }
    }

    actual suspend fun unregister(collector: Collector) {
        val collectorName = collector.javaClass.simpleName
        mutex.withLock {
            if(this.collectorNames.contains(collectorName)) {
                this.collectors -= collector
            }
            this.collectorNames.remove(collectorName)
        }
    }

    actual suspend fun clear() {
        mutex.withLock {
            this.collectors = listOf()
            this.collectorNames.clear()
        }
    }

    actual suspend fun collect(): List<Collector> {
        return withContext(Dispatchers.Default) { getCollectors() }
    }

    actual companion object {
        actual val defaultRegistry = CollectorRegistry()
    }

    actual suspend fun getCollectors(): List<Collector> {
        return mutex.withLock { collectors.toList() }
    }
}