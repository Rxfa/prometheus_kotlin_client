package io.github.kotlin.fibonacci

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Counter(
    fullName: String,
    help: String,
    labelNames: List<String> = listOf(),
    unit: String = "",
    val includeCreatedSeries: Boolean = false,
) : SimpleCollector<Counter.Child>(fullName, help, labelNames, unit) {
    override val suffixes: Set<String> = setOf("_total")

    override val name: String = if(suffixes.any{ fullName.endsWith(it) }) fullName else fullName + "_total"

    override val type: Type = Type.COUNTER

    init {
        initializeNoLabelsChild()
    }

    override fun newChild(): Child {
        return Child()
    }

    inner class Child {
        //TODO(WOULD BE MORE EFFICIENT UDSING JAVA ADDERS)
        //TODO(LOOK IF @PublishedApi is worthit)
        private var value = atomic(0.0.toRawBits())

        suspend fun inc(amount: Double) {
            require(amount >= 0) { "Value must be positive" }
            withContext(Dispatchers.Default){
                 value.updateAndGet { currentBits ->
                     val current = Double.fromBits(currentBits)
                     val updated = current + amount
                     updated.toBits()
                }
            }
        }

        suspend fun inc(){
            inc(1.0)
        }

        fun get(): Double = Double.fromBits(value.value)

    }

    suspend fun inc(amount: Double): Unit? {
        require(amount >= 0) { "Amount must be positive" }
        return noLabelsChild?.inc(amount)
    }

    suspend fun inc(): Unit? = noLabelsChild?.inc()

    fun get(): Double = noLabelsChild?.get() ?: 0.0

    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for ((labels, child) in childMetrics){
            samples += Sample(name = name, labelNames = labelNames, labelValues = labels, value = child.get())
            if(includeCreatedSeries){
                val createdSeriesName = name.removeSuffix("_total") + "_created"
                samples += Sample(name = createdSeriesName, labelNames = labelNames, labelValues = labels, value = child.get())
            }
        }
        return familySamplesList(samples)
    }
}