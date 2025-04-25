package io.github.kotlin.fibonacci

actual fun Collector.getClassName(): String {
    return this::class.qualifiedName!!
}