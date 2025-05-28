package io.github.kotlin.fibonacci

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


class CollectorRegistry {
    private val mutex = Mutex()
    private val collectorNames = mutableSetOf<String>()
    private var collectors = listOf<Collector>()

    suspend fun register(collector: Collector) {
        val collectorName = collector.getClassName()
        mutex.withLock {
            val addedToRegistry = collectorNames.add(collectorName)
            if (!addedToRegistry) {
                throw IllegalStateException("Collector is already registered: $collectorName")
            }
            this.collectors += collector
        }
    }

    suspend fun unregister(collector: Collector) {
        val collectorName = collector.fullName
        mutex.withLock {
            if(this.collectorNames.contains(collectorName)) {
                this.collectors -= collector
            }
            this.collectorNames.remove(collectorName)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            this.collectors = listOf()
            this.collectorNames.clear()
        }
    }

    suspend fun collect(): List<Collector> {
        return withContext(Dispatchers.Default) { getCollectors() }
    }

    companion object {
        val defaultRegistry = CollectorRegistry()
    }

    suspend fun getCollectors(): List<Collector> {
        return mutex.withLock { collectors.toList() }
    }
}