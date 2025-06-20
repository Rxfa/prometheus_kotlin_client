package io.github.rxfa.prometheus.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A thread-safe registry for [Collector] instances.
 *
 * The [CollectorRegistry] keeps track of registered collectors and provides
 * utilities to register, unregister, clear, and list collectors.
 *
 * All operations are safe to use in concurrent and coroutine-based environments.
 */
public class CollectorRegistry {
    private val mutex = Mutex()
    private val collectorNames = mutableSetOf<String>()
    private var collectors = listOf<Collector>()

    /**
     * Registers a new [Collector] in this registry.
     *
     * @param collector The [Collector] to register.
     * @throws IllegalStateException if a collector with the same full name is already registered.
     */
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

    /**
     * Unregisters a [Collector] from the registry.
     *
     * If the collector is not found, the call is silently ignored.
     *
     * @param collector The [Collector] to remove.
     */
    public suspend fun unregister(collector: Collector) {
        val collectorName = collector.fullName
        mutex.withLock {
            if(this.collectorNames.contains(collectorName)) {
                this.collectors -= collector
            }
            this.collectorNames.remove(collectorName)
        }
    }

    /**
     * Clears all registered collectors from the registry.
     */
    public suspend fun clear() {
        mutex.withLock {
            this.collectors = listOf()
            this.collectorNames.clear()
        }
    }

    /**
     * Asynchronously collects all registered [Collector]s.
     *
     * Useful for use in exposition endpoints (e.g., `/metrics`).
     *
     * @return A list of currently registered [Collector]s.
     */
    public suspend fun collect(): List<Collector> {
        return withContext(Dispatchers.Default) { getCollectors() }
    }

    /**
     * Retrieves the current list of registered collectors.
     *
     * @return A snapshot list of registered [Collector]s.
     */
    public suspend fun getCollectors(): List<Collector> {
        return mutex.withLock { collectors.toList() }
    }

    public companion object {
        /**
         * A globally accessible default registry instance.
         *
         * Use this for quick-start scenarios and typical single-application usage.
         */
        public val defaultRegistry: CollectorRegistry = CollectorRegistry()
    }
}
