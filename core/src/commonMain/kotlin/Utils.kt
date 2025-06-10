package io.github.kotlin.fibonacci

import kotlinx.datetime.Clock

public fun getCurrentMillis(clock: Clock = Clock.System): Long {
    return clock.now().toEpochMilliseconds()
}

public fun getCurrentSeconds(clock: Clock = Clock.System): Long {
    return clock.now().epochSeconds
}

public fun Collector.getClassName(): String {
    return this::class.simpleName!!
}

public fun doubleQuoteString(s: String): String = when{
    s.startsWith("\"") && s.endsWith("\"") -> s
    s.startsWith("\"") -> "\"${s.drop(1)}\""
    s.endsWith("\"") -> "\"${s.dropLast(1)}\""
    else -> "\"$s\""
}
