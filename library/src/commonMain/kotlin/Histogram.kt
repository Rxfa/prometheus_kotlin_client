package io.github.kotlin.fibonacci

import kotlinx.atomicfu.atomic
import kotlinx.datetime.Clock

public class Histogram(
    fullName: String,
    help: String,
    labelNames: List<String> = emptyList(),
    unit: String = "",
    includeCreatedSeries: Boolean = false,
    buckets: List<Double> = listOf(
        0.005, 0.01, 0.025, 0.05, 0.1,
        0.25, 0.5, 1.0, 2.5, 5.0, 10.0
    )
):SimpleCollector<Histogram.Child>(fullName, help, labelNames, unit) {

    //TODO(eventually change this version to the buffer one)

    override val suffixes: Set<String> = setOf()

    /**
     * Histograms and summaries
     *
     * The histogram and summary types are difficult to represent in the text format. The following conventions apply:
     *
     *     The sample sum for a summary or histogram named x is given as a separate sample named x_sum.
     *     The sample count for a summary or histogram named x is given as a separate sample named x_count.
     *     Each quantile of a summary named x is given as a separate sample line with the same name x and a label {quantile="y"}.
     *     Each bucket count of a histogram named x is given as a separate sample line with the name x_bucket and a label {le="y"} (where y is the upper bound of the bucket).
     *     A histogram must have a bucket with {le="+Inf"}. Its value must be identical to the value of x_count.
     *     The buckets of a histogram and the quantiles of a summary must appear in increasing numerical order of their label values (for the le or the quantile label, respectively).
     *
     */

    override val name: String = fullName

    override val type: Type = Type.HISTOGRAM

    private val sortedBuckets: List<Double> = (buckets.sorted() + Double.POSITIVE_INFINITY).distinct()

    override fun newChild(): Child {
        return Child()
    }

    public inner class Child {
        private val bucketCounts: MutableMap<Double, Long> = mutableMapOf()
        private var sum = atomic(0.0)
        private var count = atomic(0L)
        private val created: Long = Clock.System.now().toEpochMilliseconds()

        init {
            for (bucket in sortedBuckets) {
                bucketCounts[bucket] = 0
            }
        }

        public fun observe(value: Double) {
            for (bucket in sortedBuckets) {
                if (value <= bucket) {
                    bucketCounts[bucket] = (bucketCounts[bucket] ?: 0) + 1
                }
            }
        }

        public fun getBuckets(): Double {
            TODO()
        }

        public fun created(): Long {
            TODO()
        }
    }

    public fun observe(value: Double): Unit? {
        TODO()
    }

    public fun get(): Double {
        TODO()
    }

    override fun collect(): MetricFamilySamples {
        val samples = mutableListOf<Sample>()
        //TODO()
        return familySamplesList(emptyList())
    }
}