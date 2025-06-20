package io.github.kotlin.fibonacci

import kotlinx.datetime.Clock
import kotlin.time.TimeSource


public class SimpleTimer {
    private val start:Long;
    public val defaultTimeProvider: TimeProvider = TimeProvider();
    private val timeProvider: TimeProvider ;

    private val MILLISECONDS_PER_SECOND = 1000.0
    public class TimeProvider {
        private val clock: Clock = Clock.System
        public val milliTime:Long get() = getCurrentMillis(clock)
    }

    public constructor(timeProvider: TimeProvider) {
        this.timeProvider = timeProvider
        start = timeProvider.milliTime
    }

    public constructor() {
        this.timeProvider = defaultTimeProvider
        start = defaultTimeProvider.milliTime
    }

    /**
     * @return Measured duration in seconds since {@link SimpleTimer} was constructed.
     */
    public fun elapsedSeconds(): Double {
        return elapsedSecondsFromNanos(start, timeProvider.milliTime);
    }

    public fun elapsedSecondsFromNanos(startNanos: Long, endNanos: Long): Double {
        return (endNanos - startNanos) / MILLISECONDS_PER_SECOND;
    }
}