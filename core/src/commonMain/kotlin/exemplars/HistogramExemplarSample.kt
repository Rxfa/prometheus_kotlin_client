package io.github.rxfa.prometheus.core

public interface HistogramExemplarSample {
    public fun sample(
        value: Double,
        bucketFrom: Double,
        bucketTo: Double,
        previous: Exemplar?,
    ): Exemplar
}
