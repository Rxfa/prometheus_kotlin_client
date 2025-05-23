package io.github.kotlin.fibonacci

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
        private var value = 0.0

        /**
         * Increment the gauge by the given amount.
         */
        fun inc(amount: Double){
            require(amount >= 0) { "Increment must be non-negative" }
            value += amount
        }

        /**
         * Increment the gauge by 1.
         */
        fun inc(){
            value += 1
        }

        /**
         * Decrement the Gauge by the given amount.
         */
        fun dec(amount: Double){
            require(amount >= 0) { "Decrement must be non-negative" }
            value -= amount
        }

        /**
         * Decrement the gauge by 1.
         */
        fun dec(){
            value -= 1
        }

        /**
         * Set the gauge to the given value.
         */
        fun set(amount: Double){
            value = amount
        }

        /**
         * Set the gauge to the current unixtime in seconds.
         */
        fun setToCurrentTime(){
            value = getCurrentSeconds(clock).toDouble()
        }

        fun get(): Double = value
    }

    /**
     * Increment the gauge by 1.
     */
    fun inc(){
        noLabelsChild?.inc()
    }

    /**
     * Increment the gauge by the given amount.
     */
    fun inc(amount: Double){
        noLabelsChild?.inc(amount)
    }

    /**
     * Decrement the gauge by 1.
     */
    fun dec(){
        noLabelsChild?.dec()
    }

    /**
     * Decrement the Gauge by the given amount.
     */
    fun dec(amount: Double){
        noLabelsChild?.dec(amount)
    }

    /**
     * Set the gauge to the given value.
     */
    fun set(amount: Double){
        noLabelsChild?.set(amount)
    }

    /**
     * Set the gauge to the current unixtime in seconds.
     */
    fun setToCurrentTime() {
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