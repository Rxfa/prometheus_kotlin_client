package io.github.rxfa.prometheus.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

public class TimeWindowQuantiles(
    private val quantiles: Array<Quantiles.Quantile>,
    maxAgeSeconds: Long,
    ageBuckets: Int,
) {
    private val ringBuffer: Array<Quantiles> = Array(ageBuckets) { Quantiles(quantiles) }
    private var currentBucket: Int = 0
    private var lastRotateTimestampMillis: Long = Clock.System.now().toEpochMilliseconds()
    private val durationBetweenRotatesMillis: Long = (maxAgeSeconds * 1000) / ageBuckets
    private val mutex = Mutex()

    public fun get(q: Double): Double {
        return rotate().get(q)
    }

    public suspend fun insert(value: Double) {
        withContext(Dispatchers.Default) {
            mutex.withLock {
                rotate()
                for (ckmsQuantiles in ringBuffer) {
                    ckmsQuantiles.insert(value)
                }
            }
        }
    }

    private fun rotate(): Quantiles {
        var timeSinceLastRotateMillis = Clock.System.now().toEpochMilliseconds() - lastRotateTimestampMillis
        while (timeSinceLastRotateMillis > durationBetweenRotatesMillis) {
            ringBuffer[currentBucket] = Quantiles(quantiles)
            if (++currentBucket >= ringBuffer.size) {
                currentBucket = 0
            }
            timeSinceLastRotateMillis -= durationBetweenRotatesMillis
            lastRotateTimestampMillis += durationBetweenRotatesMillis
        }
        return ringBuffer[currentBucket]
    }
}
