package io.github.rxfa.prometheus.core

// The original implementation was copied from
// https://github.com/prometheus/client_java/blob/simpleclient/simpleclient/src/main/java/io/prometheus/client/CKMSQuantiles.java#L54C1-L57C8
// and adapted to Kotlin.

public class Quantiles(
    private val quantiles: Array<Quantile>
) {

    private var n:Int = 0

    private val samples: MutableList<Sample> = mutableListOf()


    private val compressInterval: Int = 128
    private var insertsSinceLastCompress: Int = 0

    private val buffer:Array<Double> = Array(compressInterval){0.0}
    private var bufferIndex:Int = 0

    public fun insert(value: Double) {
        buffer[bufferIndex++] = value

        if(bufferIndex == buffer.size){
            flush()
        }
        if(++ insertsSinceLastCompress == compressInterval){
            compress()
            insertsSinceLastCompress = 0
        }

    }

    private fun flush() {
        buffer.sort(0,bufferIndex)
        insertBatch(buffer, bufferIndex)
        bufferIndex = 0
    }

    private fun insertBatch(sortedBuffer: Array<Double>, toIndex: Int) {
        if(toIndex == 0) return
        val iterator = samples.listIterator()
        var i = 0 // position in buffer
        var r = 0 // sum of g's left of the current sample

        while (iterator.hasNext() && i < toIndex) {
            val item = iterator.next()
            while (i < toIndex) {
                if (sortedBuffer[i] > item.value) {
                    break
                }
                insertBefore(iterator, sortedBuffer[i], r)
                r++ // new item with g=1 was inserted before, so increment r
                i++
                n++
            }
            r += item.g
        }
        while (i < toIndex) {
            samples.add(Sample(sortedBuffer[i], 0))
            i++
            n++
        }
    }


    private fun insertBefore(iterator: MutableListIterator<Sample>, value: Double, r: Int) {
        if (!iterator.hasPrevious()) {
            samples.add(0, Sample(value, 0))
        } else {
            iterator.previous()
            iterator.add(Sample(value, f(r) - 1))
            iterator.next()
        }
    }

    public fun get(q: Double): Double {
        flush()

        if (samples.isEmpty()) {
            return Double.NaN
        }

        if (q == 0.0) {
            return samples.first().value
        }

        if (q == 1.0) {
            return samples.last().value
        }

        var r = 0 // sum of g's left of the current sample
        val desiredRank = kotlin.math.ceil(q * n).toInt()
        val upperBound = desiredRank + f(desiredRank) / 2

        val iterator = samples.listIterator()
        while (iterator.hasNext()) {
            val sample = iterator.next()
            if (r + sample.g + sample.delta > upperBound) {
                iterator.previous() // roll back the iterator.next() above
                return if (iterator.hasPrevious()) {
                    iterator.previous().value
                } else {
                    sample.value
                }
            }
            r += sample.g
        }
        return samples.last().value
    }

    // The expected result of (2*0.01*30)/(1-0.95) is 12. The actual result is 11.99999999999999.
    // To avoid running into these types of error we add 0.00000000001 before rounding down.

    private fun f(r: Int): Int {
        var minResult = Int.MAX_VALUE
        for (q in quantiles) {
            if (q.quantile == 0.0 || q.quantile == 1.0) {
                continue
            }
            val result = if (r >= q.quantile * n) {
                (q.v * r + 0.00000000001).toInt()
            } else {
                (q.u * (n - r) + 0.00000000001).toInt()
            }
            if (result < minResult) {
                minResult = result
            }
        }
        return minResult.coerceAtLeast(1)
    }

    private fun compress() {
        if (samples.size < 3) {
            return
        }
        val descendingIterator = samples.asReversed().iterator()
        var r = n // n is equal to the sum of the g's of all samples

        var right: Sample
        var left = descendingIterator.next()
        r -= left.g

        while (descendingIterator.hasNext()) {
            right = left
            left = descendingIterator.next()
            r -= left.g
            if (left == samples.first()) {
                // The min sample must never be merged.
                break
            }
            if (left.g + right.g + right.delta < f(r)) {
                right.g += left.g
                descendingIterator.remove()
                left = right
            }
        }
    }


    public data class Sample(
        val value:Double,
        /**
         * Observed value.
         */
        val delta: Int,
        /**
         * Difference between the greatest possible rank of this sample and the lowest possible rank of this sample.
         */
        var g: Int = 1 //
        /**
         * Difference between the lowest possible rank of this sample and its predecessor.
         * This always starts with 1, but will be updated when compress() merges Samples.
         */
    ){
        public override fun toString(): String {
            return "Sample{value=${value}, delta=${delta}, g=${g}}"
        }
    }

    public class Quantile(quantile: Double, epsilon: Double) {

        public val quantile: Double

        public val epsilon: Double

        public val u: Double

        public val v: Double

        init {
            if (quantile < 0.0 || quantile > 1.0) throw IllegalArgumentException("Quantile must be between 0 and 1");
            if (epsilon < 0.0 || epsilon > 1.0) throw IllegalArgumentException("Epsilon must be between 0 and 1");

            this.quantile = quantile;
            this.epsilon = epsilon;
            u = 2.0 * epsilon / (1.0 - quantile); // if quantile == 1 this will be Double.NaN
            v = 2.0 * epsilon / quantile; // if quantile == 0 this will be Double.NaN
        }


        public override fun toString(): String {
            return "Quantile{q=${quantile}.3f, epsilon=${epsilon}.3f}"
        }
    }
}