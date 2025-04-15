package io.github.kotlin.fibonacci

expect fun getCurrentTime(): Long

expect fun Collector.getClassName(): String

fun doubleQuoteString(s: String): String = when{
    s.startsWith("\"") && s.endsWith("\"") -> s
    s.startsWith("\"") -> "\"${s.drop(1)}\""
    s.endsWith("\"") -> "\"${s.dropLast(1)}\""
    else -> "\"$s\""
}
