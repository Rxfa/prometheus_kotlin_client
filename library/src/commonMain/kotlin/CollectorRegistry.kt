package io.github.kotlin.fibonacci

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


public class CollectorRegistry {
    private val mutex = Mutex()
    private val collectorNames = mutableSetOf<String>()
    private var collectors = listOf<Collector>()

    public suspend fun register(collector: Collector) {
        val collectorName = collector.fullName
        mutex.withLock {
            val addedToRegistry =  collectorNames.add(collectorName)
            if (!addedToRegistry) {
                throw IllegalStateException("Collector is already registered: $collectorName")
            }
           this.collectors += collector
        }
    }

    public suspend fun unregister(collector: Collector) {
        val collectorName = collector.fullName
        mutex.withLock {
            if(this.collectorNames.contains(collectorName)) {
                this.collectors -= collector
            }
            this.collectorNames.remove(collectorName)
        }
    }

    public suspend fun clear() {
        mutex.withLock {
            this.collectors = listOf()
            this.collectorNames.clear()
        }
    }

    public suspend fun collect(): List<Collector> {
        return withContext(Dispatchers.Default) { getCollectors() }
    }

    public companion object {
        public val defaultRegistry : CollectorRegistry = CollectorRegistry()
    }

    public suspend fun getCollectors(): List<Collector> {
        return mutex.withLock { collectors.toList() }
    }
}