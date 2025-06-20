package io.github.rxfa.prometheus.core

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

public data class ValueSummary(
    val count: Double,
    val sum : Double,
    val quantiles: Map<Double,Double>?,
    val created: Long

)

public fun quantile(
    quantile: Double,
    error: Double = 0.01
): Quantiles.Quantile {
    if (quantile < 0.0 || quantile > 1.0) {
        throw IllegalArgumentException("Quantile " + quantile + " invalid: Expected number between 0.0 and 1.0.")
    }
    if (error < 0.0 || error > 1.0) {
        throw IllegalArgumentException("Error " + error + " invalid: Expected number between 0.0 and 1.0.")
    }
    return Quantiles.Quantile(quantile, error)
}

public fun quantiles(
    vararg quantiles: Quantiles.Quantile
): List<Quantiles.Quantile> {
    return quantiles.toList()
}

public fun summaryQuantiles(
    name: String,
    block: SummaryBuilder.() -> Unit,
    quantiles: List<Quantiles.Quantile>,
    maxAgeSeconds: Long = 60,
    ageBuckets: Int = 5
): Summary {
    return SummaryBuilder(name,false,quantiles,maxAgeSeconds,ageBuckets).apply {block}.build()
}

public fun summary(
    name: String,
    block: SummaryBuilder.() -> Unit
): Summary {
    return SummaryBuilder(name).apply(block).build()
}

public fun Summary.addQuantile(quantile: Double, error: Double){
    if (quantile < 0.0 || quantile > 1.0) {
        throw IllegalArgumentException("Quantile " + quantile + " invalid: Expected number between 0.0 and 1.0.");
    }
    if (error < 0.0 || error > 1.0) {
        throw IllegalArgumentException("Error " + error + " invalid: Expected number between 0.0 and 1.0.");
    }
    this.addQuantile(quantile, error)
}


public class Summary internal constructor(
    fullName: String,
    help: String,
    labelNames: List<String> = emptyList(),
    unit: String = "",
    includeCreatedSeries: Boolean = false,
    private val quantiles: List<Quantiles.Quantile>,
    private val maxAgeSeconds: Long ,
    private val ageBuckets: Int
): SimpleCollector<Summary.Child>(fullName, help, labelNames, unit) {

    override val suffixes: Set<String> = setOf()
    override val name: String = fullName
    override val type: Type = Type.SUMMARY

    override fun newChild(): Child {
        return Child(quantiles)
    }

    init{
        for (label in labelNames) {
            if (label.equals("quantile")) {
                throw IllegalStateException("Summary cannot have a label named 'quantile'.")
            }
        }
        initializeNoLabelsChild()
    }

    public class Timer{

        private val child: Child
        private val start: Long

        private val simpleTimer = SimpleTimer()

        public constructor(child: Summary.Child,start: Long) {
            this.child = child
            this.start = start
        }

        public suspend fun observeDuration():Double{
            val elapsed = simpleTimer.elapsedSecondsFromNanos(start, simpleTimer.defaultTimeProvider.milliTime)
            child.observe(elapsed)
            return elapsed
        }


    }

    public inner class Child{

        public fun startTimer():Timer{
            return Timer(this, Clock.System.now().toEpochMilliseconds())
        }

        public suspend fun time(runnable: Runnable):Double{
            val start = startTimer()
            val final : Double;
            try {
                runnable.run()
            } catch (e: Exception) {
                throw RuntimeException("Error while running timed block", e)
            } finally {
                final = start.observeDuration()
            }
            return final
        }

        public constructor(
            quantiles: List<Quantiles.Quantile>
        ) {
            this.quantiles = quantiles
            if(quantiles.isNotEmpty()){
                quantilesValues = TimeWindowQuantiles(quantiles.toTypedArray(), maxAgeSeconds, ageBuckets)
            }else quantilesValues = null
        }

        private val quantiles: List<Quantiles.Quantile>
        private val quantilesValues: TimeWindowQuantiles?
        private val sum: AtomicLong = atomic(0.0.toRawBits())
        private val count: AtomicLong = atomic(0L)
        private val created: Long = Clock.System.now().toEpochMilliseconds()



        public fun get():ValueSummary{
            val map = mutableMapOf<Double, Double>()
            for(quantile in quantiles){
                map[quantile.quantile] = quantilesValues?.get(quantile.quantile) ?: 0.0
            }



            return ValueSummary(
                count.value.toDouble(),
                Double.fromBits(sum.value),
                map,
                created
                )
        }

        public suspend fun observe(value:Double){
            withContext(Dispatchers.Default) {
                count.getAndIncrement()
                sum.updateAndGet { currentBits ->
                    val current = Double.fromBits(currentBits)
                    val updated = current + value
                    updated.toBits()
                }
                quantilesValues?.insert(value)
            }

        }

    }
    public suspend fun observe(value: Double) {
        noLabelsChild?.observe(value)
    }

    public suspend fun time(runnable: Runnable): Double {
        return noLabelsChild?.time(runnable) ?: 0.0
    }

    public fun get(): ValueSummary {
        return noLabelsChild?.get() ?: throw IllegalStateException("No labels child is not initialized.")
    }


    public override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for ((labels, childs) in childMetrics) {
            val value = childs.get()
            val labelNamesWithQuantiles = labelNames + "quantile"
            if(value.quantiles != null){
            for (quantile in value.quantiles) {
                val labelValuesWithQuantiles = labels + quantile.key.toString()
                samples.add(
                    Sample(name = fullName + "_quantile",
                        labelNames = labelNamesWithQuantiles,
                        labelValues = labelValuesWithQuantiles,
                        value = quantile.value,
                        timestamp = value.created
                    )
                )
            }
            }
            samples.add(Sample(name = fullName + "_count",
                labelNames = labelNames,
                labelValues = labels,
                value = value.count,
                timestamp = value.created
            ))
            samples.add(Sample(name = fullName + "_sum",
                labelNames = labelNames,
                labelValues = labels,
                value = value.sum,
                timestamp = value.created
            ))

        }
        return familySamplesList(samples)
    }


}


