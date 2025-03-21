package io.github.kotlin.fibonacci


expect class CollectorRegistry {
    suspend fun register(collector: Collector)

    suspend fun unregister(collector: Collector)

    suspend fun clear()

    suspend fun collect(): List<List<Collector. MetricFamilySamples>>

    suspend fun getCollectors(): List<Collector>

    companion object {
        val defaultRegistry: CollectorRegistry
    }
}