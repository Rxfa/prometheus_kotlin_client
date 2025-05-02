package io.github.kotlin.fibonacci

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class Gauge(
    fullName: String,
    help: String,
    labelNames: List<String> = listOf(),
    unit: String = "",
    private val clock: Clock = Clock.System,
) : SimpleCollector<Gauge.Child>(fullName, help, labelNames, unit) {

    override val suffixes: Set<String> = setOf()
    override val name: String = fullName
    override val type: Type = Type.GAUGE

    init {
        initializeNoLabelsChild()
    }

    override fun newChild(): Child {
        return Child()
    }


    inner class Child(){
        private var valueLong = atomic(0L)
        private var valueDouble = atomic(0.0)
        /**
         * Increment the gauge by the given amount.
         */
        suspend fun inc(amount: Double){
            require(amount >= 0) { "Increment must be non-negative" }
            withContext(Dispatchers.Default){
                while(true){
                    val current = valueDouble.value
                    if (valueDouble.compareAndSet(current, current + amount)){
                        break
                    }
                }
            }
        }

        /**
         * Increment the gauge by 1.
         */
        suspend fun inc(){
            withContext(Dispatchers.Default){
                valueLong.getAndIncrement()
            }
        }

        /**
         * Decrement the Gauge by the given amount.
         */
        suspend fun dec(amount: Double){
            require(amount >= 0) { "Decrement must be non-negative" }
            withContext(Dispatchers.Default){
                while(true){
                    val current = valueDouble.value
                    if (valueDouble.compareAndSet(current, current - amount)){
                        break
                    }
                }
            }
        }

        /**
         * Decrement the gauge by 1.
         */
        suspend fun dec(){
            withContext(Dispatchers.Default){
                valueLong.getAndDecrement()
            }
        }

        /**
         * Set the gauge to the given value.
         */
        suspend fun set(amount: Double) {
            withContext(Dispatchers.Default) {
                valueLong.value = 0L
                valueDouble.value = amount
            }
        }


        /**
         * Set the gauge to the current unixtime in seconds.
         */
        suspend fun setToCurrentTime(){
            withContext(Dispatchers.Default){
                valueLong.value = 0L
                valueDouble.value = getCurrentSeconds(clock).toDouble()
            }

        }

        fun get(): Double = valueLong.value + valueDouble.value
    }

    /**
     * Increment the gauge by 1.
     */
    suspend fun inc(){
        noLabelsChild?.inc()
    }

    /**
     * Increment the gauge by the given amount.
     */
    suspend fun inc(amount: Double){
        noLabelsChild?.inc(amount)
    }

    /**
     * Decrement the gauge by 1.
     */
    suspend fun dec(){
        noLabelsChild?.dec()
    }

    /**
     * Decrement the Gauge by the given amount.
     */
    suspend fun dec(amount: Double){
        noLabelsChild?.dec(amount)
    }

    /**
     * Set the gauge to the given value.
     */
    suspend fun set(amount: Double){
        noLabelsChild?.set(amount)
    }

    /**
     * Set the gauge to the current unixtime in seconds.
     */
    suspend fun setToCurrentTime() {
        noLabelsChild?.setToCurrentTime()
    }

    fun get(): Double {
        return noLabelsChild?.get() ?: 0.0
    }

    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        for ((labels, child) in childMetrics){
            samples += Sample(name = name, labelNames = labelNames, labelValues = labels, value = child.get())
        }
        return familySamplesList(samples)
    }
}