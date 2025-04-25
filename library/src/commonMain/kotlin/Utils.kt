package io.github.kotlin.fibonacci

import kotlinx.datetime.Clock

fun getCurrentTime(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

expect fun Collector.getClassName(): String

fun doubleQuoteString(s: String): String = when{
    s.startsWith("\"") && s.endsWith("\"") -> s
    s.startsWith("\"") -> "\"${s.drop(1)}\""
    s.endsWith("\"") -> "\"${s.dropLast(1)}\""
    else -> "\"$s\""
}
