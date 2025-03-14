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
        val name: String,
        val unit: String,
        val type: Type,
        val help: String,
        val samples: List<Sample>
    )

    /**
     * A single Sample, with a unique name and set of labels.
     */
    actual class Sample actual constructor(
        val name: String,
        val labelNames: List<String>,
        val labelValues: List<String>,
        val value: Double,
        val timestamp: Long
    ){
        constructor(
            name: String,
            labelNames: List<String>,
            labelValues: List<String>,
            value: Double,
        ): this(name, labelNames, labelValues, value, System.currentTimeMillis())
    }

    actual suspend fun register(registry: CollectorRegistry): Collector {
        registry.register(this)
        return this
    }

}