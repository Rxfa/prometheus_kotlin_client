package io.github.kotlin.fibonacci

actual abstract class Collector {
    actual abstract fun collect(): List<MetricFamilySamples>
    actual abstract fun getName(): String?
    actual enum class Type {
        COUNTER, GAUGE
    }

    /**
     * A metric and all of its samples.
     */
    actual class MetricFamilySamples actual constructor(
        name: String,
        unit: String,
        type: Type,
        help: String,
        samples: List<Sample>
    )

    /**
     * A single Sample, with a unique name and set of labels.
     */
    actual class Sample actual constructor(
        name: String,
        labelNames: List<String>,
        labelValues: List<String>,
        value: Double,
        timestamp: Long
    )

    actual suspend fun register(registry: CollectorRegistry): Collector {
        TODO("Not yet implemented")
    }

}