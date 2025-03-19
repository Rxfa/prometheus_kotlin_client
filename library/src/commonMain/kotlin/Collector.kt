package io.github.kotlin.fibonacci

expect abstract class Collector {
    abstract fun collect(): List<MetricFamilySamples>

    enum class Type{
        COUNTER,
    }

    /**
     * A metric and all of its samples.
     */
    class MetricFamilySamples(
        name: String,
        unit: String?,
        type: Type,
        help: String,
        samples: List<Sample>
    )

    /**
     * A single Sample, with a unique name and set of labels.
     */
    class Sample(
        name: String,
        labelNames: List<String>,
        labelValues: List<String>,
        value: Double,
        timestamp: Long,
    )

    suspend fun register(registry: CollectorRegistry = CollectorRegistry.defaultRegistry): Collector
}