package io.github.rxfa.prometheus.core

import kotlinx.datetime.Clock

/**
 * Returns the current Unix timestamp in milliseconds from the given [Clock].
 *
 * @param clock The clock to use for time resolution (default is [Clock.System]).
 * @return Current time in milliseconds since epoch.
 */
public fun getCurrentMillis(clock: Clock = Clock.System): Long {
    return clock.now().toEpochMilliseconds()
}

/**
 * Returns the current Unix timestamp in seconds from the given [Clock].
 *
 * @param clock The clock to use for time resolution (default is [Clock.System]).
 * @return Current time in seconds since epoch.
 */
public fun getCurrentSeconds(clock: Clock = Clock.System): Long {
    return clock.now().epochSeconds
}

/**
 * Returns the simple class name of the [Collector] instance.
 *
 * @return The simple class name (non-null).
 * @throws KotlinNullPointerException if the class has no name (rare).
 */
public fun Collector.getClassName(): String {
    return this::class.simpleName!!
}

/**
 * Ensures a given string is surrounded by double quotes, correcting partial quotes.
 *
 * Used to safely encode label values in Prometheus exposition format.
 *
 * @param s The input string.
 * @return The input string enclosed in double quotes.
 */
public fun doubleQuoteString(s: String): String = when{
    s.startsWith("\"") && s.endsWith("\"") -> s
    s.startsWith("\"") -> "\"${s.drop(1)}\""
    s.endsWith("\"") -> "\"${s.dropLast(1)}\""
    else -> "\"$s\""
}
