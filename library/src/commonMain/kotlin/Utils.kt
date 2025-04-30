package io.github.kotlin.fibonacci

import kotlinx.datetime.Clock

fun getCurrentMillis(clock: Clock = Clock.System): Long {
    return clock.now().toEpochMilliseconds()
}

fun getCurrentSeconds(clock: Clock = Clock.System): Long {
    return clock.now().epochSeconds
}

expect fun Collector.getClassName(): String

fun doubleQuoteString(s: String): String = when{
    s.startsWith("\"") && s.endsWith("\"") -> s
    s.startsWith("\"") -> "\"${s.drop(1)}\""
    s.endsWith("\"") -> "\"${s.dropLast(1)}\""
    else -> "\"$s\""
}
