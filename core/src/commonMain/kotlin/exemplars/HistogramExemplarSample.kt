package io.github.kotlin.fibonacci.exemplars

public interface HistogramExemplarSample {

     public fun sample(
         value: Double,
         bucketFrom: Double,
         bucketTo: Double,
         previous: Exemplar?
     ):Exemplar;
}

