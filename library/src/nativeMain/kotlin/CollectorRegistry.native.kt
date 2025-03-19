package io.github.kotlin.fibonacci

actual class CollectorRegistry {
    actual suspend fun register(collector: Collector) {
    }

    actual suspend fun unregister(collector: Collector) {
    }

    actual suspend fun clear() {
    }

    actual suspend fun collect(): List<List<Collector.MetricFamilySamples>> {
        TODO("Not yet implemented")
    }

    actual suspend fun getCollectors(): List<Collector> {
        TODO("Not yet implemented")
    }

    actual companion object {
        actual val defaultRegistry: CollectorRegistry
            get() = TODO("Not yet implemented")
    }

}