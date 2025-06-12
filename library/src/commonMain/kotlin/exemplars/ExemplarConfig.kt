package io.github.kotlin.fibonacci.exemplars

import kotlin.concurrent.Volatile

public object ExemplarConfig {

    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var histogramExemplarSample: HistogramExemplarSample?;


    init{
        //TODO(need to create default sampler)
        histogramExemplarSample = object : HistogramExemplarSample {
            override fun sample(
                value: Double,
                bucketFrom: Double,
                bucketTo: Double,
                previous: Exemplar?
            ): Exemplar {
                return Exemplar(listOf("bucketFrom" + bucketFrom.toString(), "bucketTo" + bucketTo.toString()), value)
            }

        }
    }

    public fun isEnabled(): Boolean {
        return enabled
    }

    public fun disable() {
        this.enabled = false
    }

    public fun enable() {
        this.enabled = true
    }

    public fun getHistogramExemplarSampler(): HistogramExemplarSample? {
        return histogramExemplarSample
    }

    public fun setHistogramExemplarSampler(sampler: HistogramExemplarSample) {
        this.histogramExemplarSample = sampler
    }

}